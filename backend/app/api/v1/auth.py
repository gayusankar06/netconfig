from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.database import get_db
from google.oauth2 import id_token
from google.auth.transport import requests as google_requests
import logging
import os
from app.models.user import User, Role
# If AuditLog exists, use it. Otherwise, comment out for now.
try:
    from app.models.audit_log import AuditLog
    HAS_AUDIT = False # Disabled due to schema mismatch
except ImportError:
    HAS_AUDIT = False

# Assuming AuthService is defined, if not we will define inline or rely on existing security core
try:
    from app.services.auth_service import AuthService, get_current_active_user
except ImportError:
    # Fallback to core security
    from app.core.security import verify_password, get_password_hash as hash_password, create_access_token
    from app.core.deps import get_current_active_user
    class AuthService:
        @staticmethod
        def verify_password(plain, hashed): return verify_password(plain, hashed)
        @staticmethod
        def hash_password(password): return hash_password(password)
        @staticmethod
        def create_access_token(data): return create_access_token(data)
        @staticmethod
        def create_refresh_token(data): return create_access_token(data) # Simplified fallback

from app.config import settings

router = APIRouter()

# --- Schemas ---
class UserCreate(BaseModel):
    email: str
    password: str
    full_name: str
    role: Role = Role.network_engineer

import uuid

class UserOut(BaseModel):
    id: uuid.UUID
    email: str
    full_name: str
    role: Role
    is_active: bool

    class Config:
        from_attributes = True

class LoginRequest(BaseModel):
    email: str
    password: str

class TokenOut(BaseModel):
    access_token: str
    refresh_token: str | None = None
    token_type: str = "Bearer"
    expires_in: int
    user: UserOut

class RefreshTokenRequest(BaseModel):
    refresh_token: str

class GoogleAuthRequest(BaseModel):
    token: str

class ForgotPasswordRequest(BaseModel):
    email: str

class ResetPasswordRequest(BaseModel):
    token: str
    new_password: str

# --- Routes ---

@router.post("/register", response_model=UserOut, status_code=status.HTTP_201_CREATED)
async def register(user_data: UserCreate, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).filter(User.email == user_data.email))
    if result.scalars().first():
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Email already registered")
    
    hashed_pwd = AuthService.hash_password(user_data.password)
    user = User(
        email=user_data.email,
        hashed_password=hashed_pwd,
        full_name=user_data.full_name,
        role=user_data.role,
        provider="local"
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)

    if HAS_AUDIT:
        audit = AuditLog(event_type="USER_MANAGEMENT", event_description=f"User {user.email} registered", user_id=user.id)
        db.add(audit)
        await db.commit()

    return user

@router.post("/login", response_model=TokenOut)
async def login(login_data: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).filter(User.email == login_data.email))
    user = result.scalars().first()
    
    if not user or not AuthService.verify_password(login_data.password, user.hashed_password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Incorrect email or password")
    
    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Inactive user")

    user.last_login_at = datetime.utcnow()
    db.add(user)

    access_token = AuthService.create_access_token(data={"sub": str(user.id), "role": user.role.value})
    refresh_token = AuthService.create_refresh_token(data={"sub": str(user.id)})

    if HAS_AUDIT:
        audit = AuditLog(event_type="AUTH_LOGIN", event_description=f"User {user.email} logged in", user_id=user.id)
        db.add(audit)
    
    await db.commit()

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "Bearer",
        "expires_in": settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        "user": user
    }

@router.post("/google", response_model=TokenOut)
async def google_auth(auth_data: GoogleAuthRequest, db: AsyncSession = Depends(get_db)):
    client_id = os.getenv("GOOGLE_CLIENT_ID", "")
    if not client_id:
        raise HTTPException(status_code=500, detail="Google Client ID is not configured on the server")
    
    try:
        idinfo = id_token.verify_oauth2_token(auth_data.token, google_requests.Request(), client_id)
        email = idinfo.get("email")
        full_name = idinfo.get("name", "Google User")
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid Google token")

    result = await db.execute(select(User).filter(User.email == email))
    user = result.scalars().first()

    if not user:
        # Auto register
        user = User(
            email=email,
            hashed_password=AuthService.hash_password(os.urandom(16).hex()),
            full_name=full_name,
            role=Role.network_engineer,
            provider="google",
            is_active=True
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)

        if HAS_AUDIT:
            audit = AuditLog(event_type="USER_MANAGEMENT", event_description=f"Google User {user.email} registered", user_id=user.id)
            db.add(audit)

    if not user.is_active:
        raise HTTPException(status_code=400, detail="Inactive user")

    user.last_login_at = datetime.utcnow()
    db.add(user)

    access_token = AuthService.create_access_token(data={"sub": str(user.id), "role": user.role.value})
    refresh_token = AuthService.create_refresh_token(data={"sub": str(user.id)})

    if HAS_AUDIT:
        audit = AuditLog(event_type="AUTH_LOGIN", event_description=f"User {user.email} logged in via Google", user_id=user.id)
        db.add(audit)
    
    await db.commit()

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "Bearer",
        "expires_in": settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        "user": user
    }

@router.post("/forgot-password")
async def forgot_password(req: ForgotPasswordRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).filter(User.email == req.email))
    user = result.scalars().first()
    
    if user:
        reset_token = AuthService.create_access_token(data={"sub": str(user.id), "type": "reset"})
        # Mock Email sending
        logging.warning(f"MOCK EMAIL [Forgot Password]: Send this link to {user.email}: http://localhost:3000/reset-password?token={reset_token}")
        
    return {"detail": "If the email is registered, a password reset link has been sent to it."}

@router.post("/reset-password")
async def reset_password(req: ResetPasswordRequest, db: AsyncSession = Depends(get_db)):
    try:
        from app.core.security import decode_token
        payload = decode_token(req.token)
        if payload.get("type") != "reset":
            raise ValueError("Invalid token type")
        user_id = payload.get("sub")
    except Exception as e:
        raise HTTPException(status_code=400, detail="Invalid or expired reset token")

    result = await db.execute(select(User).filter(User.id == user_id))
    user = result.scalars().first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    user.hashed_password = AuthService.hash_password(req.new_password)
    db.add(user)
    
    if HAS_AUDIT:
        audit = AuditLog(event_type="USER_MANAGEMENT", event_description=f"Password reset for {user.email}", user_id=user.id)
        db.add(audit)
        
    await db.commit()
    
    return {"detail": "Password has been successfully reset."}

@router.post("/refresh", response_model=TokenOut)
async def refresh_token(refresh_data: RefreshTokenRequest, db: AsyncSession = Depends(get_db)):
    raise HTTPException(status_code=status.HTTP_501_NOT_IMPLEMENTED, detail="Refresh logic to be finalized")

@router.post("/logout", status_code=status.HTTP_200_OK)
async def logout(current_user: User = Depends(get_current_active_user), db: AsyncSession = Depends(get_db)):
    if HAS_AUDIT:
        audit = AuditLog(event_type="AUTH_LOGOUT", event_description=f"User {current_user.email} logged out", user_id=current_user.id)
        db.add(audit)
        await db.commit()
    return {"detail": "Logged out successfully"}

@router.get("/me", response_model=UserOut)
async def get_me(current_user: User = Depends(get_current_active_user)):
    return current_user
