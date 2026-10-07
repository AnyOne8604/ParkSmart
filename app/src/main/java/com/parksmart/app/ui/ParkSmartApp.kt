package com.parksmart.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parksmart.app.R
import com.parksmart.app.data.CriticalZone
import com.parksmart.app.data.DashboardData
import com.parksmart.app.data.ParkSmartApiRepository
import com.parksmart.app.data.ReportStatus
import com.parksmart.app.data.ReportSummary
import com.parksmart.app.data.ZonePriority
import com.parksmart.app.ui.theme.Canvas as AppCanvas
import com.parksmart.app.ui.theme.Danger
import com.parksmart.app.ui.theme.Ink
import com.parksmart.app.ui.theme.Line
import com.parksmart.app.ui.theme.MutedInk
import com.parksmart.app.ui.theme.ParkGreen
import com.parksmart.app.ui.theme.ParkGreenDark
import com.parksmart.app.ui.theme.ParkGreenLight
import com.parksmart.app.ui.theme.Warning
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.File
import java.io.FileOutputStream

private enum class AppRoute {
    LOGIN, HOME, REPORT, MAP, PROFILE, REPORTS, STATISTICS
}

private data class MainTab(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun ParkSmartApp() {
    val context = LocalContext.current
    val repository = remember { ParkSmartApiRepository(context.applicationContext) }
    val factory = remember(repository) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ParkSmartViewModel(repository) as T
        }
    }
    val model: ParkSmartViewModel = viewModel(factory = factory)
    val state by model.state.collectAsState()
    var routeName by rememberSaveable { mutableStateOf(AppRoute.LOGIN.name) }
    val route = AppRoute.valueOf(routeName)
    val dashboard = state.dashboard ?: DashboardData(state.user ?: com.parksmart.app.data.ParkSmartUser("Ciudadano", "Ciudadano", "PS"), emptyList(), emptyList(), 0)
    LaunchedEffect(state.user) {
        if (state.user != null) routeName = AppRoute.HOME.name
    }

    if (route == AppRoute.LOGIN) {
        LoginScreen(
            onLogin = model::signIn,
            onRegister = model::register,
            busy = state.busy,
            message = state.message,
        )
        return
    }

    val selectedTab = when (route) {
        AppRoute.REPORT -> AppRoute.REPORT
        AppRoute.MAP -> AppRoute.MAP
        AppRoute.PROFILE -> AppRoute.PROFILE
        else -> AppRoute.HOME
    }
    val tabs = listOf(
        MainTab(AppRoute.HOME, "Inicio", Icons.Filled.Home),
        MainTab(AppRoute.REPORT, "Reportar", Icons.Filled.CameraAlt),
        MainTab(AppRoute.MAP, "Mapa", Icons.Filled.Place),
        MainTab(AppRoute.PROFILE, "Perfil", Icons.Filled.Person),
    )

    Scaffold(
        containerColor = AppCanvas,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars,
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab.route,
                        onClick = { routeName = tab.route.name },
                        icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(21.dp)) },
                        label = { Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ParkGreen,
                            selectedTextColor = ParkGreen,
                            indicatorColor = ParkGreenLight,
                            unselectedIconColor = Color(0xFF89928D),
                            unselectedTextColor = Color(0xFF89928D),
                        ),
                    )
                }
            }
        },
    ) { insets ->
        when (route) {
            AppRoute.HOME -> HomeScreen(dashboard, insets, onNavigate = { routeName = it.name })
            AppRoute.REPORT -> ReportScreen(insets, state.busy, state.message, model::createReport)
            AppRoute.MAP -> MapScreen(dashboard, insets)
            AppRoute.PROFILE -> ProfileScreen(dashboard, insets, onSignOut = { model.signOut(); routeName = AppRoute.LOGIN.name })
            AppRoute.REPORTS -> ReportsScreen(dashboard, insets, onBack = { routeName = AppRoute.HOME.name })
            AppRoute.STATISTICS -> StatisticsScreen(dashboard, insets, onBack = { routeName = AppRoute.HOME.name })
            AppRoute.LOGIN -> Unit
        }
    }
}

