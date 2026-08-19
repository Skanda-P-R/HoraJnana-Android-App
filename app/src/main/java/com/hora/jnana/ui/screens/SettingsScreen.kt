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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hora.jnana.DataStoreManager
import com.hora.jnana.data.AuthRepository
import com.hora.jnana.ui.theme.AppTheme
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
    location: Pair<Double, Double>?,
    locationName: String?,
    locationMode: String
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoggingOut by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val currentLang by dataStoreManager.langFlow.collectAsState(initial = "en")
    val currentTheme by dataStoreManager.themeFlow.collectAsState(initial = "green")
    val currentThemeMode by dataStoreManager.themeModeFlow.collectAsState(initial = "light")
    val currentSavePath by dataStoreManager.savePathFlow.collectAsState(initial = null)
    val currentChartStyle by dataStoreManager.chartStyleFlow.collectAsState(initial = "south")
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Take persistable permission
            val contentResolver = context.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(it, takeFlags)
            
            scope.launch {
                dataStoreManager.saveSavePath(it.toString())
            }
        }
    }

    val displayPath = remember(currentSavePath) {
        if (currentSavePath.isNullOrEmpty()) {
            if (currentLang == "kn") "ಡೀಫಾಲ್ಟ್: Documents/Kundalis" else "Default: Documents/Kundalis"
        } else {
            val uri = Uri.parse(currentSavePath)
            DocumentFile.fromTreeUri(context, uri)?.name ?: currentSavePath
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentLang == "kn") "ಸೇಟಿಂಗ್ಸ್" else "Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(padding)
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
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppTheme.entries.forEach { theme ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(theme.mainColor)
                            .border(
                                width = if (currentTheme == theme.colorName) 3.dp else 1.dp,
                                color = if (currentTheme == theme.colorName) Color.Red else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                scope.launch {
                                    dataStoreManager.saveTheme(theme.colorName)
                                }
                            }
                    )
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
                if (currentLang == "kn") "ಕುಂಡಲಿ ಉಳಿಸುವ ಸ್ಥಳ" else "Kundali Save Location",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = displayPath ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { folderPickerLauncher.launch(null) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer, 
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (currentLang == "kn") "ಫೋಲ್ಡರ್ ಆಯ್ಕೆಮಾಡಿ" else "Select Save Folder")
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(if (currentLang == "kn") "ನಮ್ಮ ಬಗ್ಗೆ" else "About", style = MaterialTheme.typography.titleMedium)
            Text("HoraJnana v0.8.5", style = MaterialTheme.typography.bodyMedium)
            
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
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (currentLang == "kn") "ಕ್ಯಾಶ್ ನವೀಕರಿಸಲಾಗಿದೆ" else "Cache refreshed"
                            )
                        }
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
                onClick = {
                    if (isLoggingOut) return@Button
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
                },
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
