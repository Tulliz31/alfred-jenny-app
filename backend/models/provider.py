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


class CompanionAIConfig(BaseModel):
    """Per-companion AI provider configuration stored in-memory on the backend."""
    companion_id: str = ""
    provider_type: str = "openai"   # "openai" | "anthropic" | "gemini" | "openrouter" | "custom"
    api_key: str = ""
    model_id: str = ""
    base_url: str | None = None
    enabled: bool = False           # False = use global fallback chain
    use_global: bool = True         # jenny: True = inherit Alfred's config
