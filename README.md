# ParkSmart

Aplicación Android para reportar situaciones de estacionamiento indebido. La versión `v0.2.0` incorpora registro e inicio de sesión con API, captura/selección de fotos, ubicación en primer plano y un backend local con FastAPI y PostgreSQL/PostGIS.

## Requisitos

- Android Studio Rabbit 1 (2026.2.1) o posterior compatible con AGP 9.4.
- Android SDK Platform 37, Build Tools 36.0.0 y JDK 17.
- Emulador API 37 o dispositivo Android 8.0 (API 26) o posterior.
- Docker Desktop para Windows con backend WSL2 activo; Docker Compose v2.29+.
- 8 GB de RAM como mínimo recomendado para Android Studio, emulador y Docker juntos.
- Conexión a Internet para descargar Gradle, dependencias Android e imágenes Docker la primera vez.

Versiones del proyecto Android: AGP 9.4.0, Gradle 9.6.0, Compose compiler 2.4.10, Compose BOM 2026.09.00, `compileSdk=37`, `targetSdk=36`, `minSdk=26`, Java 17.

## Preparar el backend

1. Copia `.env.example` a `.env` y cambia `POSTGRES_PASSWORD` y `JWT_SECRET` antes de usar datos compartidos. El valor del ejemplo es solo local.
2. Desde la raíz, inicia el stack:

   ```powershell
   docker compose up --build -d
   ```

3. Comprueba `http://localhost:8000/health`. La referencia interactiva está en `http://localhost:8000/docs`.
4. Para detener los servicios conservando base de datos e imágenes: `docker compose down`. No uses `-v` salvo que quieras borrar todos los datos locales.

El servicio de base activa la extensión PostGIS y Alembic aplica la migración inicial automáticamente. PostgreSQL almacena usuarios, reportes y geometrías; las fotos se guardan en un volumen privado de Docker y la base conserva sus claves. La imagen de PostGIS expone `5432` y la API `8000`.

## Abrir y ejecutar Android

1. En Android Studio selecciona **Open** y abre la raíz del proyecto (donde está `settings.gradle.kts`), usando una ruta Windows sin tildes ni otros caracteres no ASCII.
2. Sincroniza Gradle y selecciona `app` como configuración de ejecución.
3. Inicia el emulador API 37 o conecta el teléfono a la misma red del equipo.
4. Para emulador, el cliente usa `http://10.0.2.2:8000/`, que reenvía al host. Para un teléfono físico, cambia `API_BASE_URL` en `app/build.gradle.kts` por la IP LAN del PC (por ejemplo `http://192.168.1.20:8000/`) y permite el puerto 8000 en el firewall.
5. Asegúrate de que Docker está ejecutando la API, pulsa **Run** e inicia sesión creando una cuenta manual.

La API local usa HTTP sin TLS y `usesCleartextTraffic` está activado exclusivamente para desarrollo. No publicar esta configuración en producción. En Android, la galería usa Photo Picker sin permiso amplio de almacenamiento; cámara y ubicación solicitan permiso al utilizarlas. GPS se obtiene solo en primer plano.

## Estructura

