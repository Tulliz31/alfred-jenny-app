from fastapi import APIRouter, Depends

from models.provider import ProviderID, ProviderInfo, SetProviderRequest
from models.user import UserInDB
from services.auth_service import get_current_user, require_admin
from services import ai_service

router = APIRouter(prefix="/providers", tags=["providers"])

_PROVIDER_META: dict[ProviderID, dict] = {
    ProviderID.openai: {
        "name": "OpenAI GPT-4o",
        "description": "GPT-4o — potente, versatile, ottimo per task complessi",
        "default_model": "gpt-4o",
        "price_per_1k_input": 0.005,
        "price_per_1k_output": 0.015,
        "avg_latency_ms": 1500,
    },
    ProviderID.anthropic: {
        "name": "Anthropic Claude",
        "description": "Claude 3.5 Sonnet — eccellente nel ragionamento e nel codice",
        "default_model": "claude-3-5-sonnet-20241022",
        "price_per_1k_input": 0.003,
        "price_per_1k_output": 0.015,
        "avg_latency_ms": 1200,
    },
    ProviderID.gemini: {
        "name": "Google Gemini",
        "description": "Gemini 1.5 Pro — multimodale, veloce, contesto lungo",
        "default_model": "gemini-1.5-pro",
        "price_per_1k_input": 0.00125,
        "price_per_1k_output": 0.005,
        "avg_latency_ms": 1000,
    },
}


@router.get("", response_model=list[ProviderInfo])
async def list_providers(current_user: UserInDB = Depends(get_current_user)):
    active = ai_service.get_active_provider()
    return [
        ProviderInfo(id=pid, active=(pid == active), **meta)
        for pid, meta in _PROVIDER_META.items()
    ]


@router.put("/active", response_model=ProviderInfo)
async def set_active_provider(body: SetProviderRequest, _: UserInDB = Depends(require_admin)):
    ai_service.set_active_provider(body.provider)
    meta = _PROVIDER_META[body.provider]
    return ProviderInfo(id=body.provider, active=True, **meta)
