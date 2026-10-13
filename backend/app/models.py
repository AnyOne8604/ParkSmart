# Modelos ORM de las tablas users y reports; reportes geográficos/admin evolucionaron entre v0.2 y v0.6.1.
from datetime import datetime, timezone
from uuid import UUID
from uuid import uuid4

from geoalchemy2 import Geography
from sqlalchemy import DateTime, Float, ForeignKey, String, Text, Uuid
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


# Cuenta y rol persistentes; los hash reemplazan las contraseñas legibles.
class User(Base):
    __tablename__ = "users"

    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True, default=uuid4)
    name: Mapped[str] = mapped_column(String(120))
    email: Mapped[str] = mapped_column(String(254), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    role: Mapped[str] = mapped_column(String(20), default="citizen", nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    reports: Mapped[list["Report"]] = relationship(back_populates="user", foreign_keys="Report.user_id")


# Reporte ciudadano, evidencia, ubicación opcional PostGIS, decisión administrativa y evaluación del asistente.
class Report(Base):
    __tablename__ = "reports"

    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True, default=uuid4)
    user_id: Mapped[UUID] = mapped_column(Uuid, ForeignKey("users.id", ondelete="CASCADE"), index=True)
    category: Mapped[str] = mapped_column(String(40))
    description: Mapped[str] = mapped_column(Text, default="")
    photo_key: Mapped[str] = mapped_column(String(255))
    locality: Mapped[str] = mapped_column(String(40))
    address: Mapped[str] = mapped_column(String(250))
    # Puede ser nulo cuando la persona proporciona localidad y dirección manualmente (v0.3.0).
    location = mapped_column(Geography(geometry_type="POINT", srid=4326), nullable=True)
    status: Mapped[str] = mapped_column(String(30), default="received")
    # Auditoría de moderación introducida en v0.5.0; campos de categoría sugerida/revisada se añadieron en v0.6.0.
    moderated_by_id: Mapped[UUID | None] = mapped_column(Uuid, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    moderated_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    reviewed_category: Mapped[str | None] = mapped_column(String(40), nullable=True)
    category_reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    suggested_category: Mapped[str | None] = mapped_column(String(40), nullable=True)
    suggestion_confidence: Mapped[float | None] = mapped_column(Float, nullable=True)
    classifier_version: Mapped[str | None] = mapped_column(String(40), nullable=True)
    suggestion_created_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    user: Mapped[User] = relationship(back_populates="reports", foreign_keys=[user_id])
