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


class UserWithId(BaseModel):
    id: int
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


class CreateUserRequest(BaseModel):
    username: str
    password: str
    role: Role = Role.user


class UpdateUserRequest(BaseModel):
    role: Role | None = None
    password: str | None = None


class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str


class ActivityLogEntry(BaseModel):
    id: int
    timestamp: str
    username: str
    action: str
    details: str = ""
