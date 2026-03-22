"""
AI Service — routes chat requests to the active provider.
Supported: OpenAI, Anthropic Claude, Google Gemini,
           OpenRouter (any model), Custom OpenAI-compatible (Ollama, LM Studio, etc.)

Public functions:
  chat(...)                         → (reply_text, provider_used, fallback_used)
  chat_stream(...)                  → AsyncGenerator[str, None]
  chat_with_config(...)             → (reply_text, provider_label, fallback_used)
  chat_stream_with_config(...)      → AsyncGenerator[str, None]
  chat_with_jenny_config(...)       → backward-compat wrapper for chat_with_config
  chat_stream_with_jenny_config(...) → backward-compat wrapper
  get_companion_config(id)          → CompanionAIConfig
  set_companion_config(cfg)         → None
"""
from __future__ import annotations

import json
import httpx
from typing import AsyncGenerator
from fastapi import HTTPException

from config import settings
from models.message import ChatMessageIn
from models.provider import ProviderID, CompanionAIConfig

_active_provider: ProviderID = settings.ACTIVE_PROVIDER
_FALLBACK_ORDER = [ProviderID.openai, ProviderID.anthropic, ProviderID.gemini]

# Per-companion AI configs (in-memory; updated via PUT /providers/{companion})
_companion_configs: dict[str, CompanionAIConfig] = {
    "alfred": CompanionAIConfig(companion_id="alfred", enabled=False),
    "jenny":  CompanionAIConfig(companion_id="jenny",  enabled=False, use_global=True),
}


# ── Companion config accessors ────────────────────────────────────────────────

def get_companion_config(companion_id: str) -> CompanionAIConfig:
    return _companion_configs.get(companion_id, CompanionAIConfig(companion_id=companion_id))


def set_companion_config(config: CompanionAIConfig) -> None:
    _companion_configs[config.companion_id] = config


# ── Active global provider ────────────────────────────────────────────────────

def get_active_provider() -> ProviderID:
    return _active_provider


def set_active_provider(provider: ProviderID) -> None:
    global _active_provider
    _active_provider = provider


def _get_fallback_chain(primary: ProviderID) -> list[ProviderID]:
    others = [p for p in _FALLBACK_ORDER if p != primary]
    return [primary] + others


# ── Payload builders ──────────────────────────────────────────────────────────

def _openai_msgs(system_prompt: str, messages: list[ChatMessageIn]) -> list[dict]:
    return [{"role": "system", "content": system_prompt}] + [
        {"role": m.role, "content": m.content} for m in messages
    ]


def _anthropic_msgs(messages: list[ChatMessageIn]) -> list[dict]:
    return [{"role": m.role, "content": m.content} for m in messages if m.role != "system"]


def _gemini_payload(system_prompt: str, messages: list[ChatMessageIn]) -> dict:
    role_map = {"user": "user", "assistant": "model", "system": "user"}
    return {
        "system_instruction": {"parts": [{"text": system_prompt}]},
        "contents": [
            {"role": role_map[m.role], "parts": [{"text": m.content}]}
            for m in messages if m.role != "system"
        ],
        "generationConfig": {"maxOutputTokens": 1024},
    }


# ── Non-streaming calls ───────────────────────────────────────────────────────

async def _call_openai(
    system_prompt: str,
    messages: list[ChatMessageIn],
    api_key: str | None = None,
    model_id: str = "gpt-4o",
) -> str:
    key = api_key or settings.OPENAI_API_KEY
    if not key:
        raise HTTPException(503, "OPENAI_API_KEY non configurata")
    async with httpx.AsyncClient(timeout=60) as c:
        r = await c.post(
            "https://api.openai.com/v1/chat/completions",
            headers={"Authorization": f"Bearer {key}"},
            json={"model": model_id, "messages": _openai_msgs(system_prompt, messages), "max_tokens": 1024},
        )
    if r.status_code != 200:
        raise HTTPException(502, f"OpenAI error {r.status_code}: {r.text[:300]}")
    return r.json()["choices"][0]["message"]["content"]


