from datetime import timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.database import get_db
from app.models.user import User
from app.models.audit import AuditLog
from app.schemas.user import UserCreate, UserOut, TokenOut, LoginRequest, RefreshTokenRequest
from app.services.auth_service import AuthService, get_current_active_user

router = APIRouter()

@router.post("/register", response_model=UserOut, status_code=status.HTTP_201_CREATED)
async def register(user_data: UserCreate, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).filter(User.email == user_data.email))
    if result.scalars().first():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered"
        )
    
    hashed_pwd = AuthService.hash_password(user_data.password)
    user = User(
        email=user_data.email,
        hashed_password=hashed_pwd,
        full_name=user_data.full_name,
        role=user_data.role
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)

    # Log audit
    audit = AuditLog(
        event_type="USER_MANAGEMENT",
        event_description=f"User {user.email} registered successfully with role {user.role}",
        user_id=user.id,
        user_email=user.email,
        user_role=user.role
    )
    db.add(audit)
    await db.commit()

    return user

@router.post("/login", response_model=TokenOut)
async def login(login_data: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).filter(User.email == login_data.email))
    user = result.scalars().first()
    
    if not user or not AuthService.verify_password(login_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Inactive user")

    # Update last login
    user.last_login = datetime.utcnow()
    db.add(user)

    # Generate JWT
    access_token = AuthService.create_access_token(data={"sub": user.email})
    refresh_token = AuthService.create_refresh_token(data={"sub": user.email})

    # Log audit
    audit = AuditLog(
        event_type="AUTH_LOGIN",
        event_description=f"User {user.email} logged in successfully",
        user_id=user.id,
        user_email=user.email,
        user_role=user.role
    )
    db.add(audit)
    await db.commit()

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "Bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        "user": user
    }

@router.post("/refresh", response_model=TokenOut)
async def refresh_token(refresh_data: RefreshTokenRequest, db: AsyncSession = Depends(get_db)):
    payload = AuthService.verify_token(refresh_data.refresh_token, "refresh")
    email = payload.get("sub")
    if not email:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")

    result = await db.execute(select(User).filter(User.email == email))
    user = result.scalars().first()
    if not user or not user.is_active:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not active")

    access_token = AuthService.create_access_token(data={"sub": user.email})
    new_refresh_token = AuthService.create_refresh_token(data={"sub": user.email})

    return {
        "access_token": access_token,
        "refresh_token": new_refresh_token,
        "token_type": "Bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        "user": user
    }

@router.post("/logout", status_code=status.HTTP_200_OK)
async def logout(current_user: User = Depends(get_current_active_user), db: AsyncSession = Depends(get_db)):
    # Log audit
    audit = AuditLog(
        event_type="AUTH_LOGOUT",
        event_description=f"User {current_user.email} logged out successfully",
        user_id=current_user.id,
        user_email=current_user.email,
        user_role=current_user.role
    )
    db.add(audit)
    await db.commit()
    return {"detail": "Logged out successfully"}

@router.get("/me", response_model=UserOut)
async def get_me(current_user: User = Depends(get_current_active_user)):
    return current_user
from datetime import datetime
from app.config import settings
