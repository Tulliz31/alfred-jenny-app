from pydantic import BaseModel
from typing import Any


class TuyaDevice(BaseModel):
    id: str
    name: str
    category: str          # e.g. "dj" (light), "wk" (thermostat), "cz" (socket)
    product_name: str = ""
    online: bool = False
    status: list[dict[str, Any]] = []


class DeviceStatus(BaseModel):
    device_id: str
    online: bool
    status: list[dict[str, Any]]   # [{"code": "switch_led", "value": True}, ...]


class DeviceCommand(BaseModel):
    action: str             # "on" | "off" | "brightness" | "temperature"
    value: int | None = None   # required for brightness (10–1000) and temperature


class CommandResult(BaseModel):
    success: bool
    device_id: str
    action: str
    message: str = ""
