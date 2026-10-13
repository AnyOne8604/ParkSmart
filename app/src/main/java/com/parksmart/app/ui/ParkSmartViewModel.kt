package com.parksmart.app.ui

// Estado y operaciones de presentación de la app Android; conecta pantallas con la capa Retrofit sin lógica de UI.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parksmart.app.data.DashboardData
import com.parksmart.app.data.AdminReport
import com.parksmart.app.data.ClassificationMetrics
import com.parksmart.app.data.MapData
import com.parksmart.app.data.ParkSmartUser
import com.parksmart.app.data.ParkSmartApiRepository
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

// Estado observable único: permite que Compose redibuje solo con los datos, indicadores y mensajes actuales.
data class ParkSmartUiState(
    val user: ParkSmartUser? = null,
    val dashboard: DashboardData? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val mapData: MapData = MapData(),
    val mapLoading: Boolean = false,
    val mapError: String? = null,
    val adminReports: List<AdminReport> = emptyList(),
    val adminLoading: Boolean = false,
    val adminError: String? = null,
    val classificationMetrics: ClassificationMetrics? = null,
)

// ViewModel incorporado al flujo conectado con FastAPI en v0.2.0 y extendido al añadir mapas, moderación y analítica.
class ParkSmartViewModel(private val repository: ParkSmartApiRepository) : ViewModel() {
    private val _state = MutableStateFlow(ParkSmartUiState())
    val state = _state.asStateFlow()
    private var nearbyMapJob: Job? = null
    private var zoneStatsJob: Job? = null

    // Recupera la sesión persistida al crear el ViewModel y refresca los datos si el usuario ya había ingresado.
    init {
        repository.restoreUser()?.let { user ->
            _state.value = _state.value.copy(user = user)
            refreshDashboard()
        }
    }

    // Inicia sesión/registro mediante la misma rutina para compartir estado de carga y manejo de errores (v0.2.0).
    fun signIn(email: String, password: String) = authenticate { repository.signIn(email, password) }
    fun register(name: String, email: String, password: String) = authenticate { repository.register(name, email, password) }

