from fastapi import APIRouter, Depends
from pydantic import BaseModel

from models.user import UserInDB, Role
from services.auth_service import get_current_user
from services.companion_service import get_companions_for_role

router = APIRouter(prefix="/companions", tags=["companions"])


class CompanionOut(BaseModel):
    id: str
    name: str
    description: str
    avatar_color: str
    locked: bool = False


@router.get("", response_model=list[CompanionOut])
async def list_companions(current_user: UserInDB = Depends(get_current_user)):
    companions = get_companions_for_role(current_user.role)
    return [
        CompanionOut(
            id=c.id,
            name=c.name,
            description=c.description,
            avatar_color=c.avatar_color,
            locked=False,
        )
        for c in companions
    ]
