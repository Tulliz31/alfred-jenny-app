"""
In-memory store for users and runtime state.
Replace with a real DB (SQLModel + SQLite/Postgres) when needed.
"""
from passlib.context import CryptContext
from models.user import UserInDB, Role
from config import settings

pwd_context = CryptContext(schemes=["sha256_crypt"], deprecated="auto")


def hash_password(plain: str) -> str:
    return pwd_context.hash(plain)


def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)


# ── In-memory user store ──────────────────────────────────────────────────────
USERS: dict[str, UserInDB] = {
    settings.USER_USERNAME: UserInDB(
        username=settings.USER_USERNAME,
        hashed_password=hash_password(settings.USER_PASSWORD),
        role=Role.user,
    ),
    settings.ADMIN_USERNAME: UserInDB(
        username=settings.ADMIN_USERNAME,
        hashed_password=hash_password(settings.ADMIN_PASSWORD),
        role=Role.admin,
    ),
}


def get_user(username: str) -> UserInDB | None:
    return USERS.get(username)


def authenticate_user(username: str, password: str) -> UserInDB | None:
    user = get_user(username)
    if not user:
        return None
    if not verify_password(password, user.hashed_password):
        return None
    return user
