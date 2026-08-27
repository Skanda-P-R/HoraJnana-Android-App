package com.hora.jnana.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hora.jnana.DataStoreManager
import com.hora.jnana.data.AuthRepository
import kotlinx.coroutines.launch
import androidx.documentfile.provider.DocumentFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController, 
    dataStoreManager: DataStoreManager,
    authRepository: AuthRepository,
    repo: com.hora.jnana.repository.HoraRepository,
    homeViewModel: HomeViewModel,
    birthViewModel: BirthViewModel,
    location: Pair<Double, Double>?,
    locationName: String?,
    locationMode: String
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoggingOut by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val currentLang by dataStoreManager.langFlow.collectAsState(initial = "en")
    val currentTheme by dataStoreManager.themeFlow.collectAsState(initial = "green")
    val currentThemeMode by dataStoreManager.themeModeFlow.collectAsState(initial = "light")
    val currentChartStyle by dataStoreManager.chartStyleFlow.collectAsState(initial = "south")
    val currentAyanamsa by dataStoreManager.ayanamsaFlow.collectAsState(initial = "lahiri")
    val customColorHex by dataStoreManager.customThemeColorFlow.collectAsState(initial = null)
    val context = LocalContext.current

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            successMessage = null
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            isBackingUp = true
            birthViewModel.backupKundalis(it.toString()) { success, msg ->
                isBackingUp = false
                if (success) {
                    successMessage = if (currentLang == "kn") "ಬ್ಯಾಕಪ್ ಪೂರ್ಣಗೊಂಡಿದೆ" else msg ?: "Backup successful"
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (currentLang == "kn") "ಬ್ಯಾಕಪ್ ವಿಫಲವಾಗಿದೆ" else "Backup failed: $msg"
                        )
                    }
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            isRestoring = true
            birthViewModel.restoreKundalis(it.toString()) { success, msg ->
                isRestoring = false
                if (success) {
                    successMessage = if (currentLang == "kn") "ಮರುಸ್ಥಾಪನೆ ಪೂರ್ಣಗೊಂಡಿದೆ" else msg ?: "Restore successful"
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (currentLang == "kn") "ಮರುಸ್ಥಾಪನೆ ವಿಫಲವಾಗಿದೆ" else "Restore failed: $msg"
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentLang == "kn") "ಸೇಟಿಂಗ್ಸ್" else "Settings") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (navController.currentDestination?.route == "settings") {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val scrollState = rememberScrollState()
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text(
                    if (currentLang == "kn") "ಭಾಷೆ" else "Language", 
                    style = MaterialTheme.typography.titleMedium
                )
                val options = listOf("en" to "English", "kn" to "ಕನ್ನಡ")
                Column(Modifier.selectableGroup()) {
                    options.forEach { (code, label) ->
                        LanguageOption(
                            label = label,
                            selected = (code == currentLang),
                            onClick = { scope.launch { dataStoreManager.saveLang(code) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    if (currentLang == "kn") "ಥೀಮ್ ಮೋಡ್" else "Theme Mode",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val modeOptions = listOf(
                        "light" to if (currentLang == "kn") "ಲೈಟ್" else "Light",
                        "dark" to if (currentLang == "kn") "ಡಾರ್ಕ್" else "Dark",
                        "system" to if (currentLang == "kn") "ಸಿಸ್ಟಂ" else "System"
                    )
                    modeOptions.forEach { (mode, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                scope.launch { dataStoreManager.saveThemeMode(mode) }
                            }
                        ) {
                            ThemeModeCircle(
                                mode = mode,
                                isSelected = (mode == currentThemeMode)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    if (currentLang == "kn") "ಅಪ್ಲಿಕೇಶನ್ ಬಣ್ಣ" else "App Colour Palette",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Default Green
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(com.hora.jnana.ui.theme.Green40)
                            .border(
                                width = if (currentTheme == "green") 3.dp else 1.dp,
                                color = if (currentTheme == "green") Color.Red else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                scope.launch {
                                    dataStoreManager.saveTheme("green")
                                    dataStoreManager.saveCustomThemeColor(null)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentTheme == "green") {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Text(
                        text = if (currentLang == "kn") "ಡೀಫಾಲ್ಟ್‌ಗೆ ಮರುಹೊಂದಿಸಿ" else "Reset to Default",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            scope.launch {
                                dataStoreManager.saveTheme("green")
                                dataStoreManager.saveCustomThemeColor(null)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Wheel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val initialColor = remember(customColorHex) {
                        try {
                            customColorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Green
                        } catch (e: Exception) {
                            Color.Green
                        }
                    }
                    ColorWheel(
                        modifier = Modifier.size(180.dp),
                        selectedColor = initialColor,
                        onColorSelected = { color ->
                            scope.launch {
                                val hex = String.format("#%06X", (0xFFFFFF and color.toArgb()))
                                dataStoreManager.saveTheme("custom")
                                dataStoreManager.saveCustomThemeColor(hex)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    com.hora.jnana.utils.TranslationUtils.translate("Ayanamsa", currentLang),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                val ayanamsaOptions = listOf(
                    "lahiri" to com.hora.jnana.utils.TranslationUtils.translate("Lahiri", currentLang),
                    "raman" to com.hora.jnana.utils.TranslationUtils.translate("Raman", currentLang),
                    "krishnamurti" to com.hora.jnana.utils.TranslationUtils.translate("Krishnamurti", currentLang),
                    "fagan_bradley" to com.hora.jnana.utils.TranslationUtils.translate("Fagan Bradley", currentLang)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ayanamsaOptions.chunked(2).forEach { rowOptions ->
                        Row(
                            Modifier
                                .selectableGroup()
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { (value, label) ->
                                SelectableSquareButton(
                                    selected = (currentAyanamsa == value),
                                    onClick = { scope.launch { dataStoreManager.saveAyanamsa(value) } },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        fontWeight = if (currentAyanamsa == value) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    if (currentLang == "kn") "ಕುಂಡಲಿ ಚಾರ್ಟ್ ಶೈಲಿ" else "Kundali Chart Style",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    Modifier
                        .selectableGroup()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chartOptions = listOf(
                        "north" to if (currentLang == "kn") "ಉತ್ತರ" else "North",
                        "south" to if (currentLang == "kn") "ದಕ್ಷಿಣ" else "South",
                        "east" to if (currentLang == "kn") "ಪೂರ್ವ" else "East"
                    )
                    chartOptions.forEach { (style, label) ->
                        SelectableSquareButton(
                            selected = (currentChartStyle == style),
                            onClick = { scope.launch { dataStoreManager.saveChartStyle(style) } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (style) {
                                    "north" -> NorthIndianChartIcon(color = LocalContentColor.current)
                                    "south" -> SouthIndianChartIcon(color = LocalContentColor.current)
                                    "east" -> EastIndianChartIcon(color = LocalContentColor.current)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    if (currentLang == "kn") "ಸ್ಥಳ" else "Location", 
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { navController.navigate("locations") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentLang == "kn") "ಸ್ಥಳವನ್ನು ಬದಲಾಯಿಸಿ" else "Change Location")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    if (currentLang == "kn") "ಕುಂಡಲಿಗಳ ಬ್ಯಾಕಪ್ ಮತ್ತು ಮರುಸ್ಥಾಪನೆ" else "Backup & Restore Kundalis",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { backupLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        enabled = !isBackingUp && !isRestoring
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (currentLang == "kn") "ಬ್ಯಾಕಪ್" else "Backup")
                        }
                    }

                    OutlinedButton(
                        onClick = { restoreLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        enabled = !isBackingUp && !isRestoring
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (currentLang == "kn") "ಮರುಸ್ಥಾಪಿಸಿ" else "Restore")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(if (currentLang == "kn") "ನಮ್ಮ ಬಗ್ಗೆ" else "About", style = MaterialTheme.typography.titleMedium)
                Text("HoraJnana v1.0.1", style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.navigate("licenses") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (currentLang == "kn") "ಪರವಾನಗಿಗಳು" else "Licenses")
                    }
                    OutlinedButton(
                        onClick = { navController.navigate("privacy_policy") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (currentLang == "kn") "ಗೌಪ್ಯತೆ" else "Privacy")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val quantizedLocation = if (location == null) null else {
                                val lat = (location.first * 1000).toInt() / 1000.0
                                val lon = (location.second * 1000).toInt() / 1000.0
                                lat to lon
                            }
                            if (locationMode == "gps" && quantizedLocation != null) {
                                homeViewModel.refresh(context, quantizedLocation.first, quantizedLocation.second, null, force = true)
                            } else if (locationMode == "manual") {
                                homeViewModel.refresh(context, null, null, locationName, force = true)
                            }
                            successMessage = if (currentLang == "kn") "ಕ್ಯಾಶ್ ನವೀಕರಿಸಲಾಗಿದೆ" else "Cache refreshed"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(com.hora.jnana.utils.TranslationUtils.translate("Refresh Cache", currentLang))
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(com.hora.jnana.utils.TranslationUtils.translate("Reset Settings", currentLang))
                    }
                }

                if (showResetDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetDialog = false },
                        title = { Text(com.hora.jnana.utils.TranslationUtils.translate("Reset Settings", currentLang)) },
                        text = { Text(com.hora.jnana.utils.TranslationUtils.translate("Are you sure you want to reset all settings to default values?", currentLang)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        dataStoreManager.resetSettings()
                                        successMessage = if (currentLang == "kn") "ಮರುಹೊಂದಿಸಲಾಗಿದೆ" else "Settings Reset"
                                        showResetDialog = false
                                    }
                                }
                            ) {
                                Text(com.hora.jnana.utils.TranslationUtils.translate("Reset", currentLang), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetDialog = false }) {
                                Text(com.hora.jnana.utils.TranslationUtils.translate("Cancel", currentLang))
                            }
                        }
                    )
                }

                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = { Text(if (currentLang == "kn") "ಲಾಗ್ ಔಟ್" else "Logout") },
                        text = { Text(if (currentLang == "kn") "ನೀವು ಖಚಿತವಾಗಿ ಲಾಗ್ ಔಟ್ ಮಾಡಲು ಬಯಸುವಿರಾ?" else "Are you sure you want to logout?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showLogoutDialog = false
                                    if (isLoggingOut) return@TextButton
                                    isLoggingOut = true
                                    scope.launch {
                                        val result = repo.logout()
                                        if (result.isSuccess) {
                                            authRepository.clearSessionToken()
                                            navController.navigate("login") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        } else {
                                            isLoggingOut = false
                                            snackbarHostState.showSnackbar(
                                                if (currentLang == "kn") "ಲಾಗ್ ಔಟ್ ವಿಫಲವಾಗಿದೆ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ." 
                                                else "Logout failed. Please try again."
                                            )
                                        }
                                    }
                                }
                            ) {
                                Text(if (currentLang == "kn") "ಹೌದು, ಲಾಗ್ ಔಟ್" else "Yes, Logout", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogoutDialog = false }) {
                                Text(if (currentLang == "kn") "ರದ್ದುಮಾಡಿ" else "Cancel")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        navController.navigate("home?forceTutorial=true") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentLang == "kn") "ಟ್ಯುಟೋರಿಯಲ್ ತೋರಿಸಿ" else "Show Tutorial")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showLogoutDialog = true },
                    enabled = !isLoggingOut,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (currentLang == "kn") "ಲಾಗ್ ಔಟ್" else "Logout")
                    }
                }
            }

            if (successMessage != null) {
                SuccessBox(
                    message = successMessage!!, 
                    lang = currentLang,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun SelectableSquareButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@Composable
fun NorthIndianChartIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(w, h), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w, 0f), end = androidx.compose.ui.geometry.Offset(0f, h), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w / 2, 0f), end = androidx.compose.ui.geometry.Offset(0f, h / 2), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, h / 2), end = androidx.compose.ui.geometry.Offset(w / 2, h), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w / 2, h), end = androidx.compose.ui.geometry.Offset(w, h / 2), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w, h / 2), end = androidx.compose.ui.geometry.Offset(w / 2, 0f), strokeWidth = 2.dp.toPx())
    }
}

@Composable
fun SouthIndianChartIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.5.dp.toPx()
        
        // Outer border
        drawRect(color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth))
        
        // Vertical lines
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w / 4, 0f), end = androidx.compose.ui.geometry.Offset(w / 4, h), strokeWidth = strokeWidth)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(3 * w / 4, 0f), end = androidx.compose.ui.geometry.Offset(3 * w / 4, h), strokeWidth = strokeWidth)
        // Middle vertical segments
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w / 2, 0f), end = androidx.compose.ui.geometry.Offset(w / 2, h / 4), strokeWidth = strokeWidth)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w / 2, 3 * h / 4), end = androidx.compose.ui.geometry.Offset(w / 2, h), strokeWidth = strokeWidth)
        
        // Horizontal lines
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, h / 4), end = androidx.compose.ui.geometry.Offset(w, h / 4), strokeWidth = strokeWidth)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, 3 * h / 4), end = androidx.compose.ui.geometry.Offset(w, 3 * h / 4), strokeWidth = strokeWidth)
        // Middle horizontal segments
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, h / 2), end = androidx.compose.ui.geometry.Offset(w / 4, h / 2), strokeWidth = strokeWidth)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(3 * w / 4, h / 2), end = androidx.compose.ui.geometry.Offset(w, h / 2), strokeWidth = strokeWidth)
        
        // Center square border
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w / 4, h / 4),
            size = androidx.compose.ui.geometry.Size(w / 2, h / 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun EastIndianChartIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.5.dp.toPx()
        
        // Outer border
        drawRect(color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth))
        
        // 3x3 Grid
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w / 3, 0f), end = androidx.compose.ui.geometry.Offset(w / 3, h), strokeWidth = strokeWidth)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(2 * w / 3, 0f), end = androidx.compose.ui.geometry.Offset(2 * w / 3, h), strokeWidth = strokeWidth)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, h / 3), end = androidx.compose.ui.geometry.Offset(w, h / 3), strokeWidth = strokeWidth)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, 2 * h / 3), end = androidx.compose.ui.geometry.Offset(w, 2 * h / 3), strokeWidth = strokeWidth)
        
        // Diagonals in corner squares
        // Top-Left
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(w / 3, h / 3), strokeWidth = strokeWidth)
        // Top-Right
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w, 0f), end = androidx.compose.ui.geometry.Offset(2 * w / 3, h / 3), strokeWidth = strokeWidth)
        // Bottom-Left
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, h), end = androidx.compose.ui.geometry.Offset(w / 3, 2 * h / 3), strokeWidth = strokeWidth)
        // Bottom-Right
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w, h), end = androidx.compose.ui.geometry.Offset(2 * w / 3, 2 * h / 3), strokeWidth = strokeWidth)
    }
}

