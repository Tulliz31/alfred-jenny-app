from fastapi import APIRouter, Depends

from models.provider import ProviderID, ProviderInfo, SetProviderRequest
from models.user import UserInDB
from services.auth_service import get_current_user, require_admin
from services import ai_service

router = APIRouter(prefix="/providers", tags=["providers"])

_PROVIDER_META: dict[ProviderID, dict] = {
    ProviderID.openai: {
        "name": "OpenAI",
        "description": "GPT-4o — potente e versatile",
        "default_model": "gpt-4o",
    },
    ProviderID.anthropic: {
        "name": "Anthropic Claude",
        "description": "Claude 3.5 Haiku — veloce e preciso",
        "default_model": "claude-3-5-haiku-20241022",
    },
    ProviderID.gemini: {
        "name": "Google Gemini",
        "description": "Gemini 1.5 Flash — multimodale e rapido",
        "default_model": "gemini-1.5-flash",
    },
}


@router.get("", response_model=list[ProviderInfo])
async def list_providers(current_user: UserInDB = Depends(get_current_user)):
    active = ai_service.get_active_provider()
    return [
        ProviderInfo(
            id=pid,
            active=(pid == active),
            **meta,
        )
        for pid, meta in _PROVIDER_META.items()
    ]


@router.put("/active", response_model=ProviderInfo)
async def set_active_provider(
    body: SetProviderRequest,
    _: UserInDB = Depends(require_admin),
):
    ai_service.set_active_provider(body.provider)
    meta = _PROVIDER_META[body.provider]
    return ProviderInfo(id=body.provider, active=True, **meta)
