"""
Jenny-specific admin endpoints:
  POST /jenny/openrouter/models   — returns OpenRouter model list (1-hour cache)
  POST /jenny/test                — tests Jenny AI config with a sample message
"""
import time
import httpx
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from models.message import JennyAIConfig, ChatMessageIn, Role
from models.user import UserInDB, Role as UserRole
from services.auth_service import get_current_user
from services import ai_service

router = APIRouter(prefix="/jenny", tags=["jenny"])

# ── OpenRouter models cache ───────────────────────────────────────────────────

_models_cache: dict = {}           # api_key → list[dict]
_models_cache_at: dict = {}        # api_key → float (timestamp)
_CACHE_TTL = 3600                  # 1 hour


class OpenRouterKeyRequest(BaseModel):
    api_key: str


class OpenRouterModelOut(BaseModel):
    id: str
    name: str
    description: str = ""
    context_length: int = 0
    prompt_cost: float = 0.0        # USD per 1M tokens
    completion_cost: float = 0.0    # USD per 1M tokens
    is_free: bool = False


@router.post("/openrouter/models", response_model=list[OpenRouterModelOut])
async def get_openrouter_models(
    body: OpenRouterKeyRequest,
    current_user: UserInDB = Depends(get_current_user),
):
    """Returns available OpenRouter models. Admin only. Results cached 1 hour."""
    if current_user.role != UserRole.admin:
        raise HTTPException(403, "Solo gli admin possono accedere alla lista modelli")
    if not body.api_key:
        raise HTTPException(422, "api_key obbligatoria")

    now = time.time()
    key = body.api_key[:16]  # use prefix as cache key (don't store full key)
    if key in _models_cache and now - _models_cache_at.get(key, 0) < _CACHE_TTL:
        return _models_cache[key]

    async with httpx.AsyncClient(timeout=30) as c:
        r = await c.get(
            "https://openrouter.ai/api/v1/models",
            headers={
                "Authorization": f"Bearer {body.api_key}",
                "HTTP-Referer": "https://alfred-jenny-app.local",
            },
        )
    if r.status_code != 200:
        raise HTTPException(502, f"OpenRouter models error {r.status_code}: {r.text[:200]}")

    raw_models = r.json().get("data", [])
    result: list[OpenRouterModelOut] = []
    for m in raw_models:
        pricing = m.get("pricing", {})
        try:
            prompt_cost = float(pricing.get("prompt") or 0) * 1_000_000
            completion_cost = float(pricing.get("completion") or 0) * 1_000_000
        except (TypeError, ValueError):
            prompt_cost = 0.0
            completion_cost = 0.0
        result.append(OpenRouterModelOut(
            id=m.get("id", ""),
            name=m.get("name", m.get("id", "")),
            description=m.get("description") or "",
            context_length=m.get("context_length") or 0,
            prompt_cost=round(prompt_cost, 6),
            completion_cost=round(completion_cost, 6),
            is_free=prompt_cost == 0 and completion_cost == 0,
        ))

    # Sort: free first, then by prompt cost ascending
    result.sort(key=lambda x: (not x.is_free, x.prompt_cost))
    _models_cache[key] = result
    _models_cache_at[key] = now
    return result


# ── Test Jenny AI config ──────────────────────────────────────────────────────

class TestJennyRequest(BaseModel):
    jenny_ai_config: JennyAIConfig


class TestJennyResponse(BaseModel):
    reply: str
    provider: str


@router.post("/test", response_model=TestJennyResponse)
async def test_jenny_provider(
    body: TestJennyRequest,
    current_user: UserInDB = Depends(get_current_user),
):
    """Sends a test message to the configured Jenny AI and returns the response."""
    if current_user.role != UserRole.admin:
        raise HTTPException(403, "Solo gli admin possono testare la configurazione")

    cfg = body.jenny_ai_config
    if not cfg.enabled or not cfg.api_key or not cfg.model_id:
        raise HTTPException(422, "Configurazione Jenny AI incompleta (enabled, api_key e model_id obbligatori)")

    from services.companion_service import build_jenny_system_prompt
    system_prompt = build_jenny_system_prompt(3)
    test_messages = [ChatMessageIn(role=Role.user, content="Ciao Jenny, come stai?")]

    reply, provider_label, _ = await ai_service.chat_with_jenny_config(
        system_prompt=system_prompt,
        messages=test_messages,
        jenny_config=cfg,
        with_fallback=False,
    )
    return TestJennyResponse(reply=reply, provider=provider_label)
