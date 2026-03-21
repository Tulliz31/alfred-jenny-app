from pydantic import BaseModel
from enum import Enum
from typing import Optional


class ProviderID(str, Enum):
    openai = "openai"
    anthropic = "anthropic"
    gemini = "gemini"


class ProviderInfo(BaseModel):
    id: ProviderID
    name: str
    description: str
    default_model: str
    active: bool = False


class SetProviderRequest(BaseModel):
    provider: ProviderID
