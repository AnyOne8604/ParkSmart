package com.parksmart.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.parksmart.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.ResponseBody
import com.google.gson.annotations.SerializedName
import com.parksmart.app.data.ReportStatus
import java.io.File
import java.io.ByteArrayInputStream

data class AuthRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String)
data class UserDto(val id: String, val name: String, val email: String, val role: String = "citizen")
data class AuthResponse(@SerializedName("access_token") val accessToken: String, val user: UserDto)
data class ApiReport(
    val id: String,
    val category: String,
    val description: String,
    @SerializedName("photo_url") val photoUrl: String,
    val latitude: Double?,
    val longitude: Double?,
    val locality: String,
    val address: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
)
data class ApiDashboard(@SerializedName("total_reports") val totalReports: Int, val reports: List<ApiReport>)
data class ApiLocalityCount(val locality: String, @SerializedName("report_count") val reportCount: Int, val level: String)
data class ApiDailyTrend(@SerializedName("date") val date: String, @SerializedName("report_count") val reportCount: Int)
data class ApiDimensionCount(val name: String, @SerializedName("report_count") val reportCount: Int)
data class ApiPeriodComparison(
    @SerializedName("current_count") val currentCount: Int,
    @SerializedName("previous_count") val previousCount: Int,
    @SerializedName("change_count") val changeCount: Int,
    @SerializedName("change_percent") val changePercent: Double?,
)
data class ApiAnalytics(
    val today: String,
    @SerializedName("total_reports_today") val totalReportsToday: Int,
    @SerializedName("total_reports_30_days") val totalReports30Days: Int,
    @SerializedName("localities_today") val localitiesToday: List<ApiLocalityCount>,
    @SerializedName("daily_trend") val dailyTrend: List<ApiDailyTrend>,
    @SerializedName("category_counts") val categoryCounts: List<ApiDimensionCount>,
    @SerializedName("status_counts") val statusCounts: List<ApiDimensionCount>,
    @SerializedName("comparison_7_days") val comparison7Days: ApiPeriodComparison,
    @SerializedName("comparison_30_days") val comparison30Days: ApiPeriodComparison,
)
data class ApiClassMetrics(
    val samples: Int,
    val precision: Double?,
    val recall: Double?,
    val f1: Double?,
)
data class ApiClassificationMetrics(
    @SerializedName("reviewed_count") val reviewedCount: Int,
    @SerializedName("training_count") val trainingCount: Int,
    @SerializedName("validation_samples") val validationSamples: Int,
    val ready: Boolean,
    @SerializedName("model_ready") val modelReady: Boolean,
    @SerializedName("minimum_training_samples") val minimumTrainingSamples: Int,
    @SerializedName("minimum_validation_samples") val minimumValidationSamples: Int,
    @SerializedName("minimum_validation_per_class") val minimumValidationPerClass: Int,
    @SerializedName("classifier_version") val classifierVersion: String,
    val accuracy: Double?,
    @SerializedName("macro_precision") val macroPrecision: Double?,
    @SerializedName("macro_recall") val macroRecall: Double?,
    @SerializedName("macro_f1") val macroF1: Double?,
    @SerializedName("baseline_accuracy") val baselineAccuracy: Double?,
    @SerializedName("baseline_macro_f1") val baselineMacroF1: Double?,
    @SerializedName("per_class") val perClass: Map<String, ApiClassMetrics>,
)
data class ApiNearbyReport(
    val id: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val locality: String,
    val address: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
)
data class ApiZone(
    @SerializedName("zone_id") val zoneId: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("report_count") val reportCount: Int,
    @SerializedName("automobile_count") val automobileCount: Int,
    @SerializedName("motorcycle_count") val motorcycleCount: Int,
    @SerializedName("other_count") val otherCount: Int,
    @SerializedName("open_count") val openCount: Int,
)
data class ApiZoneStatistics(
    @SerializedName("cell_size_m") val cellSizeM: Int,
    @SerializedName("total_reports") val totalReports: Int,
    val zones: List<ApiZone>,
)
data class ApiAdminReport(
    val id: String,
    val category: String,
    val description: String,
    @SerializedName("photo_url") val photoUrl: String,
    val locality: String,
    val address: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_email") val userEmail: String,
    @SerializedName("moderated_at") val moderatedAt: String?,
    @SerializedName("reviewed_category") val reviewedCategory: String? = null,
    @SerializedName("suggested_category") val suggestedCategory: String? = null,
    @SerializedName("suggestion_confidence") val suggestionConfidence: Double? = null,
    @SerializedName("classifier_version") val classifierVersion: String? = null,
)
data class ReportDecisionRequest(val status: String)
data class CategoryReviewRequest(val category: String)

