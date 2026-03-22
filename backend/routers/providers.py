from fastapi import APIRouter, Depends, Query, HTTPException
from pydantic import BaseModel

from models.provider import ProviderID, ProviderInfo, SetProviderRequest, CompanionAIConfig
from models.user import UserInDB
from services.auth_service import get_current_user, require_admin
from services import ai_service

import httpx

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


class OpenRouterModelOut(BaseModel):
    id: str
    name: str
    description: str | None = None
    context_length: int | None = None
    prompt_cost: float | None = None
    completion_cost: float | None = None
    is_free: bool = False


class TestProviderRequest(BaseModel):
    config: CompanionAIConfig


class TestProviderResponse(BaseModel):
    reply: str
    provider: str


# ── Global provider list ──────────────────────────────────────────────────────

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


# ── Companion AI configs ──────────────────────────────────────────────────────

@router.get("/alfred", response_model=CompanionAIConfig)
async def get_alfred_config(_: UserInDB = Depends(get_current_user)):
    return ai_service.get_companion_config("alfred")


@router.put("/alfred", response_model=CompanionAIConfig)
async def set_alfred_config(body: CompanionAIConfig, _: UserInDB = Depends(require_admin)):
    config = body.model_copy(update={"companion_id": "alfred"})
    ai_service.set_companion_config(config)
    return config


@router.get("/jenny", response_model=CompanionAIConfig)
async def get_jenny_config(_: UserInDB = Depends(get_current_user)):
    return ai_service.get_companion_config("jenny")


@router.put("/jenny", response_model=CompanionAIConfig)
async def set_jenny_config(body: CompanionAIConfig, _: UserInDB = Depends(require_admin)):
    config = body.model_copy(update={"companion_id": "jenny"})
    ai_service.set_companion_config(config)
    return config


# ── OpenRouter model browser ──────────────────────────────────────────────────

@router.get("/openrouter/models", response_model=list[OpenRouterModelOut])
async def get_openrouter_models(
    api_key: str = Query(..., description="OpenRouter API key"),
    _: UserInDB = Depends(get_current_user),
):
    try:
        async with httpx.AsyncClient(timeout=15) as c:
            r = await c.get(
                "https://openrouter.ai/api/v1/models",
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "HTTP-Referer": "https://alfred-jenny-app.local",
                },
            )
        if r.status_code != 200:
            raise HTTPException(502, f"OpenRouter error {r.status_code}: {r.text[:200]}")
        data = r.json().get("data", [])
        models: list[OpenRouterModelOut] = []
        for m in data:
            pricing = m.get("pricing", {})
            try:
                prompt_cost = float(pricing.get("prompt", 0)) * 1_000_000
                compl_cost  = float(pricing.get("completion", 0)) * 1_000_000
            except (TypeError, ValueError):
                prompt_cost = compl_cost = 0.0
            models.append(OpenRouterModelOut(
                id=m.get("id", ""),
                name=m.get("name", m.get("id", "")),
                description=m.get("description"),
                context_length=m.get("context_length"),
                prompt_cost=prompt_cost,
                completion_cost=compl_cost,
                is_free=(prompt_cost == 0.0 and compl_cost == 0.0),
            ))
        return models
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(502, f"Errore recupero modelli: {str(e)[:200]}")


# ── Provider test ─────────────────────────────────────────────────────────────

@router.post("/test", response_model=TestProviderResponse)
async def test_provider_config(
    body: TestProviderRequest,
    _: UserInDB = Depends(get_current_user),
):
    from models.message import ChatMessageIn, Role
    test_messages = [
        ChatMessageIn(role=Role.user, content="Ciao! Rispondi in una frase brevissima.")
    ]
    try:
        reply, provider_label, _ = await ai_service.chat_with_config(
            system_prompt="Sei un assistente AI utile.",
            messages=test_messages,
            config=body.config,
            with_fallback=False,
        )
        return TestProviderResponse(reply=reply, provider=provider_label)
    except HTTPException as e:
        raise
    except Exception as e:
        raise HTTPException(502, str(e)[:300])