```text
ParkSmart/
|-- app/                         # Android Kotlin + Jetpack Compose
|   `-- src/main/
|       |-- java/com/parksmart/app/
|       |   |-- data/ParkSmartApi.kt
|       |   |-- ui/ParkSmartApp.kt
|       |   `-- ui/ParkSmartViewModel.kt
|       |-- res/                  # Logo, launcher icon y FileProvider
|       `-- AndroidManifest.xml
|-- backend/
|   |-- app/                      # FastAPI, auth, rutas, modelos, settings
|   `-- migrations/               # Alembic y esquema PostGIS
|-- Contexto/                     # Mockup, logo y material del proyecto
|-- Documentacion/                # Documentos de versiones
|-- Documentacion_Ampliacion_ParkSmart_v2.docx
|-- docker-compose.yml
|-- .env.example
`-- README.md
```

## Funcionalidades de v0.2.0

- Registro manual e inicio de sesión por correo y contraseña.
- Contraseñas con Argon2 y sesión JWT guardada localmente en Android.
- Captura de foto mediante cámara del dispositivo y selección por Photo Picker; subida multipart de JPEG.
- Solicitud de permisos de cámara y ubicación precisa/aproximada durante la acción correspondiente.
- Ubicación GPS adjunta a un reporte junto con categoría y descripción.
- FastAPI documentada en OpenAPI (`/docs`) con autenticación, perfil, creación/listado de reportes, descarga autenticada de foto y resumen del usuario.
- PostgreSQL 17 + PostGIS 3.5, migración Alembic, índice GiST para coordenadas y volúmenes persistentes.
- Icono adaptativo del launcher basado en el logo de ParkSmart.

## API v1

| Método | Ruta | Acceso | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Público | Crear cuenta y devolver JWT |
| `POST` | `/api/v1/auth/login` | Público | Iniciar sesión y devolver JWT |
| `GET` | `/api/v1/auth/me` | Bearer | Consultar perfil |
| `POST` | `/api/v1/reports` | Bearer | Crear reporte multipart con foto y coordenadas |
| `GET` | `/api/v1/reports` | Bearer | Listar reportes propios |
| `GET` | `/api/v1/reports/{id}/photo` | Bearer | Obtener foto propia |
| `GET` | `/api/v1/dashboard` | Bearer | Resumen y reportes recientes |
| `GET` | `/health` | Público | Verificar API y base de datos |

## Limitaciones conocidas

- Esta es una base de desarrollo local, no una configuración de producción: secretos, HTTP, CORS, almacenamiento, límites y controles antiabuso requieren endurecimiento antes de desplegar.
- Fotos limitadas a JPEG/PNG/WebP y 10 MB en el backend. Android convierte la imagen seleccionada a JPEG en caché antes de subirla.
- El cliente envía los reportes, pero no muestra aún imágenes descargadas, paginación ni edición/eliminación.
- El mapa conserva un dibujo orientativo y no presenta mapas base ni puntos reales; las estadísticas por zona no se calculan todavía.
- La cámara se abre mediante el capturador del sistema tras pedir permiso a ParkSmart; no hay visor CameraX integrado.
- No hay recuperación de contraseña, verificación de correo, cierre remoto de sesiones ni roles administrativos.
- No se integra IA, clasificación automática, analítica agregada ni notificaciones push.
- El texto de error de algunos fallos HTTP puede ser genérico y el estado del GPS depende de que el dispositivo tenga una ubicación reciente.

## Errores encontrados y soluciones

- **La ruta contiene caracteres no ASCII:** AGP rechazó la carpeta que contenía `Programación`. Se trasladó el proyecto a una ruta sin tildes, por ejemplo `E:\Trabajos\Programacion\Ingenieria de Software\ParkSmart`.
- **`ApplicationExtensionImpl cannot be cast to BaseExtension`:** había incompatibilidad entre Kotlin Android heredado y AGP 9. Se dejó Kotlin integrado de AGP y se conservó `org.jetbrains.kotlin.plugin.compose`; no se debe volver a agregar `org.jetbrains.kotlin.android`.
- **`listOf` con demasiados argumentos y errores derivados en `DemoMapCanvas`:** las listas de puntos del dibujo quedaron anidadas incorrectamente. Cada camino se organizó como una lista de `Offset`, dentro de una lista exterior.
- **`Unresolved reference: repository`:** el repositorio se usaba en métodos del ViewModel, pero no estaba guardado como propiedad del constructor. Se corrigió a `private val repository` y se alinearon el tipo y el import con la implementación API.
- **`Java heap space`:** se configuró el heap de Gradle en 2 GB en `gradle.properties` (`-Xmx2048m`). Si vuelve a ocurrir, cierra procesos pesados y reinicia Gradle desde Android Studio.
- **`Function invocation 'error(...)' expected`:** una lambda `onFailure` usaba `error` sin declarar el parámetro. Se corrigió a `.onFailure { error -> ... }`.
- **`Unsupported escape sequence` en `build.gradle.kts`:** las comillas de la URL se escaparon como parte del literal Java que genera BuildConfig. Para emulador, usa este formato; en un teléfono físico reemplaza `10.0.2.2` por la IP LAN del PC:

  ```kotlin
  buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
  ```

  No escribas `\http`; la barra invertida solo debe preceder a las comillas dentro del argumento Kotlin.

## Próximos pasos

La hoja de ruta recomendada para las siguientes versiones es:

1. **v0.3 - Reportes e historial completos:** pruebas automatizadas; historial conectado al backend con estados y detalle; miniaturas protegidas; reintentos si falla la carga; validación de formularios y mejor manejo de errores.
2. **v0.4 - Mapa y exploración:** integrar un proveedor de mapas, mostrar reportes cercanos con filtros por categoría/fecha/estado, agrupación de marcadores y cálculo de estadísticas por zona usando PostGIS.
3. **v0.5 - Confianza y gestión:** verificación de correo y recuperación de contraseña; flujo de moderación y roles administrativos; reportar contenido incorrecto; políticas visibles de privacidad, consentimiento y retención de fotos.
4. **v0.6 - Analítica e inteligencia:** panel de tendencias y zonas críticas; evaluar clasificación asistida por IA para categorizar reportes, siempre con revisión humana y métricas de calidad.
5. **v1.0 - Preparación para uso real:** desplegar API y base de datos en infraestructura administrada; HTTPS/TLS, secretos gestionados, límites antiabuso, copias de seguridad, monitoreo, CI/CD, pruebas de seguridad y publicación Android.

Antes de avanzar de versión, conviene cerrar también estos requisitos transversales: pruebas de integración Android/API, paginación, límites y validación del contenido de imágenes, estrategia de retención, accesibilidad y pruebas en teléfonos reales. Esta hoja de ruta es una propuesta; los mapas, moderación, IA y despliegue productivo no forman parte de v0.2.0.

## Referencias técnicas

- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
- [Permisos de ubicación Android](https://developer.android.com/develop/sensors-and-location/location/permissions)
- [Seguridad JWT y hashing en FastAPI](https://fastapi.tiangolo.com/tutorial/security/oauth2-jwt/)
- [Carga de archivos con FastAPI](https://fastapi.tiangolo.com/tutorial/request-files/)
- [Imagen Docker PostGIS](https://github.com/postgis/docker-postgis)
