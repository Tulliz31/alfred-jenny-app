from pydantic import BaseModel
from enum import Enum
from typing import Optional


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


class ChatResponse(BaseModel):
    reply: str
    companion_id: str
    provider: str


class HistoryMessage(BaseModel):
    role: Role
    content: str
