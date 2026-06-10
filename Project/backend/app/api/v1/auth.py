from datetime import datetime, timedelta
from typing import List, Optional, Dict, Any, Union
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
import httpx

from app.database import get_db
# google-auth still installed for fallback but we use UserInfo API directly
try:
    from google.oauth2 import id_token
    from google.auth.transport import requests as google_requests
except ImportError:
    id_token = None
import logging
import os
from app.models.user import User, Role
# If AuditLog exists, use it. Otherwise, comment out for now.
try:
    from app.models.audit import AuditLog
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
    access_token: str  # OAuth2 access_token from @react-oauth/google useGoogleLogin

class ForgotPasswordRequest(BaseModel):
    email: str

class ResetPasswordRequest(BaseModel):
    token: str
    new_password: str

# --- Routes ---

@router.post("/register", response_model=UserOut, status_code=status.HTTP_201_CREATED)
async def register(user_data: UserCreate, db: AsyncSession = Depends(get_db)):
    try:
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
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Registration failed: {str(e)}")

@router.post("/login", response_model=TokenOut)
async def login(login_data: LoginRequest, db: AsyncSession = Depends(get_db)):
    try:
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
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Login failed: {str(e)}")

@router.post("/google", response_model=TokenOut)
async def google_auth(auth_data: GoogleAuthRequest, db: AsyncSession = Depends(get_db)):
    """
    Exchange a Google OAuth2 access_token (from @react-oauth/google useGoogleLogin)
    for a platform JWT. Uses Google's UserInfo endpoint.
    """
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                headers={"Authorization": f"Bearer {auth_data.access_token}"},
                timeout=10.0
            )
        if resp.status_code != 200:
            raise HTTPException(status_code=401, detail="Invalid Google access token")
        info = resp.json()
        email = info.get("email")
        full_name = info.get("name") or info.get("given_name", "Google User")
        provider_id = info.get("sub", "")
        if not email:
            raise HTTPException(status_code=400, detail="Google account has no email")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Google verification failed: {str(e)}")

    result = await db.execute(select(User).filter(User.email == email))
    user = result.scalars().first()

    if not user:
        # Auto-register new Google user
        user = User(
            email=email,
            hashed_password=AuthService.hash_password(os.urandom(24).hex()),
            full_name=full_name,
            role=Role.network_engineer,
            provider="google",
            provider_id=provider_id,
            is_active=True
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)
        logging.info(f"Auto-registered Google user: {email}")
    elif user.provider == "local":
        # Allow Google login for existing local account — just update provider
        user.provider_id = provider_id

    if not user.is_active:
        raise HTTPException(status_code=400, detail="Account is inactive. Contact your administrator.")

    user.last_login_at = datetime.utcnow()
    db.add(user)

    access_token = AuthService.create_access_token(data={"sub": str(user.id), "role": user.role.value})
    refresh_token = AuthService.create_refresh_token(data={"sub": str(user.id)})

    # Audit log
    try:
        audit = AuditLog(
            event_type="AUTH_LOGIN",
            event_description=f"User {user.email} logged in via Google OAuth",
            user_id=user.id,
            user_email=user.email,
            user_role=str(user.role.value if hasattr(user.role, 'value') else user.role)
        )
        db.add(audit)
    except Exception:
        pass  # Audit failure must not block login

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
