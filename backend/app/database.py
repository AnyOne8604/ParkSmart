# Conexión SQLAlchemy a PostgreSQL/PostGIS y ciclo de vida de sesiones por petición (base v0.2.0).
from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config import settings

# pool_pre_ping detecta conexiones expiradas antes de reutilizarlas; la URL procede de configuración.
engine = create_engine(settings.database_url, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


# Clase base declarativa compartida por los modelos SQLAlchemy y las migraciones.
class Base(DeclarativeBase):
    pass


# Dependencia FastAPI: abre una sesión por operación y garantiza su cierre incluso ante errores.
def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
