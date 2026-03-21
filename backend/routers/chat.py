from fastapi import APIRouter, Depends, HTTPException, status

from models.message import ChatRequest, ChatResponse
from models.user import UserInDB
from services.auth_service import get_current_user
from services.companion_service import get_companion, get_companions_for_role, build_jenny_system_prompt
from services import ai_service

router = APIRouter(prefix="/chat", tags=["chat"])


@router.post("", response_model=ChatResponse)
async def send_message(
    body: ChatRequest,
    current_user: UserInDB = Depends(get_current_user),
):
    # Verify companion access
    allowed = {c.id for c in get_companions_for_role(current_user.role)}
    if body.companion_id not in allowed:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"Non hai accesso al companion '{body.companion_id}'",
        )

    companion = get_companion(body.companion_id)
    if companion is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Companion '{body.companion_id}' non trovato",
        )

    if not body.messages:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="messages non può essere vuoto",
        )

    # Build system prompt — modulate Jenny based on personality_level
    if companion.id == "jenny":
        level = max(1, min(5, body.personality_level))
        system_prompt = build_jenny_system_prompt(level)
    else:
        system_prompt = companion.system_prompt

    reply = await ai_service.chat(
        system_prompt=system_prompt,
        messages=body.messages,
    )

    return ChatResponse(
        reply=reply,
        companion_id=companion.id,
        provider=ai_service.get_active_provider(),
    )
