"""
ElevenLabs Text-to-Speech service.
Returns audio as a base64-encoded MP3 string.
"""
import base64

import httpx
from fastapi import HTTPException, status

ELEVENLABS_TTS_URL = "https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"


async def text_to_speech(text: str, voice_id: str, api_key: str) -> str:
    """
    Call ElevenLabs TTS API and return the audio as a base64-encoded string.
    Raises HTTP 502 if ElevenLabs responds with an error.
    """
    url = ELEVENLABS_TTS_URL.format(voice_id=voice_id)
    headers = {
        "xi-api-key": api_key,
        "Content-Type": "application/json",
        "Accept": "audio/mpeg",
    }
    payload = {
        "text": text,
        "model_id": "eleven_multilingual_v2",
        "voice_settings": {
            "stability": 0.50,
            "similarity_boost": 0.75,
            "style": 0.0,
            "use_speaker_boost": True,
        },
    }

    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.post(url, headers=headers, json=payload)

    if response.status_code != 200:
        snippet = response.text[:300]
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"ElevenLabs error {response.status_code}: {snippet}",
        )

    return base64.b64encode(response.content).decode("utf-8")
