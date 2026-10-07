package com.parksmart.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parksmart.app.data.DashboardData
import com.parksmart.app.data.ParkSmartUser
import com.parksmart.app.data.ParkSmartApiRepository
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParkSmartUiState(
    val user: ParkSmartUser? = null,
    val dashboard: DashboardData? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

class ParkSmartViewModel(private val repository: ParkSmartApiRepository) : ViewModel() {
    private val _state = MutableStateFlow(ParkSmartUiState())
    val state = _state.asStateFlow()

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

    fun createReport(category: String, description: String, latitude: Double?, longitude: Double?, photo: File?) {
        if (photo == null || latitude == null || longitude == null) {
            _state.value = _state.value.copy(message = "Selecciona una foto y obtén la ubicación para continuar")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            runCatching { repository.sendReport(category, description, latitude, longitude, photo) }
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