@Composable
fun ThemeModeCircle(
    mode: String,
    isSelected: Boolean
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.Red else Color.Gray,
                shape = CircleShape
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (mode) {
                "light" -> {
                    drawCircle(color = Color.White)
                    drawCircle(color = Color.LightGray, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                }
                "dark" -> {
                    drawCircle(color = Color.Black)
                }
                "system" -> {
                    drawArc(
                        color = Color.White,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = Color.Black,
                        startAngle = 270f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun ColorWheel(
    modifier: Modifier = Modifier,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { size = it.size }
    ) {
        if (size.width > 0) {
            val widthPx = size.width.toFloat()
            val heightPx = size.height.toFloat()
            val radius = minOf(widthPx, heightPx) / 2f
            val center = Offset(widthPx / 2f, heightPx / 2f)

            val hsv = remember(selectedColor) {
                val hsvArr = FloatArray(3)
                android.graphics.Color.colorToHSV(selectedColor.toArgb(), hsvArr)
                hsvArr
            }

            val pointerOffset = remember(hsv, radius, center) {
                val angleRad = Math.toRadians(hsv[0].toDouble())
                val distance = hsv[1] * radius
                Offset(
                    x = center.x + (distance * Math.cos(angleRad)).toFloat(),
                    y = center.y + (distance * Math.sin(angleRad)).toFloat()
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(radius, center) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            
                            val processEvent = { position: Offset ->
                                val dx = position.x - center.x
                                val dy = position.y - center.y
                                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                                if (distance <= radius) {
                                    val angle = Math.atan2(dy.toDouble(), dx.toDouble()) * 180.0 / Math.PI
                                    val normalizedAngle = if (angle < 0) (angle + 360.0).toFloat() else angle.toFloat()
                                    val newHsv = floatArrayOf(normalizedAngle, distance / radius, 1f)
                                    onColorSelected(Color(android.graphics.Color.HSVToColor(newHsv)))
                                }
                            }
                            
                            processEvent(down.position)
                            
                            drag(down.id) { change ->
                                processEvent(change.position)
                                change.consume()
                            }
                        }
                    }
            ) {
                val colors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
                drawCircle(
                    brush = Brush.sweepGradient(colors, center),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Pointer
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = pointerOffset,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.Black,
                    radius = 2.dp.toPx(),
                    center = pointerOffset
                )
            }
        }
    }
}