async def _call_anthropic(
    system_prompt: str,
    messages: list[ChatMessageIn],
    api_key: str | None = None,
    model_id: str = "claude-3-5-sonnet-20241022",
) -> str:
    key = api_key or settings.ANTHROPIC_API_KEY
    if not key:
        raise HTTPException(503, "ANTHROPIC_API_KEY non configurata")
    async with httpx.AsyncClient(timeout=60) as c:
        r = await c.post(
            "https://api.anthropic.com/v1/messages",
            headers={"x-api-key": key, "anthropic-version": "2023-06-01"},
            json={"model": model_id, "system": system_prompt,
                  "messages": _anthropic_msgs(messages), "max_tokens": 1024},
        )
    if r.status_code != 200:
        raise HTTPException(502, f"Anthropic error {r.status_code}: {r.text[:300]}")
    return r.json()["content"][0]["text"]


async def _call_gemini(
    system_prompt: str,
    messages: list[ChatMessageIn],
    api_key: str | None = None,
    model_id: str = "gemini-1.5-pro",
) -> str:
    key = api_key or settings.GEMINI_API_KEY
    if not key:
        raise HTTPException(503, "GEMINI_API_KEY non configurata")
    url = (f"https://generativelanguage.googleapis.com/v1beta/"
           f"models/{model_id}:generateContent?key={key}")
    async with httpx.AsyncClient(timeout=60) as c:
        r = await c.post(url, json=_gemini_payload(system_prompt, messages))
    if r.status_code != 200:
        raise HTTPException(502, f"Gemini error {r.status_code}: {r.text[:300]}")
    return r.json()["candidates"][0]["content"]["parts"][0]["text"]


# ── Streaming calls ───────────────────────────────────────────────────────────

async def _stream_openai(
    system_prompt: str,
    messages: list[ChatMessageIn],
    api_key: str | None = None,
    model_id: str = "gpt-4o",
) -> AsyncGenerator[str, None]:
    key = api_key or settings.OPENAI_API_KEY
    if not key:
        raise HTTPException(503, "OPENAI_API_KEY non configurata")
    async with httpx.AsyncClient(timeout=120) as c:
        async with c.stream(
            "POST", "https://api.openai.com/v1/chat/completions",
            headers={"Authorization": f"Bearer {key}"},
            json={"model": model_id, "messages": _openai_msgs(system_prompt, messages),
                  "max_tokens": 1024, "stream": True},
        ) as resp:
            if resp.status_code != 200:
                body = await resp.aread()
                raise HTTPException(502, f"OpenAI stream error {resp.status_code}: {body[:200]}")
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    raw = line[6:]
                    if raw == "[DONE]":
                        return
                    try:
                        delta = json.loads(raw)["choices"][0]["delta"].get("content", "")
                        if delta:
                            yield delta
                    except (json.JSONDecodeError, KeyError, IndexError):
                        pass


async def _stream_anthropic(
    system_prompt: str,
    messages: list[ChatMessageIn],
    api_key: str | None = None,
    model_id: str = "claude-3-5-sonnet-20241022",
) -> AsyncGenerator[str, None]:
    key = api_key or settings.ANTHROPIC_API_KEY
    if not key:
        raise HTTPException(503, "ANTHROPIC_API_KEY non configurata")
    async with httpx.AsyncClient(timeout=120) as c:
        async with c.stream(
            "POST", "https://api.anthropic.com/v1/messages",
            headers={"x-api-key": key, "anthropic-version": "2023-06-01"},
            json={"model": model_id, "system": system_prompt,
                  "messages": _anthropic_msgs(messages), "max_tokens": 1024, "stream": True},
        ) as resp:
            if resp.status_code != 200:
                body = await resp.aread()
                raise HTTPException(502, f"Anthropic stream error {resp.status_code}: {body[:200]}")
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    try:
                        event = json.loads(line[6:])
                        if event.get("type") == "content_block_delta":
                            text = event.get("delta", {}).get("text", "")
                            if text:
                                yield text
                    except (json.JSONDecodeError, KeyError):
                        pass


async def _stream_gemini(
    system_prompt: str,
    messages: list[ChatMessageIn],
    api_key: str | None = None,
    model_id: str = "gemini-1.5-pro",
) -> AsyncGenerator[str, None]:
    key = api_key or settings.GEMINI_API_KEY
    if not key:
        raise HTTPException(503, "GEMINI_API_KEY non configurata")
    url = (f"https://generativelanguage.googleapis.com/v1beta/"
           f"models/{model_id}:streamGenerateContent?alt=sse&key={key}")
    async with httpx.AsyncClient(timeout=120) as c:
        async with c.stream("POST", url, json=_gemini_payload(system_prompt, messages)) as resp:
            if resp.status_code != 200:
                body = await resp.aread()
                raise HTTPException(502, f"Gemini stream error {resp.status_code}: {body[:200]}")
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    try:
                        text = json.loads(line[6:])["candidates"][0]["content"]["parts"][0]["text"]
                        if text:
                            yield text
                    except (json.JSONDecodeError, KeyError, IndexError):
                        pass


