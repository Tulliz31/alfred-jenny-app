"""
Tuya Cloud API client.

Uses the official Tuya Open API v1.x with HMAC-SHA256 signed requests.
Credentials are read from environment via config.py:
  TUYA_CLIENT_ID     – Tuya app client ID
  TUYA_CLIENT_SECRET – Tuya app client secret
  TUYA_BASE_URL      – e.g. https://openapi.tuyaeu.com  (EU region)

Auth flow:
  1. GET /v1.0/token?grant_type=1  → access_token (expires ~7200 s)
  2. All subsequent calls include the token in headers and signature.
"""

from __future__ import annotations

import asyncio
import hashlib
import hmac
import json
import time
from typing import Any

import httpx

from config import settings
from models.smart_home import TuyaDevice, DeviceStatus, CommandResult

# ── Internal token cache ───────────────────────────────────────────────────────

_access_token: str = ""
_token_expiry: float = 0.0
_token_lock = asyncio.Lock()

# Cache device list for 60 s to avoid hammering the API on every Alfred request
_device_cache: list[TuyaDevice] = []
_device_cache_expiry: float = 0.0

# ── Signing ────────────────────────────────────────────────────────────────────


def _sign(
    method: str,
    path_with_query: str,
    body_str: str,
    access_token: str = "",
) -> tuple[str, str]:
    """Return (timestamp_ms_str, UPPER_HEX_HMAC_SHA256_signature)."""
    t = str(int(time.time() * 1000))
    body_sha256 = hashlib.sha256(body_str.encode()).hexdigest()
    str_to_sign = f"{method}\n{body_sha256}\n\n{path_with_query}"
    if access_token:
        message = settings.TUYA_CLIENT_ID + access_token + t + str_to_sign
    else:
        message = settings.TUYA_CLIENT_ID + t + str_to_sign
    signature = hmac.new(
        settings.TUYA_CLIENT_SECRET.encode(),
        message.encode(),
        hashlib.sha256,
    ).hexdigest().upper()
    return t, signature


# ── Auth ───────────────────────────────────────────────────────────────────────


async def _ensure_token() -> str:
    global _access_token, _token_expiry
    async with _token_lock:
        if time.time() < _token_expiry:
            return _access_token

        path = "/v1.0/token"
        query = "?grant_type=1"
        t, sign = _sign("GET", path + query, "")
        headers = {
            "client_id": settings.TUYA_CLIENT_ID,
            "sign": sign,
            "t": t,
            "sign_method": "HMAC-SHA256",
        }
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(
                f"{settings.TUYA_BASE_URL}{path}",
                params={"grant_type": 1},
                headers=headers,
            )
        data = resp.json()
        if not data.get("success"):
            raise RuntimeError(f"Tuya auth failed: {data.get('msg', data)}")
        result = data["result"]
        _access_token = result["access_token"]
        _token_expiry = time.time() + result.get("expire_time", 7200) - 60
        return _access_token


# ── Low-level request ──────────────────────────────────────────────────────────


async def _request(method: str, path: str, body: Any = None) -> dict:
    """Execute a signed Tuya API request. Returns the parsed JSON dict."""
    if not settings.TUYA_CLIENT_ID or not settings.TUYA_CLIENT_SECRET:
        raise RuntimeError("Tuya non configurato (TUYA_CLIENT_ID / TUYA_CLIENT_SECRET mancanti)")

    token = await _ensure_token()
    body_str = json.dumps(body) if body else ""
    t, sign = _sign(method.upper(), path, body_str, token)
    headers = {
        "client_id": settings.TUYA_CLIENT_ID,
        "access_token": token,
        "sign": sign,
        "t": t,
        "sign_method": "HMAC-SHA256",
        "Content-Type": "application/json",
    }
    url = f"{settings.TUYA_BASE_URL}{path}"
    async with httpx.AsyncClient(timeout=10) as client:
        if method.upper() == "GET":
            resp = await client.get(url, headers=headers)
        else:
            resp = await client.post(url, content=body_str, headers=headers)

    data = resp.json()
    if not data.get("success"):
        raise RuntimeError(f"Tuya API error on {method} {path}: {data.get('msg', data)}")
    return data


# ── Public API ─────────────────────────────────────────────────────────────────


def _is_configured() -> bool:
    return bool(settings.TUYA_CLIENT_ID and settings.TUYA_CLIENT_SECRET)


