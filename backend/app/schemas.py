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
    latitude: float
    longitude: float
    status: str
    created_at: datetime


class DashboardOut(BaseModel):
    total_reports: int
    reports: list[ReportOut]
