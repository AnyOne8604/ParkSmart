package com.parksmart.app.data

import android.content.Context
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
import com.google.gson.annotations.SerializedName
import java.io.File

data class AuthRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String)
data class UserDto(val id: String, val name: String, val email: String)
data class AuthResponse(@SerializedName("access_token") val accessToken: String, val user: UserDto)
data class ApiReport(
    val id: String,
    val category: String,
    val description: String,
    @SerializedName("photo_url") val photoUrl: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
)
data class ApiDashboard(@SerializedName("total_reports") val totalReports: Int, val reports: List<ApiReport>)

interface ParkSmartApi {
    @POST("api/v1/auth/register") suspend fun register(@Body body: RegisterRequest): AuthResponse
    @POST("api/v1/auth/login") suspend fun login(@Body body: AuthRequest): AuthResponse
    @GET("api/v1/dashboard") suspend fun dashboard(): ApiDashboard

    @Multipart
    @POST("api/v1/reports")
    suspend fun createReport(
        @Part("category") category: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part("latitude") latitude: okhttp3.RequestBody,
        @Part("longitude") longitude: okhttp3.RequestBody,
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
        val reports = response.reports.map { report ->
            ReportSummary(report.category, "${"%.5f".format(report.latitude)}, ${"%.5f".format(report.longitude)}", report.createdAt, ReportStatus.RECEIVED)
        }
        return DashboardData(currentUser(), emptyList(), reports, response.totalReports)
    }

    suspend fun sendReport(category: String, description: String, latitude: Double, longitude: Double, photo: File) {
        val multipart = MultipartBody.Part.createFormData("photo", photo.name, photo.asRequestBody("image/jpeg".toMediaType()))
        api.createReport(
            category.toRequestBody("text/plain".toMediaType()),
            description.toRequestBody("text/plain".toMediaType()),
            latitude.toString().toRequestBody("text/plain".toMediaType()),
            longitude.toString().toRequestBody("text/plain".toMediaType()),
            multipart,
        )
    }

    fun signOut() { preferences.edit().remove("token").remove("name").remove("email").apply() }
    fun restoreUser(): ParkSmartUser? = if (preferences.getString("token", null) == null) null else currentUser()

    private fun AuthResponse.saveSession(): ParkSmartUser {
        preferences.edit().putString("token", accessToken).putString("name", user.name).putString("email", user.email).apply()
        return currentUser()
    }

    private fun currentUser(): ParkSmartUser {
        val name = preferences.getString("name", "Usuario ParkSmart") ?: "Usuario ParkSmart"
        val initials = name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
        return ParkSmartUser(name, "Ciudadano", initials.ifBlank { "PS" })
    }
}
