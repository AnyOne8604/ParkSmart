from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path
from uuid import uuid4
from zoneinfo import ZoneInfo

from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, status
from fastapi.responses import FileResponse
from geoalchemy2 import Geography, WKTElement
from geoalchemy2.shape import to_shape
from sqlalchemy import cast, func, select, text
from sqlalchemy.orm import Session

from app.config import settings
from app.database import get_db
from app.classification import LabeledReport, LocalityCategoryClassifier, classification_metrics
from app.models import Report, User
from app.schemas import AdminReportOut, AnalyticsOut, AuthOut, CategoryReview, ClassificationMetricsOut, DailyTrendOut, DashboardOut, DimensionCountOut, LocalityCountOut, NearbyReportOut, ReportDecision, ReportOut, UserCreate, UserLogin, UserOut, ZoneStatisticsOut
from app.security import administrator, create_access_token, current_user, hash_password, verify_password

router = APIRouter(prefix="/api/v1")
UPLOAD_ROOT = Path(settings.upload_dir).resolve()
MAX_PHOTO_BYTES = 10 * 1024 * 1024
ALLOWED_TYPES = {"image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp"}
ALLOWED_CATEGORIES = {"Automóvil", "Motocicleta", "Otro"}
BOGOTA_TIMEZONE = ZoneInfo("America/Bogota")
BOGOTA_LOCALITIES = (
    "Usaquén", "Chapinero", "Santa Fe", "San Cristóbal", "Usme", "Tunjuelito", "Bosa",
    "Kennedy", "Fontibón", "Engativá", "Suba", "Barrios Unidos", "Teusaquillo",
    "Los Mártires", "Antonio Nariño", "Puente Aranda", "La Candelaria",
    "Rafael Uribe Uribe", "Ciudad Bolívar", "Sumapaz",
)


def canonical_bogota_locality(value: str) -> str | None:
    return next((item for item in BOGOTA_LOCALITIES if item.casefold() == value.strip().casefold()), None)


def auth_response(user: User) -> AuthOut:
    return AuthOut(access_token=create_access_token(user.id), user=user)


def report_out(report: Report) -> ReportOut:
    point = to_shape(report.location) if report.location is not None else None
    return ReportOut(
        id=report.id,
        category=report.reviewed_category or report.category,
        description=report.description,
        photo_url=f"/api/v1/reports/{report.id}/photo",
        latitude=point.y if point else None,
        longitude=point.x if point else None,
        locality=report.locality,
        address=report.address,
        status=report.status,
        created_at=report.created_at,
    )


def admin_report_out(report: Report, prediction: tuple[str, float] | None = None) -> AdminReportOut:
    data = report_out(report).model_dump()
    data["photo_url"] = f"/api/v1/admin/reports/{report.id}/photo"
    if report.reviewed_category:
        data["category"] = report.reviewed_category
    return AdminReportOut(
        **data,
        user_id=report.user_id,
        user_name=report.user.name,
        user_email=report.user.email,
        moderated_at=report.moderated_at,
        reviewed_category=report.reviewed_category,
        suggested_category=prediction[0] if prediction and report.reviewed_category is None else None,
        suggestion_confidence=prediction[1] if prediction and report.reviewed_category is None else None,
    )


def locality_level(report_count: int) -> str:
    if report_count >= 10:
        return "Alta"
    if report_count >= 5:
        return "Media"
    return "Baja"


def reviewed_examples(db: Session) -> list[LabeledReport]:
    rows = db.execute(
        select(Report.description, Report.reviewed_category)
        .where(Report.reviewed_category.is_not(None), func.length(func.trim(Report.description)) > 0)
    ).all()
    return [LabeledReport(text=description or "", category=category) for description, category in rows]


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
    locality: str = Form(min_length=2, max_length=40),
    address: str = Form(min_length=4, max_length=250),
    latitude: float | None = Form(default=None, ge=-90, le=90),
    longitude: float | None = Form(default=None, ge=-180, le=180),
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
    locality = canonical_bogota_locality(locality)
    if locality is None:
        raise HTTPException(status_code=422, detail="Selecciona una localidad válida de Bogotá")
    address = " ".join(address.split())
    if len(address) < 4:
        raise HTTPException(status_code=422, detail="Escribe una dirección más detallada")
    if (latitude is None) != (longitude is None):
        raise HTTPException(status_code=422, detail="Envía ambas coordenadas GPS o ninguna")
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
        locality=locality,
        address=address,
        location=WKTElement(f"POINT({longitude} {latitude})", srid=4326) if latitude is not None else None,
        status="received",
    )
    db.add(report)
    db.commit()
    db.refresh(report)
    return report_out(report)


@router.get("/reports", response_model=list[ReportOut])
def list_reports(user: User = Depends(current_user), db: Session = Depends(get_db)) -> list[ReportOut]:
    reports = db.scalars(select(Report).where(Report.user_id == user.id).order_by(Report.created_at.desc())).all()
    return [report_out(report) for report in reports]


