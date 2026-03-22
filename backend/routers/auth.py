from fastapi import APIRouter, HTTPException, status, Depends

import database
from models.user import (
    LoginRequest, TokenResponse, UserPublic, UserInDB,
    UserWithId, CreateUserRequest, UpdateUserRequest,
    ChangePasswordRequest, ActivityLogEntry, Role,
)
from services.auth_service import create_access_token, get_current_user, require_admin

router = APIRouter(prefix="/auth", tags=["auth"])


# ── Login / me ────────────────────────────────────────────────────────────────

@router.post("/login", response_model=TokenResponse)
async def login(body: LoginRequest):
    user = database.authenticate_user(body.username, body.password)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Credenziali non valide",
        )
    database.log_activity("login", body.username)
    token = create_access_token({"sub": user.username, "role": user.role})
    return TokenResponse(
        access_token=token,
        role=user.role,
        username=user.username,
    )


@router.get("/me", response_model=UserPublic)
async def me(current_user: UserInDB = Depends(get_current_user)):
    return UserPublic(username=current_user.username, role=current_user.role)


# ── Change own password ───────────────────────────────────────────────────────

@router.put("/me/password", status_code=204)
async def change_my_password(
    body: ChangePasswordRequest,
    current_user: UserInDB = Depends(get_current_user),
):
    if not database.verify_password(body.current_password, current_user.hashed_password):
        raise HTTPException(status_code=400, detail="Password attuale non corretta")
    if len(body.new_password) < 4:
        raise HTTPException(status_code=422, detail="La nuova password deve essere di almeno 4 caratteri")
    database.update_user(current_user.username, new_password=body.new_password)
    database.log_activity("password_changed", current_user.username)


# ── Admin: user management ────────────────────────────────────────────────────

@router.get("/admin/users", response_model=list[UserWithId])
async def list_users(_: UserInDB = Depends(require_admin)):
    return [UserWithId(**u) for u in database.list_users()]


@router.post("/admin/users", response_model=UserWithId, status_code=201)
async def create_user(body: CreateUserRequest, admin: UserInDB = Depends(require_admin)):
    if database.get_user(body.username):
        raise HTTPException(status_code=409, detail=f"Username '{body.username}' già in uso")
    if len(body.password) < 4:
        raise HTTPException(status_code=422, detail="Password deve essere di almeno 4 caratteri")
    result = database.create_user(body.username, body.password, body.role)
    database.log_activity("user_created", admin.username, f"username={body.username} role={body.role}")
    return UserWithId(**result)


@router.put("/admin/users/{username}", response_model=UserWithId)
async def update_user(
    username: str,
    body: UpdateUserRequest,
    admin: UserInDB = Depends(require_admin),
):
    if not database.get_user(username):
        raise HTTPException(status_code=404, detail="Utente non trovato")
    if body.password is not None and len(body.password) < 4:
        raise HTTPException(status_code=422, detail="Password deve essere di almeno 4 caratteri")
    new_role = Role(body.role) if body.role else None
    database.update_user(username, new_password=body.password, new_role=new_role)
    database.log_activity("user_updated", admin.username, f"username={username}")
    row = next((u for u in database.list_users() if u["username"] == username), None)
    if row is None:
        raise HTTPException(status_code=404, detail="Utente non trovato")
    return UserWithId(**row)


@router.delete("/admin/users/{username}", status_code=204)
async def delete_user(username: str, admin: UserInDB = Depends(require_admin)):
    if not database.get_user(username):
        raise HTTPException(status_code=404, detail="Utente non trovato")
    if username == admin.username:
        raise HTTPException(status_code=400, detail="Non puoi eliminare il tuo account")
    database.delete_user(username)
    database.log_activity("user_deleted", admin.username, f"username={username}")


# ── Admin: activity log ───────────────────────────────────────────────────────

@router.get("/admin/activity-log", response_model=list[ActivityLogEntry])
async def get_activity_log(_: UserInDB = Depends(require_admin)):
    return [ActivityLogEntry(**e) for e in database.get_activity_log(50)]
