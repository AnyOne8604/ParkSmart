# ParkSmart

Aplicación Android para reportar situaciones de estacionamiento indebido. La versión `v0.7.0` pule el acceso y la navegación, mejora el perfil y corrige la orientación de las fotografías.

La versión `v0.7.1` agrega comentarios explicativos a lo largo del código del proyecto para describir la UI, el diseño, las funciones, el backend, Docker y las versiones en que se incorporaron sus componentes. No modifica el comportamiento de la aplicación.

## Requisitos

- Android Studio Rabbit 1 (2026.2.1) o posterior compatible con AGP 9.4.
- Android SDK Platform 37, Build Tools 36.0.0 y JDK 17.
- Emulador API 37 o dispositivo Android 8.0 (API 26) o posterior.
- Docker Desktop para Windows con backend WSL2 activo; Docker Compose v2.29+.
- 8 GB de RAM como mínimo recomendado para Android Studio, emulador y Docker juntos.
- Conexión a Internet para descargar Gradle, dependencias Android e imágenes Docker la primera vez.

Versiones del proyecto Android: AGP 9.4.0, Gradle 9.6.0, Compose compiler 2.4.10, Compose BOM 2026.09.00, MapLibre Native 11.8.0, `compileSdk=37`, `targetSdk=36`, `minSdk=26`, Java 17.

## Preparar el backend

1. Copia `.env.example` a `.env` y cambia `POSTGRES_PASSWORD`, `JWT_SECRET` y `PGADMIN_DEFAULT_PASSWORD`. Los valores del ejemplo son solo para desarrollo local.
2. Desde la raíz, inicia el stack:

   ```powershell
   docker compose up --build -d
   ```

3. Comprueba `http://localhost:8000/health`; la referencia de la API está en `http://localhost:8000/docs`.
4. Abre pgAdmin en `http://localhost:5050` e inicia sesión con `PGADMIN_DEFAULT_EMAIL` y `PGADMIN_DEFAULT_PASSWORD` del `.env`. Si no están definidos, los valores locales por defecto son `admin@parksmart.com` y `parksmart-local-admin`. Registra el servidor con host `db`, puerto `5432`, base `parksmart`, usuario `parksmart` y la contraseña `POSTGRES_PASSWORD`.
5. Para detener los servicios conservando base de datos e imágenes: `docker compose down`. No uses `-v` salvo que quieras borrar todos los datos locales.

Alembic aplica las migraciones al iniciar la API. La migración v0.3 conserva los reportes previos y permite que la geometría sea nula cuando el ciudadano indica la dirección manualmente. PostgreSQL almacena usuarios, reportes y geometrías; las fotos se guardan en un volumen privado de Docker. pgAdmin solo se publica en `127.0.0.1:5050`, no en la red local.

## Abrir y ejecutar Android

1. En Android Studio selecciona **Open** y abre la raíz del proyecto (donde está `settings.gradle.kts`), usando una ruta Windows sin tildes ni otros caracteres no ASCII.
2. Sincroniza Gradle y selecciona `app` como configuración de ejecución.
3. Inicia el emulador API 37 o conecta el teléfono a la misma red del equipo.
4. Para emulador, el cliente usa `http://10.0.2.2:8000/`, que reenvía al host. Para un teléfono físico, agrega `parksmart.apiBaseUrl=http://IP_DEL_PC:8000/` a `local.properties` (por ejemplo `http://192.168.1.20:8000/`) y permite el puerto 8000 en el firewall. `local.properties` se ignora en Git, por lo que esta dirección no se comparte ni altera el proyecto de otros equipos.
5. Opcionalmente, define `parksmart.mapTileUrl` en `local.properties` para usar un proveedor de teselas compatible con el estilo raster de MapLibre. Si no se define, se usa OpenStreetMap durante desarrollo.
6. Asegúrate de que Docker está ejecutando la API, pulsa **Run** e inicia sesión creando una cuenta manual.

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
|       |-- versions/0002_manual_report_location.py
|       `-- versions/0003_admin_moderation.py
|       |-- versions/0004_analytics_classification.py
|       `-- versions/0005_independent_quality.py
|-- backend/test_localities.py
|-- Contexto/                     # Mockup, logo y material del proyecto
|-- Documentacion/                # Documentos de versiones anteriores
|-- Documentacion_Ampliacion_ParkSmart_v3.docx
|-- Documentacion_Ampliacion_ParkSmart_v4.docx
|-- docker-compose.yml
|-- .env.example
`-- README.md
```

