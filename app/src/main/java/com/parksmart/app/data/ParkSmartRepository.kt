package com.parksmart.app.data

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
    val location: String,
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
