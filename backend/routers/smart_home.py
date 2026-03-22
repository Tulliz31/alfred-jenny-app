from fastapi import APIRouter, Depends, HTTPException

from models.smart_home import TuyaDevice, DeviceStatus, DeviceCommand, CommandResult
from models.user import UserInDB, Role
from services.auth_service import get_current_user
from smart_home import tuya_service

router = APIRouter(prefix="/devices", tags=["smart-home"])


def _require_admin(current_user: UserInDB = Depends(get_current_user)) -> UserInDB:
    if current_user.role != Role.admin:
        raise HTTPException(403, "Richiede ruolo admin")
    return current_user


# ── Device list ───────────────────────────────────────────────────────────────


@router.get("", response_model=list[TuyaDevice])
async def list_devices(current_user: UserInDB = Depends(get_current_user)):
    """List all Tuya devices. Any authenticated user can read the list."""
    try:
        return await tuya_service.get_devices()
    except RuntimeError as e:
        raise HTTPException(503, str(e))


# ── Device status ─────────────────────────────────────────────────────────────


@router.get("/{device_id}/status", response_model=DeviceStatus)
async def device_status(
    device_id: str,
    current_user: UserInDB = Depends(get_current_user),
):
    """Get current status of a single device."""
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
    """Send a command to a device (on / off / brightness / temperature)."""
    result = await tuya_service.send_command(device_id, body.action, body.value)
    if not result.success:
        raise HTTPException(502, result.message or "Comando fallito")
    return result


# ── Admin: rediscover devices ─────────────────────────────────────────────────


@router.post("/discover", response_model=list[TuyaDevice])
async def discover_devices(current_user: UserInDB = Depends(_require_admin)):
    """Force-refresh the device cache and return the updated list. Admin only."""
    from smart_home import tuya_service as ts
    ts._device_cache.clear()
    ts._device_cache_expiry = 0
    try:
        return await tuya_service.get_devices()
    except RuntimeError as e:
        raise HTTPException(503, str(e))