async def get_devices() -> list[TuyaDevice]:
    """Return all devices registered to the Tuya app."""
    global _device_cache, _device_cache_expiry
    if time.time() < _device_cache_expiry and _device_cache:
        return _device_cache

    data = await _request("GET", "/v1.3/iot-03/devices?page_size=100")
    raw_devices: list[dict] = data.get("result", {}).get("devices", [])
    devices = [
        TuyaDevice(
            id=d.get("id", ""),
            name=d.get("name", d.get("id", "?")),
            category=d.get("category", ""),
            product_name=d.get("product_name", ""),
            online=d.get("online", False),
            status=d.get("status", []),
        )
        for d in raw_devices
    ]
    _device_cache = devices
    _device_cache_expiry = time.time() + 60
    return devices


async def get_status(device_id: str) -> DeviceStatus:
    """Return current status of a single device."""
    data = await _request("GET", f"/v1.0/devices/{device_id}/status")
    status_list: list[dict] = data.get("result", [])
    # Check online from device info
    info_data = await _request("GET", f"/v1.0/devices/{device_id}")
    online = info_data.get("result", {}).get("online", False)
    return DeviceStatus(device_id=device_id, online=online, status=status_list)


async def turn_on(device_id: str) -> bool:
    """Send switch ON command. Tries common switch codes."""
    for code in ("switch_led", "switch"):
        try:
            await _request(
                "POST",
                f"/v1.0/devices/{device_id}/commands",
                {"commands": [{"code": code, "value": True}]},
            )
            return True
        except RuntimeError:
            continue
    return False


async def turn_off(device_id: str) -> bool:
    """Send switch OFF command."""
    for code in ("switch_led", "switch"):
        try:
            await _request(
                "POST",
                f"/v1.0/devices/{device_id}/commands",
                {"commands": [{"code": code, "value": False}]},
            )
            return True
        except RuntimeError:
            continue
    return False


async def set_brightness(device_id: str, level: int) -> bool:
    """Set brightness (10–1000). level is clamped automatically."""
    value = max(10, min(1000, level))
    try:
        await _request(
            "POST",
            f"/v1.0/devices/{device_id}/commands",
            {"commands": [{"code": "bright_value_v2", "value": value}]},
        )
        return True
    except RuntimeError:
        # fallback to legacy code
        try:
            await _request(
                "POST",
                f"/v1.0/devices/{device_id}/commands",
                {"commands": [{"code": "bright_value", "value": value}]},
            )
            return True
        except RuntimeError:
            return False


async def send_command(device_id: str, action: str, value: int | None) -> CommandResult:
    """Dispatch a high-level action to a device."""
    try:
        if action == "on":
            ok = await turn_on(device_id)
        elif action == "off":
            ok = await turn_off(device_id)
        elif action == "brightness" and value is not None:
            ok = await set_brightness(device_id, value)
        else:
            return CommandResult(success=False, device_id=device_id, action=action,
                                 message=f"Azione '{action}' non supportata")
        if ok:
            return CommandResult(success=True, device_id=device_id, action=action)
        return CommandResult(success=False, device_id=device_id, action=action,
                             message="Comando rifiutato dal dispositivo")
    except RuntimeError as e:
        return CommandResult(success=False, device_id=device_id, action=action, message=str(e))


async def get_devices_for_prompt() -> str:
    """
    Returns a formatted string listing all devices for injection into Alfred's system prompt.
    Returns empty string if Tuya is not configured or unavailable.
    """
    if not _is_configured():
        return ""
    try:
        devices = await get_devices()
        if not devices:
            return ""
        lines = ["Dispositivi smart home disponibili:"]
        for d in devices:
            status_text = "online" if d.online else "offline"
            # Try to get on/off state from status list
            switch_status = next(
                (s["value"] for s in d.status if s.get("code") in ("switch_led", "switch")),
                None,
            )
            state = ""
            if switch_status is not None:
                state = ", acceso" if switch_status else ", spento"
            lines.append(f'  - {d.name} (ID: {d.id}, categoria: {d.category}, {status_text}{state})')
        lines.append(
            "\nQuando l'utente chiede di controllare un dispositivo, rispondi normalmente "
            "E aggiungi in fondo alla risposta (su riga separata) il comando:\n"
            "[TUYA:{\"device_id\":\"<id>\",\"action\":\"on\"|\"off\"|\"brightness\",\"value\":<number_or_null>}]\n"
            "Sostituisci <id> con l'ID esatto del dispositivo. Non spiegare il comando."
        )
        return "\n".join(lines)
    except Exception:
        return ""
