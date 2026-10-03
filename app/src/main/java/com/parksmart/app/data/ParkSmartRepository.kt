package com.parksmart.app.data

data class DemoUser(
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
    val location: String,
    val dateLabel: String,
    val status: ReportStatus,
)

enum class ReportStatus { IN_REVIEW, RECEIVED, CLOSED }

data class DashboardData(
    val user: DemoUser,
    val zones: List<CriticalZone>,
    val reports: List<ReportSummary>,
    val totalReportsToday: Int,
)

interface ParkSmartRepository {
    fun signIn(email: String, password: String): DemoUser
    fun loadDashboard(): DashboardData
}

class DemoParkSmartRepository : ParkSmartRepository {
    override fun signIn(email: String, password: String) = DemoUser("Carlos Ramírez", "Ciudadano", "CR")

    override fun loadDashboard() = DashboardData(
        user = DemoUser("Carlos Ramírez", "Ciudadano", "CR"),
        totalReportsToday = 27,
        zones = listOf(
            CriticalZone("Chapinero", 12, ZonePriority.HIGH),
            CriticalZone("Centro", 9, ZonePriority.HIGH),
            CriticalZone("Usaquén", 6, ZonePriority.MEDIUM),
        ),
        reports = listOf(
            ReportSummary("Automóvil en zona prohibida", "Cra. 7 #45-10, Chapinero", "Hoy, 9:42 a. m.", ReportStatus.IN_REVIEW),
            ReportSummary("Motocicleta sobre andén", "Cl. 53 #13-28, Chapinero", "Ayer, 5:18 p. m.", ReportStatus.RECEIVED),
            ReportSummary("Vehículo bloqueando acceso", "Cra. 5 #18-42, Centro", "28 sep. 2026", ReportStatus.CLOSED),
        ),
    )
}
