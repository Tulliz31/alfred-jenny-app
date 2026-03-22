from pydantic import BaseModel
from enum import Enum


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
    price_per_1k_input: float = 0.0    # USD per 1K input tokens (indicative)
    price_per_1k_output: float = 0.0   # USD per 1K output tokens (indicative)
    avg_latency_ms: int = 1500         # Average first-token latency in ms


class SetProviderRequest(BaseModel):
    provider: ProviderID
