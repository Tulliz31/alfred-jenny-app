from pydantic_settings import BaseSettings, SettingsConfigDict
from models.provider import ProviderID


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # JWT
    JWT_SECRET: str = "dev_secret_change_me"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRE_MINUTES: int = 1440

    # Users
    USER_USERNAME: str = "utente"
    USER_PASSWORD: str = "1234"
    ADMIN_USERNAME: str = "admin"
    ADMIN_PASSWORD: str = "AlfredJenny#2024!"

    # AI
    ACTIVE_PROVIDER: ProviderID = ProviderID.openai
    OPENAI_API_KEY: str = ""
    ANTHROPIC_API_KEY: str = ""
    GEMINI_API_KEY: str = ""

    # ElevenLabs TTS (optional — app can supply its own key per request)
    ELEVENLABS_API_KEY: str = ""

    # Tuya Smart Home
    TUYA_CLIENT_ID: str = ""
    TUYA_CLIENT_SECRET: str = ""
    TUYA_BASE_URL: str = "https://openapi.tuyaeu.com"   # EU region by default
    TUYA_USER_UID: str = ""   # optional – filters devices by user UID

    # CORS
    CORS_ORIGINS: str = "*"

    @property
    def cors_origins_list(self) -> list[str]:
        if self.CORS_ORIGINS == "*":
            return ["*"]
        return [o.strip() for o in self.CORS_ORIGINS.split(",")]


settings = Settings()
