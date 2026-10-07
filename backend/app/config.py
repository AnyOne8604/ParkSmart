from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    database_url: str = "postgresql+psycopg://parksmart:parksmart-local@localhost:5432/parksmart"
    jwt_secret: str = "local-only-change-before-deployment"
    jwt_expiration_minutes: int = 60 * 24
    upload_dir: str = "./uploads"
    cors_origins: str = "http://localhost:8000"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


settings = Settings()
