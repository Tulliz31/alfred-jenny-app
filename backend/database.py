"""
SQLite-backed user store and activity log.
Replaces the previous in-memory dict with a persistent database.
"""
from __future__ import annotations

import sqlite3
import threading
from datetime import datetime, timezone
from pathlib import Path

from passlib.context import CryptContext

from models.user import UserInDB, Role
from config import settings

# ── Config ────────────────────────────────────────────────────────────────────

DB_PATH = Path("data/alfred_jenny.db")
_lock = threading.Lock()
pwd_context = CryptContext(schemes=["sha256_crypt"], deprecated="auto")


# ── Connection helper ─────────────────────────────────────────────────────────

def _conn() -> sqlite3.Connection:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(DB_PATH), check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


# ── Initialisation (called at startup) ───────────────────────────────────────

def init_db() -> None:
    with _lock:
        with _conn() as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    username         TEXT    UNIQUE NOT NULL,
                    hashed_password  TEXT    NOT NULL,
                    role             TEXT    NOT NULL DEFAULT 'user'
                )
            """)
            conn.execute("""
                CREATE TABLE IF NOT EXISTS activity_log (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT    NOT NULL,
                    username  TEXT    NOT NULL DEFAULT 'system',
                    action    TEXT    NOT NULL,
                    details   TEXT    DEFAULT ''
                )
            """)
            conn.commit()

    # Seed default accounts from .env / config if not already in DB
    if get_user(settings.ADMIN_USERNAME) is None:
        _insert_user(settings.ADMIN_USERNAME, settings.ADMIN_PASSWORD, Role.admin)
    if get_user(settings.USER_USERNAME) is None:
        _insert_user(settings.USER_USERNAME, settings.USER_PASSWORD, Role.user)


# ── Password helpers ──────────────────────────────────────────────────────────

def hash_password(plain: str) -> str:
    return pwd_context.hash(plain)


def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)


# ── User CRUD ─────────────────────────────────────────────────────────────────

def _insert_user(username: str, password: str, role: Role) -> dict:
    hashed = hash_password(password)
    with _lock:
        with _conn() as conn:
            cursor = conn.execute(
                "INSERT INTO users (username, hashed_password, role) VALUES (?, ?, ?)",
                (username, hashed, role.value),
            )
            conn.commit()
    return {"id": cursor.lastrowid, "username": username, "role": role.value}


def get_user(username: str) -> UserInDB | None:
    with _conn() as conn:
        row = conn.execute(
            "SELECT username, hashed_password, role FROM users WHERE username = ?",
            (username,),
        ).fetchone()
    if row is None:
        return None
    return UserInDB(username=row["username"], hashed_password=row["hashed_password"], role=Role(row["role"]))


def authenticate_user(username: str, password: str) -> UserInDB | None:
    user = get_user(username)
    if not user or not verify_password(password, user.hashed_password):
        return None
    return user


def list_users() -> list[dict]:
    with _conn() as conn:
        rows = conn.execute("SELECT id, username, role FROM users ORDER BY id").fetchall()
    return [{"id": r["id"], "username": r["username"], "role": r["role"]} for r in rows]


def create_user(username: str, password: str, role: Role) -> dict:
    return _insert_user(username, password, role)


def update_user(
    username: str,
    *,
    new_password: str | None = None,
    new_role: Role | None = None,
) -> bool:
    parts, params = [], []
    if new_password is not None:
        parts.append("hashed_password = ?")
        params.append(hash_password(new_password))
    if new_role is not None:
        parts.append("role = ?")
        params.append(new_role.value)
    if not parts:
        return False
    params.append(username)
    with _lock:
        with _conn() as conn:
            conn.execute(f"UPDATE users SET {', '.join(parts)} WHERE username = ?", params)
            conn.commit()
    return True


def delete_user(username: str) -> bool:
    with _lock:
        with _conn() as conn:
            cursor = conn.execute("DELETE FROM users WHERE username = ?", (username,))
            conn.commit()
    return cursor.rowcount > 0


# ── Activity log ──────────────────────────────────────────────────────────────

def log_activity(action: str, username: str = "system", details: str = "") -> None:
    ts = datetime.now(timezone.utc).isoformat(timespec="seconds")
    with _lock:
        with _conn() as conn:
            conn.execute(
                "INSERT INTO activity_log (timestamp, username, action, details) VALUES (?, ?, ?, ?)",
                (ts, username, action, details),
            )
            conn.commit()


def get_activity_log(limit: int = 50) -> list[dict]:
    with _conn() as conn:
        rows = conn.execute(
            "SELECT id, timestamp, username, action, details "
            "FROM activity_log ORDER BY id DESC LIMIT ?",
            (limit,),
        ).fetchall()
    return [
        {"id": r["id"], "timestamp": r["timestamp"], "username": r["username"],
         "action": r["action"], "details": r["details"]}
        for r in rows
    ]
