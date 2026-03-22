"""
Class-based Tuya Cloud API v2.0 connector.

Each TuyaConnector instance manages its own token lifecycle.
Credentials are passed at construction time, allowing runtime reconfiguration.
"""
from __future__ import annotations

import asyncio
import hashlib
import hmac
import json
import time
from typing import Any

import httpx

# ── Category → readable type ──────────────────────────────────────────────────

CATEGORY_TYPE_MAP: dict[str, str] = {
    "dj": "light", "dd": "light", "fwd": "light", "dc": "light", "xdd": "light",
    "cz": "switch", "kg": "switch", "pc": "switch",
    "wk": "thermostat", "wsdcg": "thermostat", "moes": "thermostat",
    "fs": "fan",
    "sp": "camera",
    "cl": "curtain", "msp": "curtain",
    "kfj": "appliance",
    "kt": "ac",
}

CAPABILITY_MAP: dict[str, list[str]] = {
    "light":     ["on_off", "brightness", "colour"],
    "switch":    ["on_off"],
    "thermostat": ["on_off", "temperature"],
    "fan":       ["on_off"],
    "camera":    [],
    "curtain":   ["on_off"],
    "ac":        ["on_off", "temperature"],
    "appliance": ["on_off"],
}


class TuyaConnector:
    """Tuya Cloud API connector (v1.0 / v2.0 / v1.3 endpoints)."""

    def __init__(self, client_id: str, client_secret: str, base_url: str):
        self.client_id = client_id
        self.client_secret = client_secret
        self.base_url = base_url.rstrip("/")
        self._access_token: str = ""
        self._token_expiry: float = 0.0
        self._token_lock = asyncio.Lock()

    # ── Signing ────────────────────────────────────────────────────────────────

    def _sign(
        self,
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
            message = self.client_id + access_token + t + str_to_sign
        else:
            message = self.client_id + t + str_to_sign
        signature = hmac.new(
            self.client_secret.encode(),
            message.encode(),
            hashlib.sha256,
        ).hexdigest().upper()
        return t, signature

    # ── Auth ───────────────────────────────────────────────────────────────────

    async def get_token(self) -> str:
        """Ensure access token is fresh and return it."""
        async with self._token_lock:
            if time.time() < self._token_expiry:
                return self._access_token
            path = "/v1.0/token"
            query = "?grant_type=1"
            t, sign = self._sign("GET", path + query, "")
            headers = {
                "client_id": self.client_id,
                "sign": sign,
                "t": t,
                "sign_method": "HMAC-SHA256",
            }
            async with httpx.AsyncClient(timeout=10) as client:
                resp = await client.get(
                    f"{self.base_url}{path}",
                    params={"grant_type": 1},
                    headers=headers,
                )
            data = resp.json()
            if not data.get("success"):
                raise RuntimeError(f"Tuya auth failed: {data.get('msg', data)}")
            result = data["result"]
            self._access_token = result["access_token"]
            self._token_expiry = time.time() + result.get("expire_time", 7200) - 60
            return self._access_token

    # ── Low-level request ──────────────────────────────────────────────────────

    async def _request(self, method: str, path: str, body: Any = None) -> dict:
        """Execute a signed Tuya API request."""
        token = await self.get_token()
        body_str = json.dumps(body) if body else ""
        t, sign = self._sign(method.upper(), path, body_str, token)
        headers = {
            "client_id": self.client_id,
            "access_token": token,
            "sign": sign,
            "t": t,
            "sign_method": "HMAC-SHA256",
            "Content-Type": "application/json",
        }
        url = f"{self.base_url}{path}"
        async with httpx.AsyncClient(timeout=10) as client:
            if method.upper() == "GET":
                resp = await client.get(url, headers=headers)
            elif method.upper() == "PUT":
                resp = await client.put(url, content=body_str, headers=headers)
            else:
                resp = await client.post(url, content=body_str, headers=headers)
        data = resp.json()
        if not data.get("success"):
            raise RuntimeError(
                f"Tuya API error on {method} {path}: {data.get('msg', data)}"
            )
        return data

    # ── Public API ─────────────────────────────────────────────────────────────

    async def get_devices(self, user_uid: str = "") -> list[dict]:
        """Return raw device list from Tuya API."""
        if user_uid:
            data = await self._request(
                "GET", f"/v2.0/cloud/thing/user?uid={user_uid}"
            )
            result = data.get("result", {})
            raw: list[dict] = (
                result.get("devices", []) if isinstance(result, dict) else result
            )
        else:
            data = await self._request("GET", "/v1.3/iot-03/devices?page_size=100")
            raw = data.get("result", {}).get("devices", [])
        return raw

    async def get_device_status(self, device_id: str) -> dict:
        """Return current status of a single device as {device_id, status}."""
        data = await self._request("GET", f"/v1.0/devices/{device_id}/status")
        return {"device_id": device_id, "status": data.get("result", [])}

    async def send_command(self, device_id: str, commands: list[dict]) -> bool:
        """Send raw commands list to a device. Returns True on success."""
        await self._request(
            "POST",
            f"/v1.0/devices/{device_id}/commands",
            {"commands": commands},
        )
        return True

    async def get_all_devices(self, user_uid: str = "") -> list[dict]:
        """Return devices enriched with type/capabilities info."""
        raw_devices = await self.get_devices(user_uid)
        result = []
        for d in raw_devices:
            cat = d.get("category", "")
            dev_type = CATEGORY_TYPE_MAP.get(cat, "switch")
            result.append({
                "id": d.get("id", ""),
                "name": d.get("name", d.get("id", "?")),
                "category": cat,
                "type": dev_type,
                "product_name": d.get("product_name", ""),
                "online": d.get("online", False),
                "status": d.get("status", []),
                "capabilities": CAPABILITY_MAP.get(dev_type, ["on_off"]),
            })
        return result

    # ── Command mapping ────────────────────────────────────────────────────────

    @staticmethod
    def map_command(action: str, value: Any = None) -> list[dict]:
        """Map a high-level action to one or more Tuya DPS command candidates."""
        if action in ("turn_on", "on"):
            return [
                {"code": "switch_led", "value": True},
                {"code": "switch", "value": True},
                {"code": "switch_1", "value": True},
            ]
        if action in ("turn_off", "off"):
            return [
                {"code": "switch_led", "value": False},
                {"code": "switch", "value": False},
                {"code": "switch_1", "value": False},
            ]
        if action == "brightness" and value is not None:
            # value is 0-100; Tuya scale is 10-1000
            level = max(10, min(1000, int(float(value) * 10)))
            return [
                {"code": "bright_value_v2", "value": level},
                {"code": "bright_value", "value": level},
            ]
        if action == "colour" and value is not None:
            hex_val = str(value).lstrip("#")
            try:
                r = int(hex_val[0:2], 16)
                g = int(hex_val[2:4], 16)
                b = int(hex_val[4:6], 16)
                h, s, v = _rgb_to_hsv(r, g, b)
                colour_data = json.dumps({"h": h, "s": s, "v": v})
                return [{"code": "colour_data", "value": colour_data}]
            except Exception:
                return []
        if action == "temperature" and value is not None:
            return [{"code": "temp_set", "value": int(float(value))}]
        return []


# ── Helpers ────────────────────────────────────────────────────────────────────

def _rgb_to_hsv(r: int, g: int, b: int) -> tuple[int, int, int]:
    """Convert RGB (0-255) to Tuya HSV (H:0-360, S:0-1000, V:0-1000)."""
    r_, g_, b_ = r / 255.0, g / 255.0, b / 255.0
    cmax = max(r_, g_, b_)
    cmin = min(r_, g_, b_)
    delta = cmax - cmin
    if delta == 0:
        h = 0.0
    elif cmax == r_:
        h = 60.0 * (((g_ - b_) / delta) % 6)
    elif cmax == g_:
        h = 60.0 * ((b_ - r_) / delta + 2)
    else:
        h = 60.0 * ((r_ - g_) / delta + 4)
    s = 0.0 if cmax == 0 else delta / cmax
    return int(h), int(s * 1000), int(cmax * 1000)
