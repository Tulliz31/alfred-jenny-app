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

router = APIRouter(prefix="/chat", tags=["chat"])

# ── CMD tag pattern: [CMD:device_id:action] or [CMD:device_id:action:value] ────

_CMD_PATTERN = re.compile(r"\[CMD:([^:\]\s]+):([^:\]\s]+)(?::([^\]\s]+))?\]")


def _strip_cmd(text: str) -> str:
    """Remove [CMD:...] markers from the visible reply."""
    return _CMD_PATTERN.sub("", text).strip()


async def _execute_commands(
    text: str,
) -> list[tuple[str, str, bool, str]]:
    """
    Parse and execute all [CMD:id:action:value?] tags in text.
    Returns list of (display_name, action, success, message).
    """
    try:
        from services.smart_home.smart_home_manager import get_manager
        manager = get_manager()
    except Exception:
        return []

    results: list[tuple[str, str, bool, str]] = []
    for m in _CMD_PATTERN.finditer(text):
        device_id = m.group(1).strip()
        action = m.group(2).strip()
        value = m.group(3).strip() if m.group(3) else None
        if not device_id or not action:
            continue
        try:
            result = await manager.send_command(device_id, action, value)
            display_name = manager.get_device_display_name(device_id)
            results.append((display_name, action, result.success, result.message))
        except Exception as e:
            results.append((device_id, action, False, str(e)))
    return results


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

    # Inject smart home device list for ALL companions (Alfred and Jenny)
    try:
        from services.smart_home.smart_home_manager import get_manager
        smart_ctx = await get_manager().get_devices_for_prompt()
        if smart_ctx:
            system_prompt = system_prompt + "\n\n" + smart_ctx
    except Exception:
        pass

    # Inject long-term memory summary
    if body.summary_context:
        prefix = ChatMessageIn(
            role=Role.user,
            content=f"[Contesto sessione precedente]: {body.summary_context}"
        )
        messages = [prefix] + list(body.messages)
    else:
        messages = list(body.messages)

    # Resolve provider override (used only when no companion config is active)
    provider: ProviderID | None = None
    if body.provider_override:
        try:
            provider = ProviderID(body.provider_override)
        except ValueError:
            pass

    # Resolve AI config for this companion
    from models.provider import CompanionAIConfig
    stored_config = ai_service.get_companion_config(companion.id)

    if companion.id == "jenny":
        # Request-level jenny_ai_config overrides stored config (backward compat)
        jc = body.jenny_ai_config
        if jc and jc.enabled and jc.api_key and jc.model_id:
            ai_config = CompanionAIConfig(
                companion_id="jenny",
                provider_type=jc.provider_type,
                api_key=jc.api_key,
                model_id=jc.model_id,
                base_url=jc.base_url or None,
                enabled=True,
                use_global=False,
            )
        elif stored_config.use_global:
            # Jenny inherits Alfred's backend config
            ai_config = ai_service.get_companion_config("alfred")
        else:
            ai_config = stored_config
    else:
        ai_config = stored_config

    return companion, system_prompt, messages, provider, ai_config


# ── Non-streaming ──────────────────────────────────────────────────────────────

@router.post("", response_model=ChatResponse)
async def send_message(body: ChatRequest, current_user: UserInDB = Depends(get_current_user)):
    companion, system_prompt, messages, provider, ai_config = await _resolve_companion_and_prompt(body, current_user)
    if ai_config and ai_config.enabled:
        reply, provider_label, fallback_used = await ai_service.chat_with_config(
            system_prompt=system_prompt, messages=messages, config=ai_config, with_fallback=True
        )
    else:
        raw_reply, provider_enum, fallback_used = await ai_service.chat(
            system_prompt=system_prompt, messages=messages, provider=provider, with_fallback=True
        )
        reply = raw_reply
        provider_label = provider_enum.value
    cmd_results = await _execute_commands(reply)
    return ChatResponse(
        reply=_strip_cmd(reply),
        companion_id=companion.id,
        provider=provider_label,
        fallback_used=fallback_used,
    )


# ── Streaming (SSE) ────────────────────────────────────────────────────────────

@router.post("/stream")
async def stream_message(body: ChatRequest, current_user: UserInDB = Depends(get_current_user)):
    """
    Server-Sent Events endpoint.
    Each event is a JSON object:
      {"c": "<chunk>"}             — text chunk
      {"provider": "<id>"}         — which provider is responding
      {"fallback": "<id>"}         — fallback provider was activated
      {"cmd_ok": "name:action"}    — smart home command succeeded
      {"cmd_err": "name:error"}    — smart home command failed
      {"done": true}               — stream complete
    """
    companion, system_prompt, messages, provider, ai_config = await _resolve_companion_and_prompt(body, current_user)

    async def event_generator():
        full_text: list[str] = []
        try:
            stream_fn = (
                ai_service.chat_stream_with_config(
                    system_prompt=system_prompt, messages=messages, config=ai_config, with_fallback=True
                )
                if ai_config and ai_config.enabled
                else ai_service.chat_stream(
                    system_prompt=system_prompt, messages=messages, provider=provider, with_fallback=True
                )
            )
            async for token in stream_fn:
                if token.startswith("\x00PROVIDER:"):
                    pid = token[len("\x00PROVIDER:"):]
                    yield f"data: {json.dumps({'provider': pid})}\n\n"
                elif token.startswith("\x00FALLBACK:"):
                    pid = token[len("\x00FALLBACK:"):]
                    yield f"data: {json.dumps({'fallback': pid})}\n\n"
                else:
                    # Strip [CMD:...] markers from the visible stream
                    clean = _CMD_PATTERN.sub("", token)
                    full_text.append(token)  # keep raw for command detection
                    if clean:
                        yield f"data: {json.dumps({'c': clean})}\n\n"

            # After stream completes: execute embedded commands and emit results
            full_raw = "".join(full_text)
            cmd_results = await _execute_commands(full_raw)
            for (name, action, ok, msg) in cmd_results:
                if ok:
                    yield f"data: {json.dumps({'cmd_ok': f'{name}:{action}'})}\n\n"
                else:
                    yield f"data: {json.dumps({'cmd_err': f'{name}:{msg or action}'})}\n\n"

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


# ── Summarize ──────────────────────────────────────────────────────────────────

@router.post("/summarize", response_model=SummarizeResponse)
async def summarize_conversation(body: SummarizeRequest, current_user: UserInDB = Depends(get_current_user)):
    if not body.messages:
        raise HTTPException(422, "messages non può essere vuoto")

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