@router.get("/admin/reports", response_model=list[AdminReportOut])
def list_all_reports(
    _: User = Depends(administrator),
    db: Session = Depends(get_db),
) -> list[AdminReportOut]:
    reports = db.scalars(select(Report).join(Report.user).order_by(Report.created_at.desc())).all()
    classifier = LocalityCategoryClassifier(reviewed_examples(db))
    return [admin_report_out(report, classifier.predict(report.description)) for report in reports]


@router.post("/admin/reports/{report_id}/category-review", response_model=AdminReportOut)
def review_report_category(
    report_id: str,
    review: CategoryReview,
    admin: User = Depends(administrator),
    db: Session = Depends(get_db),
) -> AdminReportOut:
    report = db.scalar(select(Report).where(Report.id == report_id))
    if report is None:
        raise HTTPException(status_code=404, detail="Reporte no encontrado")
    report.reviewed_category = review.category
    report.category_reviewed_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(report)
    classifier = LocalityCategoryClassifier(reviewed_examples(db))
    return admin_report_out(report, classifier.predict(report.description))


@router.get("/admin/classification/metrics", response_model=ClassificationMetricsOut)
def category_classification_metrics(
    _: User = Depends(administrator),
    db: Session = Depends(get_db),
) -> ClassificationMetricsOut:
    return ClassificationMetricsOut(**classification_metrics(reviewed_examples(db)))


@router.get("/analytics", response_model=AnalyticsOut)
def report_analytics(_: User = Depends(current_user), db: Session = Depends(get_db)) -> AnalyticsOut:
    local_today = datetime.now(BOGOTA_TIMEZONE).date()
    start_today = datetime.combine(local_today, time.min, BOGOTA_TIMEZONE).astimezone(timezone.utc)
    start_tomorrow = datetime.combine(local_today + timedelta(days=1), time.min, BOGOTA_TIMEZONE).astimezone(timezone.utc)
    start_30_days = start_today - timedelta(days=29)

    locality_rows = db.execute(
        select(Report.locality, func.count(Report.id).label("report_count"))
        .where(Report.created_at >= start_today, Report.created_at < start_tomorrow)
        .group_by(Report.locality)
        .order_by(func.count(Report.id).desc(), Report.locality.asc())
    ).all()
    daily_rows = db.execute(
        text(
            """
            SELECT (created_at AT TIME ZONE 'America/Bogota')::date AS report_date, COUNT(*) AS report_count
            FROM reports
            WHERE created_at >= :start_30_days AND created_at < :start_tomorrow
            GROUP BY report_date
            """
        ),
        {"start_30_days": start_30_days, "start_tomorrow": start_tomorrow},
    ).all()
    daily_counts = {day: count for day, count in daily_rows}
    daily_trend = [
        DailyTrendOut(date=(local_today - timedelta(days=offset)).isoformat(), report_count=daily_counts.get(local_today - timedelta(days=offset), 0))
        for offset in reversed(range(30))
    ]
    effective_category = func.coalesce(Report.reviewed_category, Report.category)
    category_rows = db.execute(
        select(effective_category, func.count(Report.id))
        .where(Report.created_at >= start_30_days, Report.created_at < start_tomorrow)
        .group_by(effective_category)
        .order_by(func.count(Report.id).desc())
    ).all()
    status_rows = db.execute(
        select(Report.status, func.count(Report.id))
        .where(Report.created_at >= start_30_days, Report.created_at < start_tomorrow)
        .group_by(Report.status)
        .order_by(func.count(Report.id).desc())
    ).all()
    return AnalyticsOut(
        today=local_today.isoformat(),
        total_reports_today=sum(count for _, count in locality_rows),
        total_reports_30_days=sum(daily_counts.values()),
        localities_today=[LocalityCountOut(locality=name, report_count=count, level=locality_level(count)) for name, count in locality_rows],
        daily_trend=daily_trend,
        category_counts=[DimensionCountOut(name=name, report_count=count) for name, count in category_rows],
        status_counts=[DimensionCountOut(name=name, report_count=count) for name, count in status_rows],
    )


@router.patch("/admin/reports/{report_id}/decision", response_model=AdminReportOut)
def decide_report(
    report_id: str,
    decision: ReportDecision,
    admin: User = Depends(administrator),
    db: Session = Depends(get_db),
) -> AdminReportOut:
    report = db.scalar(select(Report).where(Report.id == report_id))
    if report is None:
        raise HTTPException(status_code=404, detail="Reporte no encontrado")
    report.status = decision.status
    report.moderated_by_id = admin.id
    report.moderated_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(report)
    return admin_report_out(report)