# ── Dispatch tables ───────────────────────────────────────────────────────────

_CALL = {
    ProviderID.openai:     _call_openai,
    ProviderID.anthropic:  _call_anthropic,
    ProviderID.gemini:     _call_gemini,
}
_STREAM = {
    ProviderID.openai:     _stream_openai,
    ProviderID.anthropic:  _stream_anthropic,
    ProviderID.gemini:     _stream_gemini,
}


# ── Public API — global provider chain ───────────────────────────────────────

async def chat(
    system_prompt: str,
    messages: list[ChatMessageIn],
    provider: ProviderID | None = None,
    with_fallback: bool = True,
) -> tuple[str, ProviderID, bool]:
    """Returns (reply, provider_used, fallback_was_used)."""
    primary = provider or _active_provider
    chain = _get_fallback_chain(primary) if with_fallback else [primary]
    last_exc: Exception = RuntimeError("No provider available")
    for pid in chain:
        fn = _CALL.get(pid)
        if not fn:
            continue
        try:
            reply = await fn(system_prompt, messages)
            return reply, pid, (pid != primary)
        except HTTPException as e:
            if e.status_code in (502, 503):
                last_exc = e
            else:
                raise
    raise last_exc


async def chat_stream(
    system_prompt: str,
    messages: list[ChatMessageIn],
    provider: ProviderID | None = None,
    with_fallback: bool = True,
) -> AsyncGenerator[str, None]:
    """
    Yields raw text chunks.
    Special control tokens (prefixed \\x00):
      \\x00PROVIDER:<id>   — which provider is now responding
      \\x00FALLBACK:<id>   — fallback was triggered
    """
    primary = provider or _active_provider
    chain = _get_fallback_chain(primary) if with_fallback else [primary]
    last_exc: Exception = RuntimeError("No provider available")
    for pid in chain:
        stream_fn = _STREAM.get(pid)
        if not stream_fn:
            continue
        try:
            yield f"\x00PROVIDER:{pid.value}"
            if pid != primary:
                yield f"\x00FALLBACK:{pid.value}"
            async for chunk in stream_fn(system_prompt, messages):
                yield chunk
            return
        except HTTPException as e:
            if e.status_code in (502, 503):
                last_exc = e
            else:
                raise
    raise last_exc


# ── OpenRouter provider ───────────────────────────────────────────────────────

_OPENROUTER_BASE = "https://openrouter.ai/api/v1"
_OPENROUTER_HEADERS = {
    "HTTP-Referer": "https://alfred-jenny-app.local",
    "X-Title": "Alfred Jenny App",
}


async def _call_openrouter(
    api_key: str, model: str, system_prompt: str, messages: list[ChatMessageIn]
) -> str:
    headers = {"Authorization": f"Bearer {api_key}", **_OPENROUTER_HEADERS}
    async with httpx.AsyncClient(timeout=60) as c:
        r = await c.post(
            f"{_OPENROUTER_BASE}/chat/completions",
            headers=headers,
            json={"model": model, "messages": _openai_msgs(system_prompt, messages), "max_tokens": 1024},
        )
    if r.status_code != 200:
        raise HTTPException(502, f"OpenRouter error {r.status_code}: {r.text[:300]}")
    return r.json()["choices"][0]["message"]["content"]


async def _stream_openrouter(
    api_key: str, model: str, system_prompt: str, messages: list[ChatMessageIn]
) -> AsyncGenerator[str, None]:
    headers = {"Authorization": f"Bearer {api_key}", **_OPENROUTER_HEADERS}
    async with httpx.AsyncClient(timeout=120) as c:
        async with c.stream(
            "POST", f"{_OPENROUTER_BASE}/chat/completions",
            headers=headers,
            json={"model": model, "messages": _openai_msgs(system_prompt, messages),
                  "max_tokens": 1024, "stream": True},
        ) as resp:
            if resp.status_code != 200:
                body = await resp.aread()
                raise HTTPException(502, f"OpenRouter stream error {resp.status_code}: {body[:200]}")
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    raw = line[6:]
                    if raw == "[DONE]":
                        return
                    try:
                        delta = json.loads(raw)["choices"][0]["delta"].get("content", "")
                        if delta:
                            yield delta
                    except (json.JSONDecodeError, KeyError, IndexError):
                        pass


