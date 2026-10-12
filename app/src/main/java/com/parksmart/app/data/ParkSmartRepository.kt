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

data class AdminReport(
    val id: String,
    val category: String,
    val description: String,
    val locality: String,
    val address: String,
    val status: String,
    val createdAt: String,
    val userName: String,
    val userEmail: String,
    val moderatedAt: String?,
    val reviewedCategory: String? = null,
    val suggestedCategory: String? = null,
    val suggestionConfidence: Double? = null,
    val classifierVersion: String? = null,
)

data class CriticalZone(
    val name: String,
    val reportCount: Int,
    val priority: ZonePriority,
)

data class DailyReportCount(val date: String, val reportCount: Int)
data class ReportDimensionCount(val name: String, val reportCount: Int)
data class PeriodComparison(val current: Int, val previous: Int, val change: Int, val changePercent: Double?)
data class ReportAnalytics(
    val today: String = "",
    val totalReportsToday: Int = 0,
    val totalReports30Days: Int = 0,
    val localitiesToday: List<CriticalZone> = emptyList(),
    val dailyTrend: List<DailyReportCount> = emptyList(),
    val categoryCounts: List<ReportDimensionCount> = emptyList(),
    val statusCounts: List<ReportDimensionCount> = emptyList(),
    val comparison7Days: PeriodComparison = PeriodComparison(0, 0, 0, null),
    val comparison30Days: PeriodComparison = PeriodComparison(0, 0, 0, null),
)
data class ClassQualityMetrics(val samples: Int, val precision: Double?, val recall: Double?, val f1: Double?)
data class ClassificationMetrics(
    val reviewedCount: Int,
    val trainingCount: Int,
    val validationSamples: Int,
    val ready: Boolean,
    val modelReady: Boolean,
    val minimumTrainingSamples: Int,
    val minimumValidationSamples: Int,
    val minimumValidationPerClass: Int,
    val classifierVersion: String,
    val accuracy: Double?,
    val macroPrecision: Double?,
    val macroRecall: Double?,
    val macroF1: Double?,
    val baselineAccuracy: Double?,
    val baselineMacroF1: Double?,
    val perClass: Map<String, ClassQualityMetrics>,
)

enum class ZonePriority { HIGH, MEDIUM, LOW }

data class ReportSummary(
    val title: String,
    val locality: String,
    val address: String,
    val dateLabel: String,
    val status: ReportStatus,
)

enum class ReportStatus { IN_REVIEW, RECEIVED, CLOSED, APPROVED, REJECTED }

data class DashboardData(
    val user: ParkSmartUser,
    val zones: List<CriticalZone>,
    val reports: List<ReportSummary>,
    val totalReports: Int,
    val analytics: ReportAnalytics = ReportAnalytics(),
)

data class MapReport(
    val id: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val locality: String,
    val address: String,
    val status: String,
    val createdAt: String,
)

data class MapZone(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val reports: Int,
    val automobiles: Int,
    val motorcycles: Int,
    val other: Int,
    val open: Int,
)

data class MapData(
    val reports: List<MapReport> = emptyList(),
    val zones: List<MapZone> = emptyList(),
    val totalReports: Int = 0,
)
