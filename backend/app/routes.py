from pathlib import Path
from uuid import uuid4

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from fastapi.responses import FileResponse
from geoalchemy2 import WKTElement
from geoalchemy2.shape import to_shape
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.config import settings
from app.database import get_db
from app.models import Report, User
from app.schemas import AuthOut, DashboardOut, ReportOut, UserCreate, UserLogin, UserOut
from app.security import create_access_token, current_user, hash_password, verify_password

router = APIRouter(prefix="/api/v1")
UPLOAD_ROOT = Path(settings.upload_dir).resolve()
MAX_PHOTO_BYTES = 10 * 1024 * 1024
ALLOWED_TYPES = {"image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp"}
ALLOWED_CATEGORIES = {"Automóvil", "Motocicleta", "Otro"}


def auth_response(user: User) -> AuthOut:
    return AuthOut(access_token=create_access_token(user.id), user=user)


def report_out(report: Report) -> ReportOut:
    point = to_shape(report.location)
    return ReportOut(
        id=report.id,
        category=report.category,
        description=report.description,
        photo_url=f"/api/v1/reports/{report.id}/photo",
        latitude=point.y,
        longitude=point.x,
        status=report.status,
        created_at=report.created_at,
    )


@router.post("/auth/register", response_model=AuthOut, status_code=status.HTTP_201_CREATED)
def register(payload: UserCreate, db: Session = Depends(get_db)) -> AuthOut:
    email = str(payload.email).lower()
    if db.scalar(select(User.id).where(User.email == email)):
        raise HTTPException(status_code=409, detail="Ya existe una cuenta con ese correo")
    user = User(name=payload.name.strip(), email=email, password_hash=hash_password(payload.password))
    db.add(user)
    db.commit()
    db.refresh(user)
    return auth_response(user)


@router.post("/auth/login", response_model=AuthOut)
def login(payload: UserLogin, db: Session = Depends(get_db)) -> AuthOut:
    user = db.scalar(select(User).where(User.email == str(payload.email).lower()))
    if user is None or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Correo o contraseña incorrectos")
    return auth_response(user)


@router.get("/auth/me", response_model=UserOut)
def me(user: User = Depends(current_user)) -> User:
    return user


@router.post("/reports", response_model=ReportOut, status_code=status.HTTP_201_CREATED)
async def create_report(
    category: str = Form(min_length=2, max_length=40),
    description: str = Form(default="", max_length=1000),
    latitude: float = Form(ge=-90, le=90),
    longitude: float = Form(ge=-180, le=180),
    photo: UploadFile = File(),
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> ReportOut:
    extension = ALLOWED_TYPES.get(photo.content_type or "")
    if extension is None:
        raise HTTPException(status_code=415, detail="Usa una foto JPEG, PNG o WebP")
    category = category.strip()
    if category not in ALLOWED_CATEGORIES:
        raise HTTPException(status_code=422, detail="Tipo de vehículo no válido")
    contents = await photo.read(MAX_PHOTO_BYTES + 1)
    if not contents or len(contents) > MAX_PHOTO_BYTES:
        raise HTTPException(status_code=413, detail="La foto debe pesar como máximo 10 MB")
    key = f"{uuid4().hex}{extension}"
    UPLOAD_ROOT.mkdir(parents=True, exist_ok=True)
    (UPLOAD_ROOT / key).write_bytes(contents)
    report = Report(
        user_id=user.id,
        category=category,
        description=description.strip(),
        photo_key=key,
        location=WKTElement(f"POINT({longitude} {latitude})", srid=4326),
    )
    db.add(report)
    db.commit()
    db.refresh(report)
    return report_out(report)


@router.get("/reports", response_model=list[ReportOut])
def list_reports(user: User = Depends(current_user), db: Session = Depends(get_db)) -> list[ReportOut]:
    reports = db.scalars(select(Report).where(Report.user_id == user.id).order_by(Report.created_at.desc())).all()
    return [report_out(report) for report in reports]


@router.get("/reports/{report_id}/photo")
def report_photo(report_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)) -> FileResponse:
    report = db.scalar(select(Report).where(Report.id == report_id, Report.user_id == user.id))
    if report is None:
        raise HTTPException(status_code=404, detail="Reporte no encontrado")
    path = (UPLOAD_ROOT / report.photo_key).resolve()
    if path.parent != UPLOAD_ROOT or not path.is_file():
        raise HTTPException(status_code=404, detail="Foto no encontrada")
    return FileResponse(path)


@router.get("/dashboard", response_model=DashboardOut)
def dashboard(user: User = Depends(current_user), db: Session = Depends(get_db)) -> DashboardOut:
    reports = db.scalars(select(Report).where(Report.user_id == user.id).order_by(Report.created_at.desc()).limit(20)).all()
    total = db.scalar(select(func.count(Report.id)).where(Report.user_id == user.id)) or 0
    return DashboardOut(total_reports=total, reports=[report_out(report) for report in reports])
