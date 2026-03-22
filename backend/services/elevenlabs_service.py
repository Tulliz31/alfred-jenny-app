"""
ElevenLabs helper functions: voices list, preview generation (with cache),
subscription info.
"""
import asyncio
import base64
import time
from typing import Optional

import httpx

ELEVENLABS_BASE = "https://api.elevenlabs.io/v1"

# In-memory preview cache: voice_id -> (audio_bytes, timestamp)
_preview_cache: dict[str, tuple[bytes, float]] = {}
_CACHE_TTL_SECONDS = 3600  # 1 hour

PREVIEW_TEXT = (
    "Ciao! Sono la tua assistente vocale. Come posso aiutarti oggi?"
)


def _headers(api_key: str) -> dict:
    return {"xi-api-key": api_key, "Content-Type": "application/json"}


async def get_voices(api_key: str) -> list[dict]:
    """Return the list of voices available for the given API key."""
    url = f"{ELEVENLABS_BASE}/voices"
    async with httpx.AsyncClient(timeout=15.0) as client:
        resp = await client.get(url, headers={"xi-api-key": api_key})
    resp.raise_for_status()
    raw = resp.json().get("voices", [])
    result = []
    for v in raw:
        labels = v.get("labels") or {}
        result.append({
            "voice_id":    v.get("voice_id", ""),
            "name":        v.get("name", ""),
            "category":    v.get("category", ""),
            "description": v.get("description") or "",
            "preview_url": v.get("preview_url") or "",
            "accent":      labels.get("accent", ""),
            "gender":      labels.get("gender", ""),
            "age":         labels.get("age", ""),
            "use_case":    labels.get("use_case", ""),
        })
    return result


async def generate_preview(voice_id: str, api_key: str) -> str:
    """
    Generate a short speech preview for the given voice.
    Returns audio as base64-encoded MP3.
    Caches results for 1 hour per voice_id.
    """
    now = time.monotonic()
    cached = _preview_cache.get(voice_id)
    if cached and (now - cached[1]) < _CACHE_TTL_SECONDS:
        return base64.b64encode(cached[0]).decode("utf-8")

    url = f"{ELEVENLABS_BASE}/text-to-speech/{voice_id}"
    payload = {
        "text": PREVIEW_TEXT,
        "model_id": "eleven_multilingual_v2",
        "voice_settings": {
            "stability": 0.5,
            "similarity_boost": 0.75,
            "style": 0.0,
            "use_speaker_boost": True,
        },
    }
    headers = {**_headers(api_key), "Accept": "audio/mpeg"}
    async with httpx.AsyncClient(timeout=30.0) as client:
        resp = await client.post(url, json=payload, headers=headers)
    resp.raise_for_status()
    audio_bytes = resp.content
    _preview_cache[voice_id] = (audio_bytes, now)
    return base64.b64encode(audio_bytes).decode("utf-8")


async def get_subscription(api_key: str) -> dict:
    """Return subscription/usage info for the given API key."""
    url = f"{ELEVENLABS_BASE}/user/subscription"
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.get(url, headers={"xi-api-key": api_key})
    resp.raise_for_status()
    data = resp.json()
    return {
        "tier":               data.get("tier", "free"),
        "character_count":    data.get("character_count", 0),
        "character_limit":    data.get("character_limit", 10000),
        "next_reset":         data.get("next_character_count_reset_unix", 0),
    }