## Funcionalidades de v0.7.0

- Registro manual e inicio de sesión por correo y contraseña.
- Contraseñas con Argon2 y sesión JWT guardada localmente en Android.
- Captura de foto mediante cámara del dispositivo y selección por Photo Picker; subida multipart de JPEG.
- Solicitud de permiso de cámara y permiso de ubicación solo cuando se desea capturar GPS; los reportes no dependen de ese permiso.
- Desplegable con las 20 localidades oficiales de Bogotá y campo obligatorio para la dirección exacta.
- Ubicación GPS opcional adjunta junto con localidad, dirección, categoría, descripción y fotografía.
- Historial mostrando localidad y dirección en cada reporte.
- FastAPI documentada en OpenAPI (`/docs`) con autenticación, perfil, creación/listado de reportes, descarga autenticada de foto y resumen del usuario.
- PostgreSQL 17 + PostGIS 3.5, migración Alembic, índice GiST para coordenadas y volúmenes persistentes.
- pgAdmin en `http://localhost:5050` para consultar tablas y registros de desarrollo.
- Mapa interactivo con MapLibre Native y teselas raster de OpenStreetMap, centrado en Bogotá y sin API key de Google.
- Marcadores agrupados por clusters en el cliente; tocar un grupo acerca la vista y tocar un punto muestra los datos del reporte.
- Filtros por categoría de vehículo, estado y periodo (7, 30, 90 días o todo el historial).
- API autenticada de reportes cercanos mediante radio de 25 km y estadísticas PostGIS en cuadrículas de 1 km proyectadas en EPSG:3116.
- Estadísticas por cuadrícula: total, categorías y reportes abiertos; respetan los filtros del mapa.
- Icono adaptativo del launcher basado en el logo de ParkSmart.
- Roles persistidos `citizen` y `admin`; las contraseñas se almacenan como hash Argon2.
- Cuenta inicial para Carlos Ramírez (`movilidad@gmail.com`), creada por el backend al iniciar si aún no existe.
- Control administrativo con todos los reportes, información del remitente y foto protegida; permite aprobar o negar cada reporte.
- Los reportes nuevos inician en `received`; las decisiones de moderación usan los estados `approved` y `rejected`.
- Perfil reemplaza Notificaciones por Políticas de privacidad, que explica datos tratados, GPS opcional, acceso y conservación actual de fotos.
- Inicio muestra las tres localidades con más reportes del día calendario de Bogotá. “Ver más” abre el listado completo, descendente y sin localidades en cero.
- Los niveles diarios son Alta (10 o más), Media (5 a 9) y Baja (1 a 4).
- Estadísticas agrega totales de hoy y últimos 30 días, tendencia diaria y desgloses globales por categoría y estado.
- Clasificador local Naive Bayes sobre el texto de la descripción (no analiza fotos), entrenado únicamente con categorías revisadas por administración. Requiere al menos 12 etiquetas y dos clases; sin suficientes ejemplos no emite sugerencias.
- Control permite solicitar una sugerencia explícita, confirmar o corregir la categoría. La sugerencia, confianza y versión del clasificador se guardan antes de la revisión humana; abrir la lista no genera predicciones de forma oculta.
- Entrenamiento y evaluación se separan de forma estable por identificador de reporte. Los reportes de evaluación apartados no se utilizan para entrenar el modelo. La evaluación compara el asistente con la categoría inicial del reportante sobre los mismos casos y muestra exactitud, precisión, cobertura y F1 por categoría.
- El panel diferencia que un modelo pueda generar sugerencias de que la evaluación ya tenga datos suficientes. Solo presenta puntuaciones cuando existen al menos 15 reportes de evaluación y 3 ejemplos confirmados por categoría; hasta entonces muestra tamaños y oculta puntuaciones.
- Estadísticas compara ventanas móviles de los últimos 7 días con los 7 anteriores, y los últimos 30 con los 30 anteriores, mostrando conteos y variación porcentual cuando existe un periodo previo no vacío.
- La clasificación no examina fotos ni usa un servicio externo; no cambia categorías ni decisiones automáticamente. La categoría inicial se conserva como línea base y la categoría revisada como referencia humana.
- Inicio de sesión y registro usan una imagen de fondo de Bogotá, con el formulario sobre la transición clara, el eslogan bajo el enlace de acceso/registro y los campos desplazados hacia la zona blanca.
- El avatar de la pantalla Inicio abre Perfil. Información personal muestra nombre, correo de la cuenta y el total de reportes.
- Los controles de cámara, galería, ubicación y localidad usan el verde tenue de la interfaz; los selectores de vehículo y el envío mantienen sus estilos destacados.
- Los tipos de vehículo Motocicleta y Otro tienen iconos específicos de moto y bus.
- Las imágenes de cámara y galería se normalizan según su orientación EXIF antes de guardarse y enviarse. El panel administrativo también corrige la orientación EXIF al mostrar las fotos existentes.
- El icono adaptativo aplica márgenes de seguridad al logo para evitar que el recorte circular del launcher corte sus elementos.

