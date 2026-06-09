import logging
from typing import List
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.database import get_db
from app.models.user import User, Role
from app.core.jwt_validation import validate_jwt

logger = logging.getLogger(__name__)

# Note: In an enterprise setup, the frontend logs into Keycloak and passes the Bearer token.
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login", auto_error=False)

async def sync_user_from_token(payload: dict, db: AsyncSession) -> User:
    """Synchronize Keycloak user into local database if they don't exist"""
    email = payload.get("email")
    if not email:
        raise HTTPException(status_code=400, detail="Token missing email")

    # Keycloak ID
    kc_id = payload.get("sub")
    
    result = await db.execute(select(User).filter(User.email == email))
    user = result.scalars().first()
    
    if not user:
        # Extract name from Keycloak token
        full_name = payload.get("name") or f"{payload.get('given_name', '')} {payload.get('family_name', '')}".strip() or email
        
        # We can extract the roles from the token or default to NETWORK_ENGINEER
        realm_access = payload.get("realm_access", {})
        roles = realm_access.get("roles", [])
        
        assigned_role = Role.network_engineer
        for r in Role:
            if r.value.upper() in [kr.upper() for kr in roles]:
                assigned_role = r
                break
                
        user = User(
            email=email,
            full_name=full_name,
            provider="keycloak",
            provider_id=kc_id,
            hashed_password="SSO_USER", # Dummy password since Keycloak handles auth
            is_active=True,
            is_verified=payload.get("email_verified", False),
            role=assigned_role
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)
    
    return user

async def get_current_user(token: str = Depends(oauth2_scheme), db: AsyncSession = Depends(get_db)) -> User:
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    payload = validate_jwt(token)
    user = await sync_user_from_token(payload, db)
    return user

async def get_current_active_user(current_user: User = Depends(get_current_user)) -> User:
    if not current_user.is_active:
        raise HTTPException(status_code=400, detail="Inactive user")
    return current_user
