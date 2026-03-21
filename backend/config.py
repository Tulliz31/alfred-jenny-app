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

    # CORS
    CORS_ORIGINS: str = "*"

    @property
    def cors_origins_list(self) -> list[str]:
        if self.CORS_ORIGINS == "*":
            return ["*"]
        return [o.strip() for o in self.CORS_ORIGINS.split(",")]


settings = Settings()