La cuenta administrativa se configura mediante `ADMIN_NAME`, `ADMIN_EMAIL` y `ADMIN_PASSWORD` en `.env`. El valor de demostración debe reemplazarse antes de compartir o desplegar el sistema; no se debe reutilizar en producción.

MapLibre es el motor cartográfico; OpenStreetMap aporta los datos/teselas de desarrollo. `tile.openstreetmap.org` es un servicio de mejor esfuerzo: exige atribución, identificación y respeto de caché, y no es una infraestructura de producción ni permite precarga/offline. Cambia `parksmart.mapTileUrl` a un proveedor contratado o teselas propias antes de desplegar.

La geocodificación de direcciones manuales se evaluó y no se activa en v0.4: los domicilios no se envían automáticamente a terceros. Nominatim público limita el uso a un máximo absoluto de una solicitud por segundo y exige identificar la app y mostrar atribución. Antes de añadir búsqueda de dirección, se debe pedir consentimiento, limitar frecuencia y revisar privacidad. Una dirección manual no se convierte en coordenadas todavía.

## API v1

| Método | Ruta | Acceso | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Público | Crear cuenta y devolver JWT |
| `POST` | `/api/v1/auth/login` | Público | Iniciar sesión y devolver JWT |
| `GET` | `/api/v1/auth/me` | Bearer | Consultar perfil |
| `POST` | `/api/v1/reports` | Bearer | Crear reporte multipart con foto, localidad y dirección; coordenadas GPS opcionales |
| `GET` | `/api/v1/reports` | Bearer | Listar reportes propios |
| `GET` | `/api/v1/reports/nearby` | Bearer | Reportes con GPS por radio; filtros `category`, `status`, `since_days` |
| `GET` | `/api/v1/reports/zones` | Bearer | Estadísticas PostGIS por cuadrícula de 1 km con filtros equivalentes |
| `GET` | `/api/v1/analytics` | Bearer | Conteos diarios y comparaciones de ventanas móviles de 7 y 30 días contra sus periodos previos |
| `GET` | `/api/v1/admin/reports` | Bearer admin | Listar reportes de todos los usuarios |
| `GET` | `/api/v1/admin/reports/{id}/photo` | Bearer admin | Obtener la foto para moderación |
| `PATCH` | `/api/v1/admin/reports/{id}/decision` | Bearer admin | Aprobar (`approved`) o negar (`rejected`) |
| `POST` | `/api/v1/admin/reports/{id}/category-review` | Bearer admin | Confirmar/corregir categoría humana para evaluación y entrenamiento local |
| `POST` | `/api/v1/admin/reports/{id}/classification-suggestion` | Bearer admin | Generar y guardar una sugerencia local previa a la revisión humana |
| `GET` | `/api/v1/admin/classification/metrics` | Bearer admin | Evaluación independiente y comparación con categoría inicial |
| `GET` | `/api/v1/reports/{id}/photo` | Bearer | Obtener foto propia |
| `GET` | `/api/v1/dashboard` | Bearer | Resumen y reportes recientes |
| `GET` | `/health` | Público | Verificar API y base de datos |