@Composable
private fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    busy: Boolean,
    message: String?,
) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 28.dp)
            .padding(top = 20.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.parksmart_logo),
            contentDescription = "ParkSmart: reporta hoy, una Bogotá mejor mañana",
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(166.dp),
        )
        Text(
            text = "Juntos por una movilidad más inteligente",
            color = MutedInk,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(24.dp))
        if (registering) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Nombre completo") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ParkGreen) },
                shape = RoundedCornerShape(12.dp),
                colors = LoginFieldColors(),
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = ParkGreen) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            colors = LoginFieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = ParkGreen) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = MutedInk,
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            colors = LoginFieldColors(),
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { if (registering) onRegister(name, email, password) else onLogin(email, password) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ParkGreen),
        ) {
            Text(if (busy) "Conectando…" else if (registering) "Crear cuenta" else "Iniciar sesión", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = Danger, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(if (registering) "¿Ya tienes cuenta?" else "¿No tienes una cuenta?", color = MutedInk, fontSize = 13.sp)
            Text(if (registering) "  Inicia sesión" else "  Regístrate", modifier = Modifier.clickable { registering = !registering }, color = ParkGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LoginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ParkGreen,
    unfocusedBorderColor = Line,
    focusedLabelColor = ParkGreen,
    cursorColor = ParkGreen,
)

@Composable
private fun HomeScreen(data: DashboardData, insets: PaddingValues, onNavigate: (AppRoute) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(insets)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hola,", color = MutedInk, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(data.user.fullName, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Juntos por una mejor movilidad", color = MutedInk, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ParkGreen),
                contentAlignment = Alignment.Center,
            ) {
                Text(data.user.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onNavigate(AppRoute.REPORT) },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ParkGreen),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 18.dp, bottom = 18.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Reporta un mal parqueo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Ayuda a construir una Bogotá más ordenada.", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 17.sp)
                }
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Abrir reporte", tint = ParkGreen)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction("Mis reportes", Icons.AutoMirrored.Filled.Assignment, Modifier.weight(1f)) { onNavigate(AppRoute.REPORTS) }
            QuickAction("Mapa", Icons.Filled.LocationOn, Modifier.weight(1f)) { onNavigate(AppRoute.MAP) }
            QuickAction("Estadísticas", Icons.Filled.BarChart, Modifier.weight(1f)) { onNavigate(AppRoute.STATISTICS) }
        }
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Zonas críticas hoy", modifier = Modifier.weight(1f), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Ver todas", color = ParkGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onNavigate(AppRoute.STATISTICS) })
        }
        Spacer(Modifier.height(10.dp))
        if (data.zones.isEmpty()) {
            Text("Aún no hay datos suficientes para identificar zonas críticas.", modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp), color = MutedInk, fontSize = 12.sp)
        } else data.zones.forEachIndexed { index, zone ->
            ZoneRow(zone)
            if (index != data.zones.lastIndex) Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Actividad reciente", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (data.reports.isEmpty()) {
            Text("Todavía no has enviado reportes.", modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp), color = MutedInk, fontSize = 12.sp)
        } else ReportRow(data.reports.first(), compact = true)
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ParkGreen, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun ZoneRow(zone: CriticalZone) {
    val (accent, tag, tagBg) = when (zone.priority) {
        ZonePriority.HIGH -> Triple(Danger, "Alta", Color(0xFFFFE9E9))
        ZonePriority.MEDIUM -> Triple(Warning, "Media", Color(0xFFFFF1D9))
        ZonePriority.LOW -> Triple(ParkGreen, "Baja", ParkGreenLight)
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(zone.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${zone.reportCount} reportes", color = MutedInk, fontSize = 11.sp)
        }
        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(tagBg).padding(horizontal = 9.dp, vertical = 4.dp)) {
            Text(tag, color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReportScreen(
    insets: PaddingValues,
    busy: Boolean,
    message: String?,
    onSubmit: (String, String, Double?, Double?, File?) -> Unit,
) {
    val context = LocalContext.current
    var selectedPhoto by remember { mutableStateOf<File?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var category by rememberSaveable { mutableStateOf("Automóvil") }
    var description by rememberSaveable { mutableStateOf("") }
    var captureFile by remember { mutableStateOf<File?>(null) }

    fun decodePreview(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 1600) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    fun saveJpeg(uri: Uri) {
        runCatching {
            val bitmap = decodePreview(uri) ?: error("No se pudo abrir la imagen")
            val file = File(context.cacheDir, "parksmart-${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            preview = bitmap
            selectedPhoto = file
        }
    }

    fun requestCurrentLocation() {
        try {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                if (location != null) { latitude = location.latitude; longitude = location.longitude }
            }
        } catch (_: SecurityException) { }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) captureFile?.let { file ->
            selectedPhoto = file
            preview = decodePreview(Uri.fromFile(file))
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "parksmart-${System.currentTimeMillis()}.jpg")
            captureFile = file
            takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::saveJpeg) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.any { it }) requestCurrentLocation()
    }

    Column(modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        ScreenHeading("Nuevo reporte", "Captura de evidencia y ubicación")
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE5EEE9)).clickable {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            contentAlignment = Alignment.Center,
        ) {
            val image = preview
            if (image != null) Image(image.asImageBitmap(), contentDescription = "Foto seleccionada", modifier = Modifier.fillMaxSize())
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = ParkGreen, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("Evidencia fotográfica", color = Ink, fontWeight = FontWeight.SemiBold)
                Text("Elige galería o toma una foto", color = MutedInk, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val file = File(context.cacheDir, "parksmart-${System.currentTimeMillis()}.jpg")
                    captureFile = file
                    takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                } else cameraPermission.launch(Manifest.permission.CAMERA)
            }, modifier = Modifier.weight(1f)) { Text("Tomar foto") }
            OutlinedButton(onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Text("Elegir de galería") }
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel("Ubicación del reporte")
        OutlinedButton(onClick = {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fine || coarse) requestCurrentLocation()
            else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.MyLocation, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (latitude == null) "Obtener ubicación actual" else "Ubicación capturada")
        }
        latitude?.let { lat -> longitude?.let { lon -> Text("%.5f, %.5f".format(lat, lon), modifier = Modifier.padding(top = 6.dp), color = MutedInk, fontSize = 11.sp) } }
        Spacer(Modifier.height(16.dp))
        SectionLabel("Tipo de vehículo")
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf("Automóvil", "Motocicleta", "Otro").forEach { type ->
                VehicleChoice(type, if (type == "Otro") Icons.Filled.ReportProblem else Icons.Filled.DirectionsCar, Modifier.weight(1f), selected = category == type) { category = type }
            }
        }
        Spacer(Modifier.height(15.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Descripción (opcional)") }, minLines = 2, maxLines = 4, shape = RoundedCornerShape(12.dp))
        if (message != null) Text(message, modifier = Modifier.padding(top = 10.dp), color = if (message.contains("correctamente")) ParkGreen else Danger, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onSubmit(category, description, latitude, longitude, selectedPhoto) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = ParkGreen)) {
            Text(if (busy) "Enviando…" else "Enviar reporte")
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun VehicleChoice(label: String, icon: ImageVector, modifier: Modifier, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = modifier.height(78.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) ParkGreenLight else Color.White)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) ParkGreen else MutedInk, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = if (selected) ParkGreenDark else MutedInk, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun MapScreen(data: DashboardData, insets: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 18.dp)) {
        ScreenHeading("Mapa de reportes", "Bogotá, Colombia")
        Text("Vista esquemática. Los datos del mapa aún no están conectados.", modifier = Modifier.padding(bottom = 10.dp), color = MutedInk, fontSize = 11.sp)
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = ParkGreen, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("Buscar dirección o localidad", color = MutedInk, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
            DemoMapCanvas()
            MapLabel("Bogotá", Modifier.align(Alignment.Center).offset(y = 16.dp))
            MapLabel("Chapinero", Modifier.align(Alignment.TopEnd).padding(top = 76.dp, end = 22.dp))
            MapLabel("Teusaquillo", Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 78.dp))
            MapPin(Color(0xFFE44E4E), Modifier.align(Alignment.Center).offset(x = 22.dp, y = (-24).dp))
            MapPin(Color(0xFFF5A623), Modifier.align(Alignment.TopStart).padding(start = 74.dp, top = 116.dp))
            MapPin(Color(0xFF0E9B69), Modifier.align(Alignment.BottomEnd).padding(end = 70.dp, bottom = 138.dp))
            MapPin(Color(0xFFE44E4E), Modifier.align(Alignment.BottomStart).padding(start = 104.dp, bottom = 158.dp))
            IconButton(
                onClick = {},
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(42.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Centrar mapa", tint = ParkGreen)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(Danger, "Alta")
            LegendDot(Warning, "Media")
            LegendDot(ParkGreen, "Baja")
            Text("${data.totalReports} reportes", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DemoMapCanvas() {
    Canvas(Modifier.fillMaxSize().background(Color(0xFFE8F1E9))) {
        val road = Color(0xFFFDFEFC)
        val minor = Color(0xFFD5E2D8)
        val paths = listOf(
            listOf(Offset(size.width * 0.02f, size.height * 0.25f), Offset(size.width * 0.32f, size.height * 0.35f), Offset(size.width * 0.55f, size.height * 0.28f), Offset(size.width * 0.98f, size.height * 0.38f)),
            listOf(Offset(size.width * 0.08f, size.height * 0.68f), Offset(size.width * 0.42f, size.height * 0.58f), Offset(size.width * 0.70f, size.height * 0.72f), Offset(size.width * 1.02f, size.height * 0.61f)),
            listOf(Offset(size.width * 0.35f, -10f), Offset(size.width * 0.44f, size.height * 0.32f), Offset(size.width * 0.40f, size.height * 0.68f), Offset(size.width * 0.53f, size.height + 10f)),
        )
        paths.forEach { points ->
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                cubicTo(points[1].x, points[1].y, points[2].x, points[2].y, points[3].x, points[3].y)
            }
            drawPath(path, road, style = Stroke(width = 24.dp.toPx()))
            drawPath(path, minor, style = Stroke(width = 1.dp.toPx()))
        }
        for (i in 0..7) {
            val x = size.width * (0.10f + i * 0.12f)
            drawLine(minor, Offset(x, size.height * 0.05f), Offset(x - size.width * 0.07f, size.height * 0.95f), 1.dp.toPx())
        }
        for (i in 0..5) {
            val y = size.height * (0.08f + i * 0.17f)
            drawLine(minor, Offset(size.width * 0.04f, y), Offset(size.width * 0.96f, y + size.height * 0.04f), 1.dp.toPx())
        }
        drawCircle(Color(0xFFCEE6D6), radius = 36.dp.toPx(), center = Offset(size.width * 0.78f, size.height * 0.23f))
        drawCircle(Color(0xFFD7EBD9), radius = 24.dp.toPx(), center = Offset(size.width * 0.17f, size.height * 0.80f))
    }
}

@Composable
private fun MapPin(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(27.dp).shadow(3.dp, CircleShape).background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(9.dp).background(Color.White, CircleShape))
    }
}

@Composable
private fun MapLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.86f)).padding(horizontal = 7.dp, vertical = 4.dp), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(text, color = MutedInk, fontSize = 10.sp)
    }
}