interface ParkSmartApi {
    @POST("api/v1/auth/register") suspend fun register(@Body body: RegisterRequest): AuthResponse
    @POST("api/v1/auth/login") suspend fun login(@Body body: AuthRequest): AuthResponse
    @GET("api/v1/dashboard") suspend fun dashboard(): ApiDashboard
    @GET("api/v1/analytics") suspend fun analytics(): ApiAnalytics

    @GET("api/v1/admin/reports") suspend fun allReports(): List<ApiAdminReport>
    @GET("api/v1/admin/classification/metrics") suspend fun classificationMetrics(): ApiClassificationMetrics
    @POST("api/v1/admin/reports/{reportId}/classification-suggestion")
    suspend fun suggestReportCategory(@Path("reportId") reportId: String): ApiAdminReport
    @POST("api/v1/admin/reports/{reportId}/category-review")
    suspend fun reviewReportCategory(@Path("reportId") reportId: String, @Body review: CategoryReviewRequest): ApiAdminReport
    @PATCH("api/v1/admin/reports/{reportId}/decision")
    suspend fun decideReport(@Path("reportId") reportId: String, @Body decision: ReportDecisionRequest): ApiAdminReport
    @GET("api/v1/admin/reports/{reportId}/photo") suspend fun adminReportPhoto(@Path("reportId") reportId: String): ResponseBody

    @GET("api/v1/reports/nearby")
    suspend fun nearbyReports(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius_m") radiusM: Int,
        @Query("category") category: String?,
        @Query("status") status: String?,
        @Query("since_days") sinceDays: Int?,
    ): List<ApiNearbyReport>

    @GET("api/v1/reports/zones")
    suspend fun zoneStatistics(
        @Query("category") category: String?,
        @Query("status") status: String?,
        @Query("since_days") sinceDays: Int?,
    ): ApiZoneStatistics

    @Multipart
    @POST("api/v1/reports")
    suspend fun createReport(
        @Part("category") category: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part("latitude") latitude: okhttp3.RequestBody?,
        @Part("longitude") longitude: okhttp3.RequestBody?,
        @Part("locality") locality: okhttp3.RequestBody,
        @Part("address") address: okhttp3.RequestBody,
        @Part photo: MultipartBody.Part,
    ): ApiReport
}

