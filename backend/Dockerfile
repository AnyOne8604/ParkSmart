# Imagen del backend: Python 3.13 mínimo para reducir tamaño y superficie de paquetes del sistema.
FROM python:3.13-slim

# Evita bytecode en la imagen y transmite logs inmediatamente a Docker.
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
# Todas las rutas siguientes son relativas a /app dentro del contenedor.
WORKDIR /app
# Instala dependencias antes de copiar fuentes para aprovechar la caché de capas Docker.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
# Copia solo API y migraciones necesarias para ejecutar el servicio.
COPY app ./app
COPY alembic.ini .
COPY migrations ./migrations
# Migra esquema hasta la última revisión y luego inicia el servidor HTTP accesible desde la red Docker.
CMD ["sh", "-c", "alembic upgrade head && uvicorn app.main:app --host 0.0.0.0 --port 8000"]
