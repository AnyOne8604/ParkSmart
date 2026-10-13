# Funciones transversales de autenticación: Argon2 para contraseñas, JWT para sesión y dependencias de rol (v0.2/v0.5).
from datetime import datetime, timedelta, timezone
from uuid import UUID

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pwdlib import PasswordHash
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.config import settings
from app.database import get_db
from app.models import User

# Configura el algoritmo recomendado por pwdlib y el lector opcional de Bearer.
password_hash = PasswordHash.recommended()
bearer = HTTPBearer(auto_error=False)


# Convierte una contraseña a hash Argon2 para almacenar solo su derivado seguro.
def hash_password(password: str) -> str:
    return password_hash.hash(password)


# Verifica una contraseña ingresada contra el hash almacenado sin recuperar el texto original.
def verify_password(password: str, stored_hash: str) -> bool:
    return password_hash.verify(password, stored_hash)


# Firma un JWT temporal que identifica al usuario autenticado.
def create_access_token(user_id: UUID) -> str:
    expires = datetime.now(timezone.utc) + timedelta(minutes=settings.jwt_expiration_minutes)
    return jwt.encode({"sub": str(user_id), "exp": expires}, settings.jwt_secret, algorithm="HS256")


# Dependencia de ruta: valida firma/expiración del JWT y busca la cuenta activa en la base.
def current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    db: Session = Depends(get_db),
) -> User:
    if credentials is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Autenticación requerida")
    try:
        subject = jwt.decode(credentials.credentials, settings.jwt_secret, algorithms=["HS256"]).get("sub")
        user_id = UUID(subject)
    except (jwt.InvalidTokenError, ValueError, TypeError):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token inválido o vencido") from None
    user = db.scalar(select(User).where(User.id == user_id))
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Usuario no encontrado")
    return user


# Restringe operaciones sensibles a cuentas cuyo rol persistido sea admin (control desde v0.5.0).
def administrator(user: User = Depends(current_user)) -> User:
    if user.role != "admin":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Se requiere rol de administrador")
    return user