## Limitaciones conocidas

- Esta es una base de desarrollo local, no una configuración de producción: secretos, HTTP, CORS, almacenamiento, límites y controles antiabuso requieren endurecimiento antes de desplegar.
- Fotos limitadas a JPEG/PNG/WebP y 10 MB en el backend. Android convierte la imagen seleccionada a JPEG en caché antes de subirla.
- El cliente envía los reportes, pero no muestra aún imágenes descargadas, paginación ni edición/eliminación.
- Los reportes sin GPS no se muestran como puntos ni participan en agregados espaciales; su dirección permanece en el historial.
- Las teselas OSM estándar son solo para desarrollo/pruebas interactivas; no usarlas para tráfico alto ni descargas offline. Producción requiere proveedor con SLA o servicio propio.
- El endpoint de mapa busca en un radio de 25 km alrededor de la cámara y devuelve como máximo 500 reportes; no hay paginación espacial ni selector de radio.
- Las zonas son cuadrículas métricas de 1 km (EPSG:3116), no localidades oficiales ni polígonos administrativos.
- Las direcciones manuales no se geocodifican todavía.
- La cámara se abre mediante el capturador del sistema tras pedir permiso a ParkSmart; no hay visor CameraX integrado.
- No hay recuperación de contraseña, verificación de correo, cierre remoto de sesiones ni un historial separado de auditoría de moderación.
- El clasificador es experimental, léxico y puede tener pocos ejemplos o sesgos. El corte de evaluación por UUID es estable, pero no reemplaza una cohorte externa, temporal o geográficamente independiente. Las métricas se ocultan hasta alcanzar los umbrales configurados.
- Solo cuentan para evaluar los reportes cuya sugerencia quedó guardada antes de la etiqueta humana final. Los reportes antiguos sin predicción previa no son casos de evaluación. La categoría inicial es una comparación práctica, no necesariamente una referencia experta independiente.
- No se normaliza el conteo por población ni se estima prevalencia/riesgo por localidad: faltan denominadores poblacionales y datos de cobertura confiables. Las cifras describen reportes recibidos, no la incidencia real.
- Las tendencias no ajustan estacionalidad ni población y no ejecutan pruebas formales de anomalías. El ranking por localidad mide volumen reportado, no riesgo ni prevalencia.
- No hay notificaciones push.
- El texto de error de algunos fallos HTTP puede ser genérico y el estado del GPS depende de que el dispositivo tenga una ubicación reciente.

## Errores encontrados y soluciones

- **La ruta contiene caracteres no ASCII:** AGP rechazó la carpeta que contenía `Programación`. Se trasladó el proyecto a una ruta sin tildes, por ejemplo `E:\Trabajos\Programacion\Ingenieria de Software\ParkSmart`.
- **`ApplicationExtensionImpl cannot be cast to BaseExtension`:** había incompatibilidad entre Kotlin Android heredado y AGP 9. Se dejó Kotlin integrado de AGP y se conservó `org.jetbrains.kotlin.plugin.compose`; no se debe volver a agregar `org.jetbrains.kotlin.android`.
- **`listOf` con demasiados argumentos y errores derivados en `DemoMapCanvas`:** las listas de puntos del dibujo quedaron anidadas incorrectamente. Cada camino se organizó como una lista de `Offset`, dentro de una lista exterior.
- **`Unresolved reference: repository`:** el repositorio se usaba en métodos del ViewModel, pero no estaba guardado como propiedad del constructor. Se corrigió a `private val repository` y se alinearon el tipo y el import con la implementación API.
- **`Java heap space`:** se configuró el heap de Gradle en 2 GB en `gradle.properties` (`-Xmx2048m`). Si vuelve a ocurrir, cierra procesos pesados y reinicia Gradle desde Android Studio.
- **`Function invocation 'error(...)' expected`:** una lambda `onFailure` usaba `error` sin declarar el parámetro. Se corrigió a `.onFailure { error -> ... }`.
- **`Unsupported escape sequence` en `build.gradle.kts`:** la URL ya no se escribe como literal fijo en el script. El proyecto lee `parksmart.apiBaseUrl` de `local.properties` y usa `http://10.0.2.2:8000/` si no existe esa propiedad. Para teléfono, agrega la IP LAN a `local.properties`.

