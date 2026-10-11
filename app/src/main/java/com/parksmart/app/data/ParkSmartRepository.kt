package com.parksmart.app.data

val BOGOTA_LOCALITIES = listOf(
    "Usaquén", "Chapinero", "Santa Fe", "San Cristóbal", "Usme", "Tunjuelito", "Bosa",
    "Kennedy", "Fontibón", "Engativá", "Suba", "Barrios Unidos", "Teusaquillo",
    "Los Mártires", "Antonio Nariño", "Puente Aranda", "La Candelaria",
    "Rafael Uribe Uribe", "Ciudad Bolívar", "Sumapaz",
)

data class ParkSmartUser(
    val fullName: String,
    val role: String,
    val initials: String,
)

data class CriticalZone(
    val name: String,
    val reportCount: Int,
    val priority: ZonePriority,
)

enum class ZonePriority { HIGH, MEDIUM, LOW }

data class ReportSummary(
    val title: String,
    val locality: String,
    val address: String,
    val dateLabel: String,
    val status: ReportStatus,
)

enum class ReportStatus { IN_REVIEW, RECEIVED, CLOSED }

data class DashboardData(
    val user: ParkSmartUser,
    val zones: List<CriticalZone>,
    val reports: List<ReportSummary>,
    val totalReports: Int,
)
