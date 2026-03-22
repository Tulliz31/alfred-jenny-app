import json
import re
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse

from models.message import ChatRequest, ChatResponse, SummarizeRequest, SummarizeResponse, ChatMessageIn, Role
from models.provider import ProviderID
from models.user import UserInDB
from services.auth_service import get_current_user
from services.companion_service import get_companion, get_companions_for_role, build_jenny_system_prompt
from services import ai_service
from smart_home import tuya_service

router = APIRouter(prefix="/chat", tags=["chat"])

_TUYA_PATTERN = re.compile(r"\[TUYA:(\{[^]]+\})\]", re.DOTALL)


def _strip_tuya(text: str) -> str:
    """Remove [TUYA:{...}] markers from the visible reply."""
    return _TUYA_PATTERN.sub("", text).strip()


async def _execute_tuya_if_present(text: str) -> None:
    """Parse and execute any [TUYA:{...}] command embedded in an AI reply."""
    for m in _TUYA_PATTERN.finditer(text):
        try:
            cmd = json.loads(m.group(1))
            device_id = cmd.get("device_id", "")
            action = cmd.get("action", "")
            value = cmd.get("value")
            if device_id and action:
                await tuya_service.send_command(device_id, action, value)
        except Exception:
            pass

_SUMMARY_SYSTEM = (
    "Sei un assistente che crea riassunti concisi di conversazioni. "
    "Il riassunto deve essere in italiano, max 200 parole, e includere: "
    "i temi principali trattati, le decisioni prese, il contesto rilevante per il futuro."
)


async def _resolve_companion_and_prompt(body: ChatRequest, current_user: UserInDB):
    allowed = {c.id for c in get_companions_for_role(current_user.role)}
    if body.companion_id not in allowed:
        raise HTTPException(403, f"Non hai accesso al companion '{body.companion_id}'")
    companion = get_companion(body.companion_id)
    if companion is None:
        raise HTTPException(404, f"Companion '{body.companion_id}' non trovato")
    if not body.messages:
        raise HTTPException(422, "messages non può essere vuoto")
    if companion.id == "jenny":
        system_prompt = build_jenny_system_prompt(max(1, min(5, body.personality_level)))
    else:
        system_prompt = companion.system_prompt
        # Inject smart home device list for Alfred (non-blocking: if Tuya fails, skip)
        try:
            smart_ctx = await tuya_service.get_devices_for_prompt()
            if smart_ctx:
                system_prompt = system_prompt + "\n\n" + smart_ctx
        except Exception:
            pass
    # Inject long-term memory summary as first user/assistant context exchange
    if body.summary_context:
        prefix = ChatMessageIn(
            role=Role.user,
            content=f"[Contesto sessione precedente]: {body.summary_context}"
        )
        messages = [prefix] + list(body.messages)
    else:
        messages = list(body.messages)
    # Resolve provider override
    provider: ProviderID | None = None
    if body.provider_override:
        try:
            provider = ProviderID(body.provider_override)
        except ValueError:
            pass
    return companion, system_prompt, messages, provider


# ── Non-streaming ─────────────────────────────────────────────────────────────

@router.post("", response_model=ChatResponse)
async def send_message(body: ChatRequest, current_user: UserInDB = Depends(get_current_user)):
    companion, system_prompt, messages, provider = await _resolve_companion_and_prompt(body, current_user)
    reply, provider_used, fallback_used = await ai_service.chat(
        system_prompt=system_prompt,
        messages=messages,
        provider=provider,
        with_fallback=True,
    )
    await _execute_tuya_if_present(reply)
    return ChatResponse(
        reply=_strip_tuya(reply),
        companion_id=companion.id,
        provider=provider_used.value,
        fallback_used=fallback_used,
    )


# ── Streaming (SSE) ───────────────────────────────────────────────────────────

@router.post("/stream")
async def stream_message(body: ChatRequest, current_user: UserInDB = Depends(get_current_user)):
    """
    Server-Sent Events endpoint.
    Each event is a JSON object:
      {"c": "<chunk>"}            — text chunk
      {"provider": "<id>"}        — which provider is responding
      {"fallback": "<id>"}        — fallback provider was activated
      {"done": true}              — stream complete
    """
    companion, system_prompt, messages, provider = await _resolve_companion_and_prompt(body, current_user)

    async def event_generator():
        full_text: list[str] = []
        try:
            async for token in ai_service.chat_stream(
                system_prompt=system_prompt,
                messages=messages,
                provider=provider,
                with_fallback=True,
            ):
                if token.startswith("\x00PROVIDER:"):
                    pid = token[len("\x00PROVIDER:"):]
                    yield f"data: {json.dumps({'provider': pid})}\n\n"
                elif token.startswith("\x00FALLBACK:"):
                    pid = token[len("\x00FALLBACK:"):]
                    yield f"data: {json.dumps({'fallback': pid})}\n\n"
                else:
                    # Strip [TUYA:...] markers from the visible stream
                    clean = _TUYA_PATTERN.sub("", token)
                    full_text.append(token)  # keep raw for command detection
                    if clean:
                        yield f"data: {json.dumps({'c': clean})}\n\n"
            # After stream complete: execute any embedded Tuya command
            await _execute_tuya_if_present("".join(full_text))
            yield f"data: {json.dumps({'done': True})}\n\n"
        except HTTPException as e:
            yield f"data: {json.dumps({'error': e.detail})}\n\n"
        except Exception as e:
            yield f"data: {json.dumps({'error': str(e)})}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


# ── Summarize ─────────────────────────────────────────────────────────────────

@router.post("/summarize", response_model=SummarizeResponse)
async def summarize_conversation(body: SummarizeRequest, current_user: UserInDB = Depends(get_current_user)):
    """
    Given a list of messages, produce a concise summary for long-term memory.
    Uses the active provider (with fallback).
    """
    if not body.messages:
        raise HTTPException(422, "messages non può essere vuoto")

    # Build a single-turn summarization request
    conversation_text = "\n".join(
        f"{'Utente' if m.role == Role.user else 'Assistente'}: {m.content}"
        for m in body.messages
    )
    summary_messages = [
        ChatMessageIn(role=Role.user, content=f"Conversazione:\n\n{conversation_text}\n\nCreane un riassunto conciso.")
    ]

    summary, _, _ = await ai_service.chat(
        system_prompt=_SUMMARY_SYSTEM,
        messages=summary_messages,
        with_fallback=True,
    )
    return SummarizeResponse(summary=summary)