@router.get("/admin/reports/{report_id}/photo")
def admin_report_photo(
    report_id: str,
    _: User = Depends(administrator),
    db: Session = Depends(get_db),
) -> FileResponse:
    report = db.scalar(select(Report).where(Report.id == report_id))
    if report is None:
        raise HTTPException(status_code=404, detail="Reporte no encontrado")
    path = (UPLOAD_ROOT / report.photo_key).resolve()
    if path.parent != UPLOAD_ROOT or not path.is_file():
        raise HTTPException(status_code=404, detail="Foto no encontrada")
    return FileResponse(path)


def validate_map_filters(category: str | None, status_filter: str | None) -> None:
    if category is not None and category not in ALLOWED_CATEGORIES:
        raise HTTPException(status_code=422, detail="Categoría de reporte no válida")
    if status_filter is not None and status_filter not in {"received", "in_review", "closed", "approved", "rejected"}:
        raise HTTPException(status_code=422, detail="Estado de reporte no válido")


@router.get("/reports/nearby", response_model=list[NearbyReportOut])
def nearby_reports(
    latitude: float = Query(4.7110, ge=-90, le=90),
    longitude: float = Query(-74.0721, ge=-180, le=180),
    radius_m: int = Query(25000, ge=500, le=25000),
    category: str | None = Query(default=None, max_length=40),
    status_filter: str | None = Query(default=None, alias="status", max_length=30),
    since_days: int | None = Query(default=None, ge=1, le=365),
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[NearbyReportOut]:
    validate_map_filters(category, status_filter)
    center = cast(func.ST_SetSRID(func.ST_MakePoint(longitude, latitude), 4326), Geography(geometry_type="POINT", srid=4326))
    query = select(Report).where(
        Report.location.is_not(None),
        func.ST_DWithin(Report.location, center, radius_m),
    )
    effective_category = func.coalesce(Report.reviewed_category, Report.category)
    if category:
        query = query.where(effective_category == category)
    if status_filter:
        query = query.where(Report.status == status_filter)
    if since_days:
        query = query.where(Report.created_at >= datetime.now(timezone.utc) - timedelta(days=since_days))

    reports = db.scalars(query.order_by(func.ST_Distance(Report.location, center)).limit(500)).all()
    result = []
    for report in reports:
        point = to_shape(report.location)
        result.append(
            NearbyReportOut(
                id=report.id,
                category=report.reviewed_category or report.category,
                latitude=point.y,
                longitude=point.x,
                locality=report.locality,
                address=report.address,
                status=report.status,
                created_at=report.created_at,
            )
        )
    return result


@router.get("/reports/zones", response_model=ZoneStatisticsOut)
def report_zone_statistics(
    category: str | None = Query(default=None, max_length=40),
    status_filter: str | None = Query(default=None, alias="status", max_length=30),
    since_days: int | None = Query(default=None, ge=1, le=365),
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> ZoneStatisticsOut:
    validate_map_filters(category, status_filter)
    since_time = datetime.now(timezone.utc) - timedelta(days=since_days) if since_days else None
    rows = db.execute(
        text(
            """
            WITH filtered AS (
                SELECT
                    ST_SnapToGrid(ST_Transform(location::geometry, 3116), 1000) AS cell,
                    COALESCE(reviewed_category, category) AS category,
                    status
                FROM reports
                WHERE location IS NOT NULL
                  AND ST_Y(location::geometry) BETWEEN 3.7 AND 5.2
                  AND ST_X(location::geometry) BETWEEN -74.5 AND -73.5
                  AND (CAST(:category AS VARCHAR) IS NULL OR COALESCE(reviewed_category, category) = :category)
                  AND (CAST(:status AS VARCHAR) IS NULL OR status = :status)
                  AND (CAST(:since_time AS TIMESTAMPTZ) IS NULL OR created_at >= :since_time)
            ), grouped AS (
                SELECT
                    cell,
                    COUNT(*) AS report_count,
                    COUNT(*) FILTER (WHERE category = 'Automóvil') AS automobile_count,
                    COUNT(*) FILTER (WHERE category = 'Motocicleta') AS motorcycle_count,
                    COUNT(*) FILTER (WHERE category = 'Otro') AS other_count,
                    COUNT(*) FILTER (WHERE status NOT IN ('closed', 'rejected')) AS open_count
                FROM filtered
                GROUP BY cell
            )
            SELECT
                CONCAT(FLOOR(ST_X(cell) / 1000)::INT, ':', FLOOR(ST_Y(cell) / 1000)::INT) AS zone_id,
                ST_Y(ST_Transform(ST_Centroid(cell), 4326)) AS latitude,
                ST_X(ST_Transform(ST_Centroid(cell), 4326)) AS longitude,
                report_count,
                automobile_count,
                motorcycle_count,
                other_count,
                open_count
            FROM grouped
            ORDER BY report_count DESC
            """
        ),
        {"category": category, "status": status_filter, "since_time": since_time},
    ).mappings().all()
    zones = [dict(row) for row in rows]
    return ZoneStatisticsOut(
        cell_size_m=1000,
        total_reports=sum(zone["report_count"] for zone in zones),
        zones=zones,
    )


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