    // Ejecuta una operación de autenticación en corrutina y publica el usuario o un mensaje apto para UI.
    private fun authenticate(action: suspend () -> ParkSmartUser) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            runCatching { action() }
                .onSuccess { user ->
                    _state.value = _state.value.copy(user = user, busy = false)
                    refreshDashboard()
                }
                .onFailure { error -> _state.value = _state.value.copy(busy = false, message = error.messageForUser()) }
        }
    }

    // Actualiza el panel personal con reportes e indicadores devueltos por el backend (analítica desde v0.6.0).
    fun refreshDashboard() {
        viewModelScope.launch {
            runCatching { repository.loadDashboard() }
                .onSuccess { _state.value = _state.value.copy(dashboard = it) }
                .onFailure { error -> _state.value = _state.value.copy(message = error.messageForUser()) }
        }
    }

    // Carga todos los reportes para Control, operación disponible solo para rol administrativo desde v0.5.0.
    fun loadAdminReports() {
        viewModelScope.launch {
            _state.value = _state.value.copy(adminLoading = true, adminError = null)
            runCatching { repository.loadAdminReports() }
                .onSuccess { reports ->
                    _state.value = _state.value.copy(adminReports = reports, adminLoading = false)
                    loadClassificationMetrics()
                }
                .onFailure { _state.value = _state.value.copy(adminLoading = false, adminError = it.messageForUser()) }
        }
    }

    // Obtiene calidad del clasificador y línea base humana para el panel de administración (v0.6.0-v0.6.1).
    private fun loadClassificationMetrics() {
        viewModelScope.launch {
            runCatching { repository.loadClassificationMetrics() }
                .onSuccess { _state.value = _state.value.copy(classificationMetrics = it) }
        }
    }

    // Pide una sugerencia asistida y actualiza ese reporte; la decisión final continúa siendo humana (v0.6.0).
    fun requestClassificationSuggestion(reportId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(adminLoading = true, adminError = null)
            runCatching { repository.suggestReportCategory(reportId) }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        adminReports = _state.value.adminReports.map { if (it.id == updated.id) updated else it },
                        adminLoading = false,
                    )
                    loadClassificationMetrics()
                }
                .onFailure { _state.value = _state.value.copy(adminLoading = false, adminError = it.messageForUser()) }
        }
    }

    // Guarda la categoría revisada por una persona y vuelve a cargar reportes/métricas (v0.6.0).
    fun reviewReportCategory(reportId: String, category: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(adminLoading = true, adminError = null)
            runCatching { repository.reviewReportCategory(reportId, category) }
                .onSuccess {
                    refreshDashboard()
                    loadAdminReports()
                }
                .onFailure { _state.value = _state.value.copy(adminLoading = false, adminError = it.messageForUser()) }
        }
    }

    // Aprueba o niega un reporte desde Control y sincroniza el inicio después de la decisión (v0.5.0).
    fun moderateReport(reportId: String, status: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(adminLoading = true, adminError = null)
            runCatching { repository.decideReport(reportId, status) }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        adminReports = _state.value.adminReports.map { if (it.id == updated.id) updated else it },
                        adminLoading = false,
                    )
                    refreshDashboard()
                }
                .onFailure { _state.value = _state.value.copy(adminLoading = false, adminError = it.messageForUser()) }
        }
    }

    // Recupera la foto protegida del reporte seleccionado por el administrador.
    suspend fun loadAdminPhoto(reportId: String) = runCatching { repository.loadAdminPhoto(reportId) }.getOrNull()

    // Consulta marcadores cercanos y estadísticas PostGIS con filtros; cancela búsquedas anteriores para evitar carreras (v0.4.0).
    fun refreshMap(latitude: Double, longitude: Double, category: String?, status: String?, sinceDays: Int?, refreshZones: Boolean = true) {
        nearbyMapJob?.cancel()
        nearbyMapJob = viewModelScope.launch {
            delay(250)
            _state.value = _state.value.copy(mapLoading = true, mapError = null)
            runCatching { repository.loadNearbyMapReports(latitude, longitude, category, status, sinceDays) }
                .onSuccess { reports -> _state.value = _state.value.copy(mapData = _state.value.mapData.copy(reports = reports), mapLoading = false) }
                .onFailure { _state.value = _state.value.copy(mapLoading = false, mapError = it.messageForUser()) }
        }
        if (refreshZones) {
            zoneStatsJob?.cancel()
            zoneStatsJob = viewModelScope.launch {
                delay(250)
                runCatching { repository.loadZoneStatistics(category, status, sinceDays) }
                    .onSuccess { stats ->
                        _state.value = _state.value.copy(
                            mapData = _state.value.mapData.copy(zones = stats.zones, totalReports = stats.totalReports),
                            mapError = null,
                        )
                    }
                    .onFailure { _state.value = _state.value.copy(mapError = it.messageForUser()) }
            }
        }
    }

    // Valida datos requeridos y envía el reporte; ubicación GPS opcional y localidad/dirección manual existen desde v0.3.0.
    fun createReport(
        category: String,
        description: String,
        latitude: Double?,
        longitude: Double?,
        locality: String,
        address: String,
        photo: File?,
    ) {
        if (photo == null || locality.isBlank() || address.isBlank()) {
            _state.value = _state.value.copy(message = "Agrega una foto, selecciona una localidad y escribe la dirección exacta")
            return
        }
        if ((latitude == null) != (longitude == null)) {
            _state.value = _state.value.copy(message = "La ubicación GPS está incompleta; vuelve a obtenerla o continúa solo con la dirección")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            runCatching { repository.sendReport(category, description, latitude, longitude, locality, address, photo) }
                .onSuccess {
                    _state.value = _state.value.copy(busy = false, message = "Reporte enviado correctamente")
                    refreshDashboard()
                }
                .onFailure {
                    _state.value = _state.value.copy(busy = false, message = it.messageForUser())
                }
        }
    }

    // Descarta mensajes transitorios mostrados por formularios y operaciones.
    fun clearMessage() { _state.value = _state.value.copy(message = null) }
    // Cierra la sesión local y reinicia los datos observables de la aplicación.
    fun signOut() { repository.signOut(); _state.value = ParkSmartUiState() }

    // Traduce errores de HTTP, conectividad y casos inesperados a texto que la interfaz puede presentar en español.
    private fun Throwable.messageForUser(): String = when (this) {
        is HttpException -> runCatching { JSONObject(response()?.errorBody()?.string()).optString("detail") }
            .getOrNull().orEmpty().ifBlank { "La API respondió con un error (${code()})." }
        is IOException -> "No fue posible conectar con ParkSmart API. Verifica que el backend esté iniciado."
        else -> message ?: "Ocurrió un error inesperado."
    }
}