- **HTTP 500 al agrupar la tendencia diaria en PostgreSQL:** SQLAlchemy compiló la zona horaria con parámetros distintos en `SELECT` y `GROUP BY`. Se reemplazó esa expresión por SQL PostgreSQL explícito con `AT TIME ZONE 'America/Bogota'`, conversión a fecha y agrupación por alias. La ruta volvió a responder con una serie de 30 días.
- **`Unable to establish loopback connection` en Gradle (también al verificar v0.6.1):** Gradle falla antes de compilar Kotlin, por lo que no confirma ni descarta errores del código Android. No quedó resuelto. Cierra Android Studio, termina procesos Gradle/JDK atascados, vuelve a abrir el proyecto y prueba Sync y `:app:compileDebugKotlin`; si persiste, revisa firewall/antivirus y permisos de loopback para Java/Gradle, y consulta el log de Gradle para identificar el proceso bloqueado.
- **Fotos giradas de lado:** la cámara y la galería podían proporcionar píxeles sin rotar y guardar el giro solo en EXIF. Android ahora aplica la transformación EXIF a los píxeles y genera un JPEG normalizado antes de subirlo; Administración aplica la misma corrección a imágenes anteriores.
- **Logo del launcher recortado:** el icono adaptativo mostraba el logo casi a tamaño completo, fuera de la zona segura del recorte del sistema. Se añadió un margen interior y un fondo blanco para que el logo completo quede visible.
- **`maxHeight` con receptor implícito en `LoginScreen`:** dentro del contenido de `Column`, Kotlin resolvía el receptor implícito incorrecto. Se captura `maxHeight` como `availableHeight` en `BoxWithConstraints` antes de crear la columna y se usa esa variable para calcular el espacio superior.

  ```properties
  parksmart.apiBaseUrl=http://192.168.1.20:8000/
  ```

  En Gradle Kotlin DSL, importa `java.util.Properties` y crea la instancia con `Properties()`; usar `java.util.Properties()` dio `Unresolved reference: util` en esta configuración.

## Próximos pasos

La hoja de ruta recomendada para las siguientes versiones es:

1. **v0.8 - Calidad analítica:** ampliar la evaluación independiente del clasificador con etiquetas humanas suficientes, comparar su desempeño contra una línea base humana y revisar resultados por localidad y periodo. Evaluar estacionalidad solo con datos históricos y cobertura confiables.
2. **v0.9 - Confianza y gestión:** verificación de correo y recuperación de contraseña; auditoría de moderación, reclamos, consentimiento versionado y retención/eliminación automática de fotos.
3. **v1.0 - Preparación para uso real:** desplegar API y base de datos en infraestructura administrada; HTTPS/TLS, secretos gestionados, límites antiabuso, copias de seguridad, monitoreo, CI/CD, pruebas de seguridad y publicación Android.

Antes de un uso público, deben definirse el responsable y canal de atención, registrar la aceptación de una versión de la política y automatizar la retención de fotos. La política visible de v0.5 es informativa y todavía no captura esa constancia.

## Referencias técnicas

- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
- [Permisos de ubicación Android](https://developer.android.com/develop/sensors-and-location/location/permissions)
- [Seguridad JWT y hashing en FastAPI](https://fastapi.tiangolo.com/tutorial/security/oauth2-jwt/)
- [Carga de archivos con FastAPI](https://fastapi.tiangolo.com/tutorial/request-files/)
- [Imagen Docker PostGIS](https://github.com/postgis/docker-postgis)
- [Localidades de Bogotá](https://bogota.gov.co/mi-ciudad/gobierno/parques-y-monumentos-de-bogota-y-sus-20-localidades-2024)
- [MapLibre Native Android](https://maplibre.org/maplibre-native/android/examples/getting-started/)
- [Política de uso de teselas OpenStreetMap](https://operations.osmfoundation.org/policies/tiles/)
- [Política de uso de Nominatim](https://operations.osmfoundation.org/policies/nominatim/)
