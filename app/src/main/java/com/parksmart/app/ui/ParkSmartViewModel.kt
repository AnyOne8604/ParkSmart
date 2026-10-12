package com.parksmart.app.ui

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

class ParkSmartViewModel(private val repository: ParkSmartApiRepository) : ViewModel() {
    private val _state = MutableStateFlow(ParkSmartUiState())
    val state = _state.asStateFlow()
    private var nearbyMapJob: Job? = null
    private var zoneStatsJob: Job? = null

    init {
        repository.restoreUser()?.let { user ->
            _state.value = _state.value.copy(user = user)
            refreshDashboard()
        }
    }

    fun signIn(email: String, password: String) = authenticate { repository.signIn(email, password) }
    fun register(name: String, email: String, password: String) = authenticate { repository.register(name, email, password) }

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

    fun refreshDashboard() {
        viewModelScope.launch {
            runCatching { repository.loadDashboard() }
                .onSuccess { _state.value = _state.value.copy(dashboard = it) }
                .onFailure { error -> _state.value = _state.value.copy(message = error.messageForUser()) }
        }
    }

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

    private fun loadClassificationMetrics() {
        viewModelScope.launch {
            runCatching { repository.loadClassificationMetrics() }
                .onSuccess { _state.value = _state.value.copy(classificationMetrics = it) }
        }
    }

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

    suspend fun loadAdminPhoto(reportId: String) = runCatching { repository.loadAdminPhoto(reportId) }.getOrNull()

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

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
    fun signOut() { repository.signOut(); _state.value = ParkSmartUiState() }

    private fun Throwable.messageForUser(): String = when (this) {
        is HttpException -> runCatching { JSONObject(response()?.errorBody()?.string()).optString("detail") }
            .getOrNull().orEmpty().ifBlank { "La API respondió con un error (${code()})." }
        is IOException -> "No fue posible conectar con ParkSmart API. Verifica que el backend esté iniciado."
        else -> message ?: "Ocurrió un error inesperado."
    }
}
