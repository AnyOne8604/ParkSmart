from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, EmailStr, Field


class UserCreate(BaseModel):
    name: str = Field(min_length=2, max_length=120)
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class UserLogin(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1, max_length=128)


class UserOut(BaseModel):
    id: UUID
    name: str
    email: EmailStr
    role: str = "citizen"
    model_config = ConfigDict(from_attributes=True)


class AuthOut(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserOut


class ReportOut(BaseModel):
    id: UUID
    category: str
    description: str
    photo_url: str
    latitude: float | None
    longitude: float | None
    locality: str
    address: str
    status: str
    created_at: datetime


class DashboardOut(BaseModel):
    total_reports: int
    reports: list[ReportOut]


class NearbyReportOut(BaseModel):
    id: UUID
    category: str
    latitude: float
    longitude: float
    locality: str
    address: str
    status: str
    created_at: datetime


class ZoneStatisticOut(BaseModel):
    zone_id: str
    latitude: float
    longitude: float
    report_count: int
    automobile_count: int
    motorcycle_count: int
    other_count: int
    open_count: int


class ZoneStatisticsOut(BaseModel):
    cell_size_m: int
    total_reports: int
    zones: list[ZoneStatisticOut]


class ReportDecision(BaseModel):
    status: str = Field(pattern="^(approved|rejected)$")


class AdminReportOut(ReportOut):
    user_id: UUID
    user_name: str
    user_email: EmailStr
    moderated_at: datetime | None