# ── Custom OpenAI-compatible provider ─────────────────────────────────────────

async def _call_custom(
    base_url: str, api_key: str, model: str, system_prompt: str, messages: list[ChatMessageIn]
) -> str:
    headers = {"Content-Type": "application/json"}
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"
    url = base_url.rstrip("/") + "/chat/completions"
    async with httpx.AsyncClient(timeout=60) as c:
        r = await c.post(url, headers=headers,
                         json={"model": model, "messages": _openai_msgs(system_prompt, messages), "max_tokens": 1024})
    if r.status_code != 200:
        raise HTTPException(502, f"Custom AI error {r.status_code}: {r.text[:300]}")
    return r.json()["choices"][0]["message"]["content"]


async def _stream_custom(
    base_url: str, api_key: str, model: str, system_prompt: str, messages: list[ChatMessageIn]
) -> AsyncGenerator[str, None]:
    headers = {"Content-Type": "application/json"}
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"
    url = base_url.rstrip("/") + "/chat/completions"
    async with httpx.AsyncClient(timeout=120) as c:
        async with c.stream(
            "POST", url, headers=headers,
            json={"model": model, "messages": _openai_msgs(system_prompt, messages),
                  "max_tokens": 1024, "stream": True},
        ) as resp:
            if resp.status_code != 200:
                body = await resp.aread()
                raise HTTPException(502, f"Custom stream error {resp.status_code}: {body[:200]}")
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    raw = line[6:]
                    if raw == "[DONE]":
                        return
                    try:
                        delta = json.loads(raw)["choices"][0]["delta"].get("content", "")
                        if delta:
                            yield delta
                    except (json.JSONDecodeError, KeyError, IndexError):
                        pass


# ── Unified companion-config-based chat ───────────────────────────────────────

_DEFAULT_MODEL: dict[str, str] = {
    "openai":     "gpt-4o",
    "anthropic":  "claude-3-5-sonnet-20241022",
    "gemini":     "gemini-1.5-pro",
    "openrouter": "",
    "custom":     "",
}


async def chat_with_config(
    system_prompt: str,
    messages: list[ChatMessageIn],
    config: CompanionAIConfig,
    with_fallback: bool = True,
) -> tuple[str, str, bool]:
    """Route through a CompanionAIConfig. Returns (reply, provider_label, fallback_used)."""
    if not config or not config.enabled:
        reply, pid, fb = await chat(system_prompt, messages, with_fallback=with_fallback)
        return reply, pid.value, fb

    pt = config.provider_type
    key = config.api_key or None
    mid = config.model_id or _DEFAULT_MODEL.get(pt, "")

    try:
        if pt == "openrouter":
            if not config.api_key:
                raise HTTPException(422, "OpenRouter richiede api_key")
            if not config.model_id:
                raise HTTPException(422, "OpenRouter richiede model_id")
            reply = await _call_openrouter(config.api_key, config.model_id, system_prompt, messages)
            return reply, f"openrouter/{config.model_id}", False

        elif pt == "custom":
            if not config.base_url or not config.model_id:
                raise HTTPException(422, "Custom richiede base_url e model_id")
            reply = await _call_custom(config.base_url, config.api_key or "", config.model_id, system_prompt, messages)
            return reply, f"custom/{config.model_id}", False

        elif pt == "anthropic":
            reply = await _call_anthropic(system_prompt, messages, api_key=key, model_id=mid or "claude-3-5-sonnet-20241022")
            return reply, f"anthropic/{mid}", False

        elif pt == "gemini":
            reply = await _call_gemini(system_prompt, messages, api_key=key, model_id=mid or "gemini-1.5-pro")
            return reply, f"gemini/{mid}", False

        else:  # openai (default)
            reply = await _call_openai(system_prompt, messages, api_key=key, model_id=mid or "gpt-4o")
            return reply, f"openai/{mid}", False

    except HTTPException as e:
        if with_fallback and e.status_code in (502, 503, 422):
            reply, pid, _ = await chat(system_prompt, messages, with_fallback=with_fallback)
            return reply, pid.value, True
        raise


