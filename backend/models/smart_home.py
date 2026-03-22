from pydantic import BaseModel
from typing import Any


# ── Legacy models (kept for backward compatibility) ───────────────────────────

class TuyaDevice(BaseModel):
    id: str
    name: str
    category: str
    product_name: str = ""
    online: bool = False
    status: list[dict[str, Any]] = []


class DeviceStatus(BaseModel):
    device_id: str
    online: bool
    status: list[dict[str, Any]]


class DeviceCommand(BaseModel):
    action: str
    value: Any = None  # int for brightness/temperature, str for colour


class CommandResult(BaseModel):
    success: bool
    device_id: str
    action: str
    message: str = ""


# ── Enriched device models (v2 API) ───────────────────────────────────────────

class DeviceStatusInfo(BaseModel):
    is_on: bool = False
    brightness: int | None = None    # percentage 0-100
    colour: str | None = None        # hex string e.g. "#FF5500"
    temperature: float | None = None  # °C


class DeviceEnriched(BaseModel):
    id: str
    name: str
    name_custom: str = ""
    type: str = "switch"   # light | switch | thermostat | fan | camera | curtain | ac
    source: str = "tuya"
    online: bool = False
    status: DeviceStatusInfo = DeviceStatusInfo()
    capabilities: list[str] = []
    visible: bool = True


class DevicesResponse(BaseModel):
    devices: list[DeviceEnriched]
    last_updated: str


class RenameDeviceRequest(BaseModel):
    name: str


# ── Admin / config ────────────────────────────────────────────────────────────

class TuyaConfigRequest(BaseModel):
    client_id: str
    client_secret: str
    user_uid: str = ""
    base_url: str = "https://openapi.tuyaeu.com"


# ── Connector status ──────────────────────────────────────────────────────────

class ConnectorInfo(BaseModel):
    id: str
    name: str
    connected: bool
    device_count: int = 0
    status: str = ""


class ConnectorsResponse(BaseModel):
    connectors: list[ConnectorInfo]
