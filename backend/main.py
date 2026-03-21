from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import settings
from routers import auth, chat, companions, providers

app = FastAPI(
    title="AlfredJenny API",
    description="Backend per l'app AlfredJenny — routing AI multi-provider con companion personalizzati.",
    version="1.0.0",
)

# ── CORS ─────────────────────────────────────────────────────────────────────
origins = settings.cors_origins_list
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Routers ───────────────────────────────────────────────────────────────────
app.include_router(auth.router)
app.include_router(chat.router)
app.include_router(companions.router)
app.include_router(providers.router)


@app.get("/", tags=["health"])
async def root():
    return {"status": "ok", "service": "AlfredJenny API", "version": "1.0.0"}


@app.get("/health", tags=["health"])
async def health():
    from services.ai_service import get_active_provider
    return {"status": "healthy", "active_provider": get_active_provider()}
