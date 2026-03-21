from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from typing import Optional

from models.user import UserInDB
from services.auth_service import get_current_user
from services import voice_service
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
