from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import select, text

from app.config import settings
from app.database import SessionLocal, engine
from app.models import User
from app.routes import router
from app.security import hash_password


def ensure_admin_account() -> None:
    if len(settings.admin_password) < 8:
        raise RuntimeError("ADMIN_PASSWORD debe configurarse con al menos 8 caracteres")
    with SessionLocal() as db:
        admin_email = settings.admin_email.strip().lower()
        account = db.scalar(select(User).where(User.email == admin_email))
        if account is None:
            db.add(User(
                name=settings.admin_name.strip(),
                email=admin_email,
                password_hash=hash_password(settings.admin_password),
                role="admin",
            ))
        elif account.role != "admin":
            account.name = settings.admin_name.strip()
            account.password_hash = hash_password(settings.admin_password)
            account.role = "admin"
        db.commit()


@asynccontextmanager
async def lifespan(_: FastAPI):
    ensure_admin_account()
    yield


app = FastAPI(title="ParkSmart API", version="0.6.0", description="API REST de reportes geoespaciales ciudadanos ParkSmart.", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[origin.strip() for origin in settings.cors_origins.split(",") if origin.strip()],
    allow_credentials=False,
    allow_methods=["GET", "POST", "PATCH"],
    allow_headers=["Authorization", "Content-Type"],
)
app.include_router(router)


@app.get("/health")
def health() -> dict[str, str]:
    with engine.connect() as connection:
        connection.execute(text("SELECT 1"))
    return {"status": "ok", "database": "connected"}
