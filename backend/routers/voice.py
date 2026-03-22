from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from typing import Optional

import httpx
from models.user import UserInDB
from services.auth_service import get_current_user
from services import voice_service, elevenlabs_service
from config import settings

router = APIRouter(prefix="/voice", tags=["voice"])

DEFAULT_VOICE_ID = "pNInz6obpgDQGcFmaJgB"  # Adam (ElevenLabs)


class VoiceSpeakRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=2000)
    voice_id: str = DEFAULT_VOICE_ID
    api_key: Optional[str] = None  # if None, server-side key from .env is used


class VoiceSpeakResponse(BaseModel):
    audio_base64: str
    format: str = "mp3"
    voice_id: str


class ElevenLabsKeyRequest(BaseModel):
    api_key: Optional[str] = None


@router.post("/speak", response_model=VoiceSpeakResponse)
async def speak(
    body: VoiceSpeakRequest,
    current_user: UserInDB = Depends(get_current_user),
):
    """
    Convert text to speech via ElevenLabs.
    The client may supply its own API key; otherwise the server-side key is used.
    """
    api_key = body.api_key or settings.ELEVENLABS_API_KEY
    if not api_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="ElevenLabs API key non configurata. Inseriscila nelle impostazioni o aggiungi ELEVENLABS_API_KEY al .env del backend.",
        )

    audio_b64 = await voice_service.text_to_speech(body.text, body.voice_id, api_key)
    return VoiceSpeakResponse(audio_base64=audio_b64, voice_id=body.voice_id)


@router.get("/voices")
async def list_voices(
    api_key: Optional[str] = None,
    current_user: UserInDB = Depends(get_current_user),
):
    """Return all ElevenLabs voices available for the given API key."""
    key = api_key or settings.ELEVENLABS_API_KEY
    if not key:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail="ElevenLabs API key non configurata.")
    try:
        voices = await elevenlabs_service.get_voices(key)
        return {"voices": voices}
    except httpx.HTTPStatusError as e:
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY,
                            detail=f"ElevenLabs error: {e.response.text[:200]}")


@router.post("/preview/{voice_id}")
async def preview_voice(
    voice_id: str,
    body: ElevenLabsKeyRequest,
    current_user: UserInDB = Depends(get_current_user),
):
    """Generate a short audio preview for the given voice (cached 1 h)."""
    key = body.api_key or settings.ELEVENLABS_API_KEY
    if not key:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail="ElevenLabs API key non configurata.")
    try:
        audio_b64 = await elevenlabs_service.generate_preview(voice_id, key)
        return {"audio_base64": audio_b64, "format": "mp3", "voice_id": voice_id}
    except httpx.HTTPStatusError as e:
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY,
                            detail=f"ElevenLabs error: {e.response.text[:200]}")


@router.get("/subscription")
async def subscription_info(
    api_key: Optional[str] = None,
    current_user: UserInDB = Depends(get_current_user),
):
    """Return subscription and character-usage info for the given API key."""
    key = api_key or settings.ELEVENLABS_API_KEY
    if not key:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail="ElevenLabs API key non configurata.")
    try:
        return await elevenlabs_service.get_subscription(key)
    except httpx.HTTPStatusError as e:
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY,
                            detail=f"ElevenLabs error: {e.response.text[:200]}")
