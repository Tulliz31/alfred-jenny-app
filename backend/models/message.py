from pydantic import BaseModel
from enum import Enum


class Role(str, Enum):
    user = "user"
    assistant = "assistant"
    system = "system"


class ChatMessageIn(BaseModel):
    role: Role
    content: str


class JennyAIConfig(BaseModel):
    """Optional dedicated AI provider configuration for Jenny companion."""
    enabled: bool = False
    provider_type: str = "openrouter"   # "openrouter" | "custom"
    api_key: str = ""
    model_id: str = ""
    base_url: str = ""                  # for custom provider


class ChatRequest(BaseModel):
    companion_id: str = "alfred"
    messages: list[ChatMessageIn]
    personality_level: int = 3
    session_id: str = ""
    summary_context: str = ""          # Long-term memory summary injected by client
    provider_override: str = ""        # Force a specific provider for this request
    jenny_ai_config: JennyAIConfig | None = None  # Dedicated AI for Jenny


class ChatResponse(BaseModel):
    reply: str
    companion_id: str
    provider: str
    fallback_used: bool = False
    memo_action: dict | None = None
    calendar_action: dict | None = None
    reminder_action: dict | None = None
    calendar_read_request: dict | None = None


class SummarizeRequest(BaseModel):
    messages: list[ChatMessageIn]  # Full conversation to summarize


class SummarizeResponse(BaseModel):
    summary: str


class HistoryMessage(BaseModel):
    role: Role
    content: str