class ParkSmartApiRepository(context: Context) {
    private val preferences = context.getSharedPreferences("parksmart_session", Context.MODE_PRIVATE)
    private val tokenInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder().apply {
            preferences.getString("token", null)?.let { header("Authorization", "Bearer $it") }
        }.build()
        chain.proceed(request)
    }
    private val api: ParkSmartApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().addInterceptor(tokenInterceptor).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ParkSmartApi::class.java)

    suspend fun signIn(email: String, password: String): ParkSmartUser = api.login(AuthRequest(email.trim(), password)).saveSession()

    suspend fun register(name: String, email: String, password: String): ParkSmartUser =
        api.register(RegisterRequest(name.trim(), email.trim(), password)).saveSession()

    suspend fun loadDashboard(): DashboardData {
        val response = api.dashboard()
        val analytics = api.analytics().toReportAnalytics()
        val reports = response.reports.map { report ->
            ReportSummary(report.category, report.locality, report.address, report.createdAt, report.status.toReportStatus())
        }
        return DashboardData(currentUser(), analytics.localitiesToday, reports, response.totalReports, analytics)
    }

    suspend fun loadAdminReports() = api.allReports().map { it.toAdminReport() }

    suspend fun loadClassificationMetrics() = api.classificationMetrics().toMetrics()

    suspend fun suggestReportCategory(reportId: String) = api.suggestReportCategory(reportId).toAdminReport()

    suspend fun reviewReportCategory(reportId: String, category: String) =
        api.reviewReportCategory(reportId, CategoryReviewRequest(category)).toAdminReport()

    suspend fun decideReport(reportId: String, status: String) =
        api.decideReport(reportId, ReportDecisionRequest(status)).toAdminReport()

    suspend fun loadAdminPhoto(reportId: String): Bitmap? {
        val bytes = api.adminReportPhoto(reportId).bytes()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val orientation = ByteArrayInputStream(bytes).use(::readPhotoOrientation)
        return bitmap.withExifOrientation(orientation)
    }

    suspend fun loadNearbyMapReports(latitude: Double, longitude: Double, category: String?, status: String?, sinceDays: Int?): List<MapReport> =
        api.nearbyReports(latitude, longitude, 25000, category, status, sinceDays).map {
            MapReport(it.id, it.category, it.latitude, it.longitude, it.locality, it.address, it.status, it.createdAt)
        }

    suspend fun loadZoneStatistics(category: String?, status: String?, sinceDays: Int?): MapData {
        val stats = api.zoneStatistics(category, status, sinceDays)
        return MapData(
            reports = emptyList(),
            zones = stats.zones.map { MapZone(it.zoneId, it.latitude, it.longitude, it.reportCount, it.automobileCount, it.motorcycleCount, it.otherCount, it.openCount) },
            totalReports = stats.totalReports,
        )
    }

    suspend fun sendReport(
        category: String,
        description: String,
        latitude: Double?,
        longitude: Double?,
        locality: String,
        address: String,
        photo: File,
    ) {
        val multipart = MultipartBody.Part.createFormData("photo", photo.name, photo.asRequestBody("image/jpeg".toMediaType()))
        api.createReport(
            category.toRequestBody("text/plain".toMediaType()),
            description.toRequestBody("text/plain".toMediaType()),
            latitude?.toString()?.toRequestBody("text/plain".toMediaType()),
            longitude?.toString()?.toRequestBody("text/plain".toMediaType()),
            locality.toRequestBody("text/plain".toMediaType()),
            address.toRequestBody("text/plain".toMediaType()),
            multipart,
        )
    }

    fun signOut() { preferences.edit().remove("token").remove("name").remove("email").remove("role").apply() }
    fun restoreUser(): ParkSmartUser? = if (preferences.getString("token", null) == null) null else currentUser()

    private fun AuthResponse.saveSession(): ParkSmartUser {
        preferences.edit().putString("token", accessToken).putString("name", user.name).putString("email", user.email).putString("role", user.role).apply()
        return currentUser()
    }

    private fun currentUser(): ParkSmartUser {
        val name = preferences.getString("name", "Usuario ParkSmart") ?: "Usuario ParkSmart"
        val email = preferences.getString("email", "") ?: ""
        val initials = name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
        val role = preferences.getString("role", "citizen") ?: "citizen"
        return ParkSmartUser(name, email, role, initials.ifBlank { "PS" })
    }

    private fun ApiAdminReport.toAdminReport() = com.parksmart.app.data.AdminReport(
        id = id,
        category = category,
        description = description,
        locality = locality,
        address = address,
        status = status,
        createdAt = createdAt,
        userName = userName,
        userEmail = userEmail,
        moderatedAt = moderatedAt,
        reviewedCategory = reviewedCategory,
        suggestedCategory = suggestedCategory,
        suggestionConfidence = suggestionConfidence,
        classifierVersion = classifierVersion,
    )

    private fun ApiAnalytics.toReportAnalytics() = ReportAnalytics(
        today = today,
        totalReportsToday = totalReportsToday,
        totalReports30Days = totalReports30Days,
        localitiesToday = localitiesToday.map { count ->
            val priority = when (count.level) {
                "Alta" -> ZonePriority.HIGH
                "Media" -> ZonePriority.MEDIUM
                else -> ZonePriority.LOW
            }
            CriticalZone(count.locality, count.reportCount, priority)
        },
        dailyTrend = dailyTrend.map { DailyReportCount(it.date, it.reportCount) },
        categoryCounts = categoryCounts.map { ReportDimensionCount(it.name, it.reportCount) },
        statusCounts = statusCounts.map { ReportDimensionCount(it.name, it.reportCount) },
        comparison7Days = comparison7Days.toPeriodComparison(),
        comparison30Days = comparison30Days.toPeriodComparison(),
    )

    private fun ApiPeriodComparison.toPeriodComparison() = PeriodComparison(currentCount, previousCount, changeCount, changePercent)

    private fun ApiClassificationMetrics.toMetrics() = ClassificationMetrics(
        reviewedCount = reviewedCount,
        trainingCount = trainingCount,
        validationSamples = validationSamples,
        ready = ready,
        modelReady = modelReady,
        minimumTrainingSamples = minimumTrainingSamples,
        minimumValidationSamples = minimumValidationSamples,
        minimumValidationPerClass = minimumValidationPerClass,
        classifierVersion = classifierVersion,
        accuracy = accuracy,
        macroPrecision = macroPrecision,
        macroRecall = macroRecall,
        macroF1 = macroF1,
        baselineAccuracy = baselineAccuracy,
        baselineMacroF1 = baselineMacroF1,
        perClass = perClass.mapValues { (_, value) -> ClassQualityMetrics(value.samples, value.precision, value.recall, value.f1) },
    )

    private fun String.toReportStatus() = when (this) {
        "in_review" -> ReportStatus.IN_REVIEW
        "closed" -> ReportStatus.CLOSED
        "approved" -> ReportStatus.APPROVED
        "rejected" -> ReportStatus.REJECTED
        else -> ReportStatus.RECEIVED
    }
}
