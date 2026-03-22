"""
SmartHomeManager — unified smart home interface.

Wraps TuyaConnector, manages a 60-second device cache, and persists device
metadata (custom names, visibility) to device_meta.json on disk.
"""
from __future__ import annotations

import json
import pathlib
import time
from datetime import datetime, timezone
from typing import Any

from config import settings
from models.smart_home import (
    CommandResult,
    DeviceEnriched,
    DeviceStatusInfo,
)
from services.smart_home.tuya_connector import (
    CAPABILITY_MAP,
    CATEGORY_TYPE_MAP,
    TuyaConnector,
)

_META_FILE = pathlib.Path("device_meta.json")


# ── Metadata persistence ───────────────────────────────────────────────────────


def _load_meta() -> dict[str, dict]:
    if _META_FILE.exists():
        try:
            return json.loads(_META_FILE.read_text(encoding="utf-8"))
        except Exception:
            pass
    return {}


def _save_meta(meta: dict[str, dict]) -> None:
    _META_FILE.write_text(
        json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8"
    )


# ── Manager ───────────────────────────────────────────────────────────────────


class SmartHomeManager:
    """
    Singleton that wraps connectors (currently Tuya only).
    Alexa / Google Home are stubs for future integration.
    """

    def __init__(self) -> None:
        self._connector: TuyaConnector | None = None
        self._device_cache: list[DeviceEnriched] = []
        self._cache_expiry: float = 0.0
        self._last_updated: str = ""
        self._meta: dict[str, dict] = _load_meta()

    # ── Connector factory ──────────────────────────────────────────────────────

    def _get_connector(self) -> TuyaConnector:
        """Return (and recreate if config changed) the Tuya connector."""
        if not settings.TUYA_CLIENT_ID or not settings.TUYA_CLIENT_SECRET:
            raise RuntimeError(
                "Tuya non configurato (TUYA_CLIENT_ID / TUYA_CLIENT_SECRET mancanti)"
            )
        if (
            self._connector is None
            or self._connector.client_id != settings.TUYA_CLIENT_ID
            or self._connector.client_secret != settings.TUYA_CLIENT_SECRET
            or self._connector.base_url.rstrip("/") != settings.TUYA_BASE_URL.rstrip("/")
        ):
            self._connector = TuyaConnector(
                client_id=settings.TUYA_CLIENT_ID,
                client_secret=settings.TUYA_CLIENT_SECRET,
                base_url=settings.TUYA_BASE_URL,
            )
        return self._connector

    def invalidate_cache(self) -> None:
        self._cache_expiry = 0.0
        self._device_cache = []

    # ── Device list ────────────────────────────────────────────────────────────

    async def get_all_devices(self, force_refresh: bool = False) -> list[DeviceEnriched]:
        """Return enriched device list with 60-second cache."""
        if not force_refresh and time.time() < self._cache_expiry and self._device_cache:
            return self._device_cache

        connector = self._get_connector()
        user_uid = getattr(settings, "TUYA_USER_UID", "")
        raw_list = await connector.get_all_devices(user_uid)
        devices = [self._enrich(d) for d in raw_list]
        self._device_cache = devices
        self._cache_expiry = time.time() + 60
        self._last_updated = datetime.now(timezone.utc).isoformat()
        return devices

    def get_last_updated(self) -> str:
        return self._last_updated

    def _enrich(self, raw: dict) -> DeviceEnriched:
        """Convert a raw connector dict to a DeviceEnriched model."""
        device_id = raw.get("id", "")
        meta = self._meta.get(device_id, {})
        status_list: list[dict] = raw.get("status", [])
        status_map = {s.get("code", ""): s.get("value") for s in status_list}

        is_on = bool(
            status_map.get("switch_led")
            or status_map.get("switch")
            or status_map.get("switch_1")
        )
        brightness_raw = (
            status_map.get("bright_value_v2") or status_map.get("bright_value")
        )
        brightness = int(brightness_raw / 10) if brightness_raw is not None else None
        temp_raw = status_map.get("temp_current")
        temperature = float(temp_raw) / 10.0 if temp_raw is not None else None

        dev_type = raw.get("type") or CATEGORY_TYPE_MAP.get(raw.get("category", ""), "switch")
        caps = raw.get("capabilities") or CAPABILITY_MAP.get(dev_type, ["on_off"])

        return DeviceEnriched(
            id=device_id,
            name=raw.get("name", device_id),
            name_custom=meta.get("name_custom", ""),
            type=dev_type,
            source="tuya",
            online=raw.get("online", False),
            status=DeviceStatusInfo(
                is_on=is_on,
                brightness=brightness,
                temperature=temperature,
            ),
            capabilities=caps,
            visible=meta.get("visible", True),
        )

    # ── Commands ───────────────────────────────────────────────────────────────

    async def send_command(
        self, device_id: str, action: str, value: Any = None
    ) -> CommandResult:
        """Execute a high-level command on a device."""
        try:
            connector = self._get_connector()
            candidates = TuyaConnector.map_command(action, value)
            if not candidates:
                return CommandResult(
                    success=False,
                    device_id=device_id,
                    action=action,
                    message=f"Azione '{action}' non supportata",
                )
            last_err = "Comando rifiutato"
            for cmd in candidates:
                try:
                    await connector._request(
                        "POST",
                        f"/v1.0/devices/{device_id}/commands",
                        {"commands": [cmd]},
                    )
                    self.invalidate_cache()
                    return CommandResult(success=True, device_id=device_id, action=action)
                except RuntimeError as e:
                    last_err = str(e)
            return CommandResult(
                success=False, device_id=device_id, action=action, message=last_err
            )
        except RuntimeError as e:
            return CommandResult(
                success=False, device_id=device_id, action=action, message=str(e)
            )

    # ── Device metadata ────────────────────────────────────────────────────────

    def set_device_name(self, device_id: str, name: str) -> None:
        self._meta.setdefault(device_id, {})["name_custom"] = name
        _save_meta(self._meta)
        for d in self._device_cache:
            if d.id == device_id:
                d.name_custom = name
                break

    def set_device_visibility(self, device_id: str, visible: bool) -> None:
        self._meta.setdefault(device_id, {})["visible"] = visible
        _save_meta(self._meta)

    def get_device_display_name(self, device_id: str) -> str:
        meta = self._meta.get(device_id, {})
        return meta.get("name_custom", "") or device_id

    def get_all_meta(self) -> dict[str, dict]:
        return dict(self._meta)

    # ── AI prompt context ──────────────────────────────────────────────────────

    async def get_devices_for_prompt(self) -> str:
        """
        Return a formatted string for injection into the AI system prompt.
        Uses [CMD:device_id:action:value?] tag format.
        Returns empty string if Tuya is not configured or unavailable.
        """
        if not settings.TUYA_CLIENT_ID or not settings.TUYA_CLIENT_SECRET:
            return ""
        try:
            devices = await self.get_all_devices()
            visible = [d for d in devices if d.visible]
            if not visible:
                return ""
            lines = ["DISPOSITIVI SMART HOME DISPONIBILI:"]
            for d in visible:
                display_name = d.name_custom or d.name
                state_parts: list[str] = []
                if not d.online:
                    state_parts.append("offline")
                else:
                    state_parts.append("acceso" if d.status.is_on else "spento")
                    if d.status.brightness is not None:
                        state_parts.append(f"luminosità {d.status.brightness}%")
                    if d.status.temperature is not None:
                        state_parts.append(f"temp {d.status.temperature:.1f}°C")
                caps = ", ".join(d.capabilities) if d.capabilities else "on_off"
                lines.append(
                    f"  - {display_name} (ID: {d.id}, tipo: {d.type}, "
                    f"stato: {', '.join(state_parts)}, capacità: {caps})"
                )
            lines.append(
                "\nQuando l'utente chiede di controllare un dispositivo, "
                "includi nella risposta uno o più tag di comando (invisibili all'utente):\n"
                "  [CMD:device_id:turn_on]\n"
                "  [CMD:device_id:turn_off]\n"
                "  [CMD:device_id:brightness:75]   ← valore 0-100\n"
                "  [CMD:device_id:colour:#FF5500]\n"
                "  [CMD:device_id:temperature:22]\n"
                "Dopo il tag aggiungi una conferma naturale in italiano. "
                "Se il dispositivo è offline, comunicalo gentilmente all'utente."
            )
            return "\n".join(lines)
        except Exception:
            return ""


# ── Global singleton ───────────────────────────────────────────────────────────

_manager: SmartHomeManager | None = None


def get_manager() -> SmartHomeManager:
    global _manager
    if _manager is None:
        _manager = SmartHomeManager()
    return _manager
