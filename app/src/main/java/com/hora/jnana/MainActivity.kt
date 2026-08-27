package com.hora.jnana

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hora.jnana.api.AuthService
import com.hora.jnana.api.HoraApiService
import com.hora.jnana.data.AuthRepository
import com.hora.jnana.repository.HoraRepository
import com.hora.jnana.security.DeviceUuidProvider
import com.hora.jnana.ui.login.LoginViewModel
import com.hora.jnana.ui.screens.*
import com.hora.jnana.workers.HoraUpdateWorker
import com.hora.jnana.ui.theme.HoraJnanaTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleBackgroundUpdates()
        
        // Trigger an immediate widget update when app starts to ensure fresh data
        val dataStoreManager = DataStoreManager(this)
        lifecycleScope.launch {
            com.hora.jnana.utils.WidgetUtils.updateAllWidgets(this@MainActivity)
        }

        setContent {
            val currentTheme by dataStoreManager.themeFlow.collectAsState(initial = "green")
            val currentThemeMode by dataStoreManager.themeModeFlow.collectAsState(initial = "light")
            val customThemeColorHex by dataStoreManager.customThemeColorFlow.collectAsState(initial = null)

            val customColor = remember(customThemeColorHex) {
                customThemeColorHex?.let {
                    try {
                        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(it))
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            HoraJnanaTheme(
                themeName = currentTheme,
                themeMode = currentThemeMode,
                customColor = customColor
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(this)
                }
            }
        }
    }

    private fun scheduleBackgroundUpdates() {
        val workRequest = PeriodicWorkRequestBuilder<HoraUpdateWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "HoraUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

@Composable
fun AppNavigation(activity: MainActivity) {
    val navController = rememberNavController()
    val dataStoreManager = remember { DataStoreManager(activity) }
    val authRepository = remember { AuthRepository(activity) }
    val uuidProvider = remember { DeviceUuidProvider(activity) }
    
    val apiBase by dataStoreManager.apiBaseFlow.collectAsState(initial = BuildConfig.BASE_URL)

    val logging = remember { 
        okhttp3.logging.HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                okhttp3.logging.HttpLoggingInterceptor.Level.BODY
            } else {
                okhttp3.logging.HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    val commonClient = remember {
        okhttp3.OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    val authService = remember(apiBase) { AuthService.create(baseUrl = apiBase, client = commonClient) }
    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    val apiService = remember(apiBase, moshi) { 
        HoraApiService.create(
            authRepository = authRepository,
            onSessionExpired = {
                activity.lifecycleScope.launch {
                    authRepository.clearSessionToken()
                    authRepository.notifySessionExpired()
                }
            },
            moshi = moshi,
            baseUrl = apiBase
        ) 
    }
    val horaRepository = remember(apiService, moshi, dataStoreManager) { 
        HoraRepository(apiService, activity, moshi, dataStoreManager) 
    }
    
    val factory = remember(horaRepository, authService, moshi, dataStoreManager) { 
        ViewModelFactory(activity, authRepository, authService, horaRepository, moshi, dataStoreManager) 
    }

    val locationState by dataStoreManager.locationFlow.collectAsState(initial = null)
    val locationName by dataStoreManager.locationNameFlow.collectAsState(initial = null)
    val locationMode by dataStoreManager.locationModeFlow.collectAsState(initial = "gps")
    val langState by dataStoreManager.langFlow.collectAsState(initial = "en")
    val chartStyleState by dataStoreManager.chartStyleFlow.collectAsState(initial = "south")
    val sessionToken by authRepository.sessionToken.collectAsState(initial = null)
    val privacyAccepted by dataStoreManager.privacyAcceptedFlow.collectAsState(initial = false)

    var showPrivacyDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            activity.lifecycleScope.launch {
                dataStoreManager.savePrivacyAccepted(true)
            }
            activity.fetchLocation(dataStoreManager)
            if (navController.currentDestination?.route == "location_required" || navController.currentDestination?.route == "login") {
                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            navController.navigate("location_required")
        }
    }

    LaunchedEffect(locationMode, sessionToken, privacyAccepted) {
        if (locationMode == "gps" && !sessionToken.isNullOrEmpty() && privacyAccepted) {
            val hasFine = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (hasFine || hasCoarse) {
                activity.fetchLocation(dataStoreManager)
            } else {
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }

    LaunchedEffect(Unit) {
        authRepository.sessionExpiredEvent.collect {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDest = if (sessionToken.isNullOrEmpty()) "login" else {
        if (!privacyAccepted) "location_required" else "home"
    }

    if (showPrivacyDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { },
            title = { Text(if (langState == "kn") "ಗೌಪ್ಯತೆ ಮತ್ತು ಸ್ಥಳ" else "Privacy & Location") },
            text = { 
                Text(if (langState == "kn") 
                    "ನಿಖರವಾದ ಜ್ಯೋತಿಷ್ಯ ಡೇಟಾವನ್ನು ಒದಗಿಸಲು ಈ ಅಪ್ಲಿಕೇಶನ್‌ಗೆ ನಿಮ್ಮ ಸ್ಥಳದ ಅಗತ್ಯವಿದೆ. ನಿಮ್ಮ ಸ್ಥಳವು ನಿಮ್ಮ ಸಾಧನದಲ್ಲಿಯೇ ಇರುತ್ತದೆ ಮತ್ತು ಎಂದಿಗೂ ಹಂಚಿಕೊಳ್ಳಲಾಗುವುದಿಲ್ಲ." 
                    else "This app requires your location to provide accurate astrological data. Your location stays on your device and is never shared.") 
            },
            confirmButton = {
                TextButton(onClick = {
                    showPrivacyDialog = false
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) {
                    Text(if (langState == "kn") "ಒಪ್ಪಿಕೊಳ್ಳಿ" else "Accept")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPrivacyDialog = false
                    navController.navigate("location_required")
                }) {
                    Text(if (langState == "kn") "ತಿರಸ್ಕರಿಸಿ" else "Deny")
                }
            }
        )
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = loginViewModel,
                uuidProvider = uuidProvider,
                onLoginSuccess = {
                    if (!privacyAccepted) {
                        showPrivacyDialog = true
                    } else {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            route = "home?forceTutorial={forceTutorial}",
            arguments = listOf(
                navArgument("forceTutorial") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val forceTutorial = backStackEntry.arguments?.getBoolean("forceTutorial") ?: false
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                navController = navController,
                viewModel = homeViewModel,
                location = locationState,
                locationName = locationName,
                locationMode = locationMode,
                lang = langState,
                forceTutorial = forceTutorial
            )
        }
        composable("panchanga_detail") {
            PanchangaDetailScreen(
                navController = navController,
                repo = horaRepository,
                location = locationState,
                locationName = if (locationMode == "manual") locationName else null,
                lang = langState
            )
        }
        composable("hora_detail") {
            HoraDetailScreen(
                navController = navController,
                repo = horaRepository,
                location = locationState,
                locationName = if (locationMode == "manual") locationName else null,
                lang = langState
            )
        }
        composable("solar_celestial") {
            SolarCelestialScreen(
                navController = navController,
                repo = horaRepository,
                location = locationState,
                locationName = if (locationMode == "manual") locationName else null,
                lang = langState
            )
        }
        composable("muhurta") {
            MuhurtaScreen(
                navController = navController,
                repo = horaRepository,
                location = locationState,
                locationName = if (locationMode == "manual") locationName else null,
                lang = langState
            )
        }
        composable("transit_kundali") {
            val transitViewModel: TransitViewModel = viewModel(factory = factory)
            TransitKundaliScreen(
                navController = navController,
                viewModel = transitViewModel,
                location = locationState,
                locationName = if (locationMode == "manual") locationName else null,
                apiBase = apiBase,
                sessionToken = sessionToken,
                lang = langState,
                chartStyle = chartStyleState
            )
        }
        composable("birth_kundali") {
            val birthViewModel: BirthViewModel = viewModel(factory = factory)
            val savePath by dataStoreManager.savePathFlow.collectAsState(initial = null)
            BirthKundaliScreen(
                navController = navController,
                viewModel = birthViewModel,
                location = locationState,
                locationName = if (locationMode == "manual") locationName else null,
                apiBase = apiBase,
                sessionToken = sessionToken,
                lang = langState,
                savePath = savePath,
                chartStyle = chartStyleState
            )
        }
        composable("match_making") {
            val matchViewModel: MatchMakingViewModel = viewModel(factory = factory)
            val savePath by dataStoreManager.savePathFlow.collectAsState(initial = null)
            MatchMakingScreen(
                navController = navController,
                viewModel = matchViewModel,
                lang = langState,
                savePath = savePath
            )
        }
        composable("locations") {
            LocationsScreen(
                navController = navController,
                repo = horaRepository,
                dataStoreManager = dataStoreManager,
                lang = langState
            )
        }
        composable("settings") {
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
            val birthViewModel: BirthViewModel = viewModel(factory = factory)
            SettingsScreen(
                navController = navController,
                dataStoreManager = dataStoreManager,
                authRepository = authRepository,
                repo = horaRepository,
                homeViewModel = homeViewModel,
                birthViewModel = birthViewModel,
                location = locationState,
                locationName = locationName,
                locationMode = locationMode
            )
        }
        composable("licenses") {
            LicensesScreen(navController = navController, lang = langState)
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(navController = navController, lang = langState)
        }
        composable("location_required") {
            LocationPermissionScreen(
                navController = navController,
                dataStoreManager = dataStoreManager,
                lang = langState,
                onPermissionGranted = {
                    navController.navigate("home") {
                        popUpTo("location_required") { inclusive = true }
                    }
                }
            )
        }
    }
}

@SuppressLint("MissingPermission")
fun MainActivity.fetchLocation(dataStoreManager: DataStoreManager) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { location ->
            location?.let {
                lifecycleScope.launch {
                    dataStoreManager.saveLocation(it.latitude, it.longitude)
                }
            }
        }
}
