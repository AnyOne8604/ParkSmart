// Configuración Android por módulo. Los valores locales de URL permiten probar cada computador sin editar fuentes.
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Lee local.properties si existe; Android Studio lo genera localmente y Git lo mantiene fuera del repositorio.
val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use { load(it) }
}
// Dirección REST y teselas ajustables por instalación; el emulador usa su alias del host como valor alternativo.
val parksmartApiBaseUrl = localProperties.getProperty("parksmart.apiBaseUrl", "http://192.168.0.7:8000/")
val parksmartMapTileUrl = localProperties.getProperty("parksmart.mapTileUrl", "https://tile.openstreetmap.org/{z}/{x}/{y}.png")

// Identidad de paquete, SDKs, versión de entrega y constantes compiladas para Retrofit/MapLibre.
android {
    namespace = "com.parksmart.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.parksmart.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.7.0"
        buildConfigField("String", "API_BASE_URL", "\"$parksmartApiBaseUrl\"")
        buildConfigField("String", "MAP_TILE_URL", "\"$parksmartMapTileUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("org.maplibre.gl:android-sdk:11.8.0")
    implementation("androidx.core:core-ktx:1.17.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
