from fastapi import APIRouter, HTTPException, status, Depends

from database import authenticate_user
from models.user import LoginRequest, TokenResponse, UserPublic
from services.auth_service import create_access_token, get_current_user
from models.user import UserInDB

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/login", response_model=TokenResponse)
async def login(body: LoginRequest):
    user = authenticate_user(body.username, body.password)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Credenziali non valide",
        )
    token = create_access_token({"sub": user.username, "role": user.role})
    return TokenResponse(
        access_token=token,
        role=user.role,
        username=user.username,
    )


@router.get("/me", response_model=UserPublic)
async def me(current_user: UserInDB = Depends(get_current_user)):
    return UserPublic(username=current_user.username, role=current_user.role)