async def chat_stream_with_config(
    system_prompt: str,
    messages: list[ChatMessageIn],
    config: CompanionAIConfig,
    with_fallback: bool = True,
) -> AsyncGenerator[str, None]:
    """Stream through a CompanionAIConfig. Yields text chunks + control tokens."""
    if not config or not config.enabled:
        async for chunk in chat_stream(system_prompt, messages, with_fallback=with_fallback):
            yield chunk
        return

    pt = config.provider_type
    key = config.api_key or None
    mid = config.model_id or _DEFAULT_MODEL.get(pt, "")

    try:
        if pt == "openrouter":
            if not config.api_key or not config.model_id:
                raise HTTPException(422, "OpenRouter richiede api_key e model_id")
            yield f"\x00PROVIDER:openrouter/{config.model_id}"
            async for chunk in _stream_openrouter(config.api_key, config.model_id, system_prompt, messages):
                yield chunk

        elif pt == "custom":
            if not config.base_url or not config.model_id:
                raise HTTPException(422, "Custom richiede base_url e model_id")
            yield f"\x00PROVIDER:custom/{config.model_id}"
            async for chunk in _stream_custom(config.base_url, config.api_key or "", config.model_id, system_prompt, messages):
                yield chunk

        elif pt == "anthropic":
            m = mid or "claude-3-5-sonnet-20241022"
            yield f"\x00PROVIDER:anthropic/{m}"
            async for chunk in _stream_anthropic(system_prompt, messages, api_key=key, model_id=m):
                yield chunk

        elif pt == "gemini":
            m = mid or "gemini-1.5-pro"
            yield f"\x00PROVIDER:gemini/{m}"
            async for chunk in _stream_gemini(system_prompt, messages, api_key=key, model_id=m):
                yield chunk

        else:  # openai
            m = mid or "gpt-4o"
            yield f"\x00PROVIDER:openai/{m}"
            async for chunk in _stream_openai(system_prompt, messages, api_key=key, model_id=m):
                yield chunk

    except HTTPException as e:
        if with_fallback and e.status_code in (502, 503, 422):
            async for chunk in chat_stream(system_prompt, messages, with_fallback=with_fallback):
                yield chunk
        else:
            raise


# ── Jenny-specific functions (backward-compat wrappers) ───────────────────────

async def chat_with_jenny_config(
    system_prompt: str,
    messages: list[ChatMessageIn],
    jenny_config,   # models.message.JennyAIConfig | None
    with_fallback: bool = True,
) -> tuple[str, str, bool]:
    """Backward-compat wrapper: converts JennyAIConfig → CompanionAIConfig."""
    if not jenny_config or not jenny_config.enabled or not jenny_config.api_key or not jenny_config.model_id:
        reply, pid, fb = await chat(system_prompt, messages, with_fallback=with_fallback)
        return reply, pid.value, fb
    cfg = CompanionAIConfig(
        companion_id="jenny",
        provider_type=jenny_config.provider_type,
        api_key=jenny_config.api_key,
        model_id=jenny_config.model_id,
        base_url=jenny_config.base_url or None,
        enabled=True,
        use_global=False,
    )
    return await chat_with_config(system_prompt, messages, cfg, with_fallback)


async def chat_stream_with_jenny_config(
    system_prompt: str,
    messages: list[ChatMessageIn],
    jenny_config,   # models.message.JennyAIConfig | None
    with_fallback: bool = True,
) -> AsyncGenerator[str, None]:
    """Backward-compat wrapper: converts JennyAIConfig → CompanionAIConfig."""
    if not jenny_config or not jenny_config.enabled or not jenny_config.api_key or not jenny_config.model_id:
        async for chunk in chat_stream(system_prompt, messages, with_fallback=with_fallback):
            yield chunk
        return
    cfg = CompanionAIConfig(
        companion_id="jenny",
        provider_type=jenny_config.provider_type,
        api_key=jenny_config.api_key,
        model_id=jenny_config.model_id,
        base_url=jenny_config.base_url or None,
        enabled=True,
        use_global=False,
    )
    async for chunk in chat_stream_with_config(system_prompt, messages, cfg, with_fallback):
        yield chunk
