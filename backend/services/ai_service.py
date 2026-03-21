"""
AI Service — routes chat requests to the active provider.
Supported: OpenAI, Anthropic, Google Gemini.
"""
from __future__ import annotations

import httpx
from fastapi import HTTPException, status

from config import settings
from models.message import ChatMessageIn
from models.provider import ProviderID

# Runtime mutable state — single active provider
_active_provider: ProviderID = settings.ACTIVE_PROVIDER


def get_active_provider() -> ProviderID:
    return _active_provider


def set_active_provider(provider: ProviderID) -> None:
    global _active_provider
    _active_provider = provider


# ── OpenAI ───────────────────────────────────────────────────────────────────

async def _call_openai(system_prompt: str, messages: list[ChatMessageIn]) -> str:
    if not settings.OPENAI_API_KEY:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail="OPENAI_API_KEY non configurata")
    payload = {
        "model": "gpt-4o",
        "messages": [{"role": "system", "content": system_prompt}]
                   + [{"role": m.role, "content": m.content} for m in messages],
        "max_tokens": 1024,
    }
    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.post(
            "https://api.openai.com/v1/chat/completions",
            headers={"Authorization": f"Bearer {settings.OPENAI_API_KEY}"},
            json=payload,
        )
    if r.status_code != 200:
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY,
                            detail=f"OpenAI error {r.status_code}: {r.text}")
    data = r.json()
    return data["choices"][0]["message"]["content"]


# ── Anthropic ────────────────────────────────────────────────────────────────

async def _call_anthropic(system_prompt: str, messages: list[ChatMessageIn]) -> str:
    if not settings.ANTHROPIC_API_KEY:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail="ANTHROPIC_API_KEY non configurata")
    payload = {
        "model": "claude-3-5-haiku-20241022",
        "system": system_prompt,
        "messages": [{"role": m.role, "content": m.content} for m in messages
                     if m.role != "system"],
        "max_tokens": 1024,
    }
    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.post(
            "https://api.anthropic.com/v1/messages",
            headers={
                "x-api-key": settings.ANTHROPIC_API_KEY,
                "anthropic-version": "2023-06-01",
            },
            json=payload,
        )
    if r.status_code != 200:
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY,
                            detail=f"Anthropic error {r.status_code}: {r.text}")
    data = r.json()
    return data["content"][0]["text"]


# ── Google Gemini ────────────────────────────────────────────────────────────

async def _call_gemini(system_prompt: str, messages: list[ChatMessageIn]) -> str:
    if not settings.GEMINI_API_KEY:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail="GEMINI_API_KEY non configurata")

    # Gemini uses "parts" format; map roles (user/assistant → user/model)
    role_map = {"user": "user", "assistant": "model", "system": "user"}
    contents = []
    for m in messages:
        if m.role == "system":
            continue
        contents.append({"role": role_map[m.role], "parts": [{"text": m.content}]})

    payload = {
        "system_instruction": {"parts": [{"text": system_prompt}]},
        "contents": contents,
        "generationConfig": {"maxOutputTokens": 1024},
    }
    url = (
        f"https://generativelanguage.googleapis.com/v1beta/"
        f"models/gemini-1.5-flash:generateContent?key={settings.GEMINI_API_KEY}"
    )
    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.post(url, json=payload)
    if r.status_code != 200:
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY,
                            detail=f"Gemini error {r.status_code}: {r.text}")
    data = r.json()
    return data["candidates"][0]["content"]["parts"][0]["text"]


# ── Public entry point ────────────────────────────────────────────────────────

async def chat(
    system_prompt: str,
    messages: list[ChatMessageIn],
    provider: ProviderID | None = None,
) -> str:
    active = provider or _active_provider
    dispatch = {
        ProviderID.openai: _call_openai,
        ProviderID.anthropic: _call_anthropic,
        ProviderID.gemini: _call_gemini,
    }
    fn = dispatch.get(active)
    if fn is None:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST,
                            detail=f"Provider sconosciuto: {active}")
    return await fn(system_prompt, messages)
