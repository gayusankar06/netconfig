from typing import List
from fastapi import Depends, HTTPException, status
from app.models.user import User
from app.core.auth_dependencies import get_current_active_user, validate_jwt
from fastapi.security import OAuth2PasswordBearer
import logging

logger = logging.getLogger(__name__)

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login", auto_error=False)

class RoleGuard:
    """
    Enterprise RBAC Role Guard.
    Checks if the user has the required Keycloak realm roles.
    """
    def __init__(self, allowed_roles: List[str]):
        self.allowed_roles = [role.upper() for role in allowed_roles]

    async def __call__(self, token: str = Depends(oauth2_scheme), current_user: User = Depends(get_current_active_user)) -> User:
        if not token:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Not authenticated")
            
        payload = validate_jwt(token)
        realm_access = payload.get("realm_access", {})
        user_roles = [r.upper() for r in realm_access.get("roles", [])]
        
        # Check if user has any of the allowed roles
        has_role = any(role in user_roles for role in self.allowed_roles)
        
        # If no keycloak roles match, fallback to DB role
        if not has_role:
            if current_user.role.value.upper() in self.allowed_roles:
                has_role = True

        if not has_role:
            logger.warning(f"User {current_user.email} denied access. Required: {self.allowed_roles}, Found: {user_roles}")
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="You do not have the enterprise permissions required for this action."
            )
            
        return current_user
