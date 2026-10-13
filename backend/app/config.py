# Ajustes centralizados del backend; se alimentan desde entorno/.env para evitar credenciales dentro del código.
from pydantic_settings import BaseSettings, SettingsConfigDict


# Valores de desarrollo y configuración que FastAPI utiliza en autenticación, base y almacenamiento.
class Settings(BaseSettings):
    database_url: str = "postgresql+psycopg://parksmart:parksmart-local@localhost:5432/parksmart"
    jwt_secret: str = "local-only-change-before-deployment"
    jwt_expiration_minutes: int = 60 * 24
    upload_dir: str = "./uploads"
    cors_origins: str = "http://localhost:8000"
    admin_name: str = "Carlos Ramírez"
    admin_email: str = "movilidad@gmail.com"
    admin_password: str = ""

    # Lee .env local y omite claves ajenas para permitir variables compartidas con Docker Compose.
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


settings = Settings()
