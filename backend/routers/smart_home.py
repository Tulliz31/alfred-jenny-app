from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException

from models.smart_home import (
    CommandResult,
    ConnectorInfo,
    ConnectorsResponse,
    DeviceCommand,
    DeviceEnriched,
    DeviceStatus,
    DevicesResponse,
    RenameDeviceRequest,
    TuyaConfigRequest,
    TuyaDevice,
)
from models.user import UserInDB, Role
from services.auth_service import get_current_user
from services.smart_home.smart_home_manager import get_manager

router = APIRouter(prefix="/devices", tags=["smart-home"])


def _require_admin(current_user: UserInDB = Depends(get_current_user)) -> UserInDB:
    if current_user.role != Role.admin:
        raise HTTPException(403, "Richiede ruolo admin")
    return current_user


# ── Connectors status (must be registered before /{device_id}) ────────────────


@router.get("/connectors", response_model=ConnectorsResponse)
async def get_connectors(current_user: UserInDB = Depends(get_current_user)):
    """Return connection status for all configured smart home integrations."""
    from config import settings

    tuya_connected = bool(settings.TUYA_CLIENT_ID and settings.TUYA_CLIENT_SECRET)
    tuya_count = 0
    tuya_status = "Non configurato"
    if tuya_connected:
        try:
            manager = get_manager()
            devices = await manager.get_all_devices()
            tuya_count = len(devices)
            tuya_status = f"Connesso — {tuya_count} dispositivi"
        except Exception as e:
            tuya_connected = False
            tuya_status = str(e)

    return ConnectorsResponse(connectors=[
        ConnectorInfo(
            id="tuya",
            name="Tuya Cloud",
            connected=tuya_connected,
            device_count=tuya_count,
            status=tuya_status,
        ),
        ConnectorInfo(
            id="alexa",
            name="Amazon Alexa",
            connected=False,
            status="Prossimamente",
        ),
        ConnectorInfo(
            id="google_home",
            name="Google Home",
            connected=False,
            status="Prossimamente",
        ),
    ])


# ── Device list ───────────────────────────────────────────────────────────────


@router.get("", response_model=DevicesResponse)
async def list_devices(current_user: UserInDB = Depends(get_current_user)):
    """List all devices enriched with type, capabilities, and custom names."""
    try:
        manager = get_manager()
        devices = await manager.get_all_devices()
        return DevicesResponse(
            devices=devices,
            last_updated=manager.get_last_updated() or datetime.now(timezone.utc).isoformat(),
        )
    except RuntimeError as e:
        raise HTTPException(503, str(e))


# ── Device status ─────────────────────────────────────────────────────────────


@router.get("/{device_id}/status", response_model=DeviceStatus)
async def device_status(
    device_id: str,
    current_user: UserInDB = Depends(get_current_user),
):
    """Get current status of a single device via direct Tuya API call."""
    from smart_home import tuya_service
    try:
        return await tuya_service.get_status(device_id)
    except RuntimeError as e:
        raise HTTPException(503, str(e))


# ── Send command ──────────────────────────────────────────────────────────────


@router.post("/{device_id}/command", response_model=CommandResult)
async def send_command(
    device_id: str,
    body: DeviceCommand,
    current_user: UserInDB = Depends(get_current_user),
):
    """Send a command to a device (turn_on / turn_off / brightness / colour / temperature)."""
    manager = get_manager()
    result = await manager.send_command(device_id, body.action, body.value)
    if not result.success:
        raise HTTPException(502, result.message or "Comando fallito")
    return result


# ── Rename device ─────────────────────────────────────────────────────────────


@router.put("/{device_id}/name")
async def rename_device(
    device_id: str,
    body: RenameDeviceRequest,
    current_user: UserInDB = Depends(get_current_user),
):
    """Set a custom display name for a device. Persisted to device_meta.json."""
    if not body.name.strip():
        raise HTTPException(422, "Il nome non può essere vuoto")
    get_manager().set_device_name(device_id, body.name.strip())
    return {"ok": True, "device_id": device_id, "name": body.name.strip()}


# ── Device visibility (admin) ─────────────────────────────────────────────────


@router.put("/{device_id}/visible")
async def set_device_visibility(
    device_id: str,
    visible: bool,
    current_user: UserInDB = Depends(_require_admin),
):
    """Show or hide a device from Alfred/Jenny's device list."""
    get_manager().set_device_visibility(device_id, visible)
    return {"ok": True, "device_id": device_id, "visible": visible}


# ── Sync / force refresh ─────────────────────────────────────────────────────


@router.post("/sync", response_model=DevicesResponse)
async def sync_devices(current_user: UserInDB = Depends(get_current_user)):
    """Force-refresh the device cache from Tuya Cloud and return updated list."""
    try:
        manager = get_manager()
        manager.invalidate_cache()
        devices = await manager.get_all_devices(force_refresh=True)
        return DevicesResponse(
            devices=devices,
            last_updated=manager.get_last_updated(),
        )
    except RuntimeError as e:
        raise HTTPException(503, str(e))


# ── Admin: legacy discover alias ──────────────────────────────────────────────


@router.post("/discover", response_model=list[TuyaDevice])
async def discover_devices(current_user: UserInDB = Depends(_require_admin)):
    """Force-refresh and return legacy TuyaDevice list. Admin only."""
    try:
        manager = get_manager()
        manager.invalidate_cache()
        enriched = await manager.get_all_devices(force_refresh=True)
        return [
            TuyaDevice(
                id=d.id,
                name=d.name_custom or d.name,
                category=d.type,
                online=d.online,
            )
            for d in enriched
        ]
    except RuntimeError as e:
        raise HTTPException(503, str(e))


# ── Admin: Tuya runtime config ────────────────────────────────────────────────


@router.put("/admin/tuya-config")
async def update_tuya_config(
    body: TuyaConfigRequest,
    current_user: UserInDB = Depends(_require_admin),
):
    """
    Update Tuya credentials at runtime (no .env write — use .env for persistence).
    Invalidates the device cache so the next request re-authenticates.
    """
    from config import settings

    settings.TUYA_CLIENT_ID = body.client_id.strip()
    settings.TUYA_CLIENT_SECRET = body.client_secret.strip()
    settings.TUYA_BASE_URL = body.base_url.strip() or "https://openapi.tuyaeu.com"
    settings.TUYA_USER_UID = body.user_uid.strip()

    manager = get_manager()
    manager._connector = None  # force reconnect with new credentials
    manager.invalidate_cache()

    return {
        "ok": True,
        "client_id": settings.TUYA_CLIENT_ID,
        "base_url": settings.TUYA_BASE_URL,
    }