@Composable
private fun ProfileScreen(data: DashboardData, insets: PaddingValues, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        ScreenHeading("Perfil", "Tu cuenta ParkSmart")
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(56.dp).background(ParkGreenLight, CircleShape), contentAlignment = Alignment.Center) {
                Text(data.user.initials, color = ParkGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(data.user.fullName, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(data.user.role, color = MutedInk, fontSize = 12.sp)
                Text("Cuenta ParkSmart", color = ParkGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(18.dp))
        Spacer(Modifier.height(16.dp))
        ProfileItem(Icons.Filled.AccountCircle, "Información personal")
        ProfileItem(Icons.Filled.NotificationsNone, "Notificaciones")
        ProfileItem(Icons.AutoMirrored.Filled.HelpOutline, "Ayuda y soporte")
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFF0D8D8)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileItem(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ParkGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedInk, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun ReportsScreen(data: DashboardData, insets: PaddingValues, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        BackHeading("Mis reportes", onBack)
        Spacer(Modifier.height(14.dp))
        if (data.reports.isEmpty()) Text("Aún no tienes reportes.", modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp), color = MutedInk, fontSize = 12.sp)
        data.reports.forEachIndexed { index, report ->
            ReportRow(report)
            if (index != data.reports.lastIndex) Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ReportRow(report: ReportSummary, compact: Boolean = false) {
    val statusColor = when (report.status) {
        ReportStatus.IN_REVIEW -> Warning
        ReportStatus.RECEIVED -> ParkGreen
        ReportStatus.CLOSED -> MutedInk
    }
    val statusLabel = when (report.status) {
        ReportStatus.IN_REVIEW -> "En revisión"
        ReportStatus.RECEIVED -> "Recibido"
        ReportStatus.CLOSED -> "Cerrado"
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color.White).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(if (compact) 36.dp else 42.dp).clip(RoundedCornerShape(10.dp)).background(ParkGreenLight), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = ParkGreen, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(report.title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(report.location, color = MutedInk, fontSize = 10.sp, maxLines = 1)
            if (!compact) Text(report.dateLabel, color = MutedInk, fontSize = 10.sp)
        }
        if (!compact) {
            Spacer(Modifier.width(6.dp))
            Text(statusLabel, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatisticsScreen(data: DashboardData, insets: PaddingValues, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        BackHeading("Estadísticas", onBack)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("Reportes totales", data.totalReports.toString(), Icons.AutoMirrored.Filled.ListAlt, Modifier.weight(1f))
            MetricTile("Zonas críticas", data.zones.size.toString(), Icons.Filled.LocationOn, Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text("Reportes por zona", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (data.zones.isEmpty()) Text("Aún no hay datos para mostrar por zona.", color = MutedInk, fontSize = 12.sp)
        data.zones.forEach { zone ->
            val fraction = zone.reportCount.toFloat() / (data.zones.maxOfOrNull { it.reportCount } ?: 1)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color.White).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(zone.name, modifier = Modifier.weight(1f), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${zone.reportCount}", color = ParkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(ParkGreenLight)) {
                    Box(Modifier.fillMaxWidth(fraction).height(7.dp).clip(CircleShape).background(ParkGreen))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun MetricTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(15.dp)) {
        Icon(icon, contentDescription = null, tint = ParkGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(12.dp))
        Text(value, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MutedInk, fontSize = 11.sp)
    }
}

@Composable
private fun ScreenHeading(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 17.dp)) {
        Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = MutedInk, fontSize = 12.sp)
    }
}

@Composable
private fun BackHeading(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Ink)
        }
        Spacer(Modifier.width(5.dp))
        Text(title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, modifier = Modifier.padding(bottom = 9.dp), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ParkGreen, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MutedInk, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}
