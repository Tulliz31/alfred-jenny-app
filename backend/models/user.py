from pydantic import BaseModel
from enum import Enum


class Role(str, Enum):
    user = "user"
    admin = "admin"


class UserInDB(BaseModel):
    username: str
    hashed_password: str
    role: Role


class UserPublic(BaseModel):
    username: str
    role: Role


class LoginRequest(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    role: Role
    username: str
