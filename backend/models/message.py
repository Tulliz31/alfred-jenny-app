from pydantic import BaseModel
from enum import Enum


class Role(str, Enum):
    user = "user"
    assistant = "assistant"
    system = "system"


class ChatMessageIn(BaseModel):
    role: Role
    content: str


class ChatRequest(BaseModel):
    companion_id: str = "alfred"
    messages: list[ChatMessageIn]
    personality_level: int = 3
    session_id: str = ""
    summary_context: str = ""      # Long-term memory summary injected by client
    provider_override: str = ""    # Force a specific provider for this request


class ChatResponse(BaseModel):
    reply: str
    companion_id: str
    provider: str
    fallback_used: bool = False    # True if a fallback provider was used


class SummarizeRequest(BaseModel):
    messages: list[ChatMessageIn]  # Full conversation to summarize


class SummarizeResponse(BaseModel):
    summary: str


class HistoryMessage(BaseModel):
    role: Role
    content: str
