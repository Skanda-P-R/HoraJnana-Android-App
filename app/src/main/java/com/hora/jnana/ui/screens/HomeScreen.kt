package com.hora.jnana.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.hora.jnana.DataStoreManager
import com.hora.jnana.R
import com.hora.jnana.utils.NetworkUtils
import com.hora.jnana.utils.TranslationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    location: Pair<Double, Double>?,
    locationName: String?,
    locationMode: String,
    lang: String = "en",
    forceTutorial: Boolean = false
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }
    val tutorialShown by dataStoreManager.tutorialShownFlow.collectAsState(initial = true)

    var showWelcomeDialog by remember { mutableStateOf(false) }
    var isTutorialActive by remember { mutableStateOf(false) }
    var currentTutorialStep by remember { mutableIntStateOf(0) }
    var showWidgetPreview by remember { mutableStateOf(false) }

    // Positions for tutorial highlights
    var locationCardRect by remember { mutableStateOf(Rect.Zero) }
    var horaCardRect by remember { mutableStateOf(Rect.Zero) }
    var summaryCardRect by remember { mutableStateOf(Rect.Zero) }
    var menuGridRect by remember { mutableStateOf(Rect.Zero) }
    var settingsIconRect by remember { mutableStateOf(Rect.Zero) }

    var hasHandledForcedTutorial by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(forceTutorial) {
        if (forceTutorial && !hasHandledForcedTutorial) {
            isTutorialActive = true
            currentTutorialStep = 0
            hasHandledForcedTutorial = true
        }
    }

    LaunchedEffect(tutorialShown) {
        if (!tutorialShown && !forceTutorial) {
            showWelcomeDialog = true
        }
    }
    
    // Quantize location to avoid jitter triggering refreshes
    val quantizedLocation = remember(location) {
        if (location == null) return@remember null
        val lat = (location.first * 1000).toInt() / 1000.0
        val lon = (location.second * 1000).toInt() / 1000.0
        lat to lon
    }

    var remainingDisplay by remember { mutableStateOf(state.remaining) }

    LaunchedEffect(state.horaEndsAt, state.remaining) {
        while (true) {
            val endsAtStr = state.horaEndsAt
            if (endsAtStr != null) {
                try {
                    val endsAt = ZonedDateTime.parse(endsAtStr).toInstant().toEpochMilli()
                    val now = System.currentTimeMillis()
                    if (now < endsAt) {
                        val diffMinutes = (endsAt - now) / 60000
                        remainingDisplay = if (diffMinutes > 0) "$diffMinutes min" else "< 1 min"
                    } else {
                        remainingDisplay = "0 min"
                        if (NetworkUtils.isOnline(context)) {
                            if (locationMode == "gps" && quantizedLocation != null) {
                                viewModel.refreshHoraOnly(context, quantizedLocation.first, quantizedLocation.second, null)
                            } else if (locationMode == "manual") {
                                viewModel.refreshHoraOnly(context, null, null, locationName)
                            }
                        }
                    }
                } catch (e: Exception) {
                    remainingDisplay = state.remaining
                }
            } else {
                remainingDisplay = state.remaining
            }
            delay(10000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = TranslationUtils.translate("HoraJnana", lang),
                        modifier = Modifier.clickable(enabled = false) { }
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = { navController.navigate("settings") },
                        enabled = !isTutorialActive,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            settingsIconRect = Rect(coords.positionInRoot(), coords.size.toSize())
                        }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                if (state.error != null) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                LocationCard(
                    name = locationName, 
                    mode = locationMode, 
                    lang = lang,
                    enabled = !isTutorialActive,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        locationCardRect = Rect(coords.positionInRoot(), coords.size.toSize())
                    }
                ) {
                    navController.navigate("locations")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            horaCardRect = Rect(coords.positionInRoot(), coords.size.toSize())
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = TranslationUtils.translate("Current Hora", lang), 
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = state.horaSymbol + " " + state.hora, 
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = TranslationUtils.translate("Remaining", lang) + ": " + remainingDisplay, 
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (remainingDisplay == "0 min" && !NetworkUtils.isOnline(context)) {
                            Text(
                                text = TranslationUtils.translate("Updated", lang) + ": " + state.lastUpdated,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SummaryCard(
                    state = state, 
                    lang = lang,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        summaryCardRect = Rect(coords.positionInRoot(), coords.size.toSize())
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.onGloballyPositioned { coords ->
                        menuGridRect = Rect(coords.positionInRoot(), coords.size.toSize())
                    }
                ) {
                    val menuItems = listOf(
                        MenuItem(TranslationUtils.translate("Panchanga", lang), Icons.Default.CalendarMonth, "panchanga_detail"),
                        MenuItem(TranslationUtils.translate("Hora", lang), Icons.Default.Schedule, "hora_detail"),
                        MenuItem(TranslationUtils.translate("Solar & Celestial", lang), Icons.Default.WbSunny, "solar_celestial"),
                        MenuItem(TranslationUtils.translate("Muhurta", lang), Icons.Default.Timer, "muhurta"),
                        MenuItem(TranslationUtils.translate("Transit Kundali", lang), Icons.Default.Map, "transit_kundali"),
                        MenuItem(TranslationUtils.translate("Birth Kundali", lang), Icons.Default.Person, "birth_kundali")
                    )

                    // Grid layout using Rows for full screen scrolling
                    menuItems.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MenuCard(
                                        item = item,
                                        enabled = !isTutorialActive
                                    ) {
                                        navController.navigate(item.route)
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .height(2.dp)
                )
            }
        }

        if (isTutorialActive) {
            TutorialOverlay(
                currentStep = currentTutorialStep,
                rects = listOf(locationCardRect, horaCardRect, summaryCardRect, menuGridRect, settingsIconRect),
                lang = lang,
                topPadding = padding.calculateTopPadding(),
                onNext = {
                    if (currentTutorialStep < 4) {
                        currentTutorialStep++
                    } else {
                        isTutorialActive = false
                        showWidgetPreview = true
                    }
                },
                onBack = {
                    if (currentTutorialStep > 0) {
                        currentTutorialStep--
                    }
                },
                onSkip = {
                    isTutorialActive = false
                    scope.launch { dataStoreManager.saveTutorialShown(true) }
                }
            )
        }

        if (showWelcomeDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showWelcomeDialog = false
                    scope.launch { dataStoreManager.saveTutorialShown(true) }
                },
                title = { Text(if (lang == "kn") "ಸ್ವಾಗತ!" else "Welcome!") },
                text = { Text(if (lang == "kn") "ನೀವು ಅಪ್ಲಿಕೇಶನ್ ಟ್ಯುಟೋರಿಯಲ್ ಮೂಲಕ ಹೋಗಲು ಬಯಸುವಿರಾ?" else "Would you like to go through the app tutorial?") },
                confirmButton = {
                    TextButton(onClick = {
                        showWelcomeDialog = false
                        isTutorialActive = true
                        scope.launch { dataStoreManager.saveTutorialShown(true) }
                    }) {
                        Text(if (lang == "kn") "ಹೌದು" else "Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showWelcomeDialog = false
                        scope.launch { dataStoreManager.saveTutorialShown(true) }
                    }) {
                        Text(if (lang == "kn") "ಬೇಡ" else "Skip")
                    }
                }
            )
        }

        if (showWidgetPreview) {
            Dialog(onDismissRequest = { 
                showWidgetPreview = false
                scope.launch { dataStoreManager.saveTutorialShown(true) }
            }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (lang == "kn") "ಹೋಮ್ ಸ್ಕ್ರೀನ್ ವಿಜೆಟ್" else "Home Screen Widget",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Image(
                            painter = painterResource(id = R.drawable.preview_hora_widget),
                            contentDescription = "Widget Preview",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (lang == "kn") 
                                "ನೀವು ನಿಮ್ಮ ಹೋಮ್ ಸ್ಕ್ರೀನ್‌ಗೆ ಹೋರಾ ವಿಜೆಟ್ ಅನ್ನು ಸೇರಿಸಬಹುದು, ಇದು ಕ್ರಿಯಾತ್ಮಕವಾಗಿ ನವೀಕರಿಸಲ್ಪಡುತ್ತದೆ." 
                                else "You can also add a Hora widget to your home screen which updates dynamically.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (lang == "kn") 
                                "ಹೇಗೆ ಸೇರಿಸುವುದು: ಹೋಮ್ ಸ್ಕ್ರೀನ್ ಮೇಲೆ ಲಾಂಗ್ ಪ್ರೆಸ್ ಮಾಡಿ, 'ವಿಜೆಟ್ಸ್' ಆಯ್ಕೆಮಾಡಿ, 'HoraJnana' ಗಾಗಿ ಹುಡುಕಿ ಮತ್ತು ವಿಜೆಟ್ ಅನ್ನು ಎಳೆಯಿರಿ. ನೀವು ಅದನ್ನು ನಿಮಗೆ ಬೇಕಾದಂತೆ ಮರುಗಾತ್ರಗೊಳಿಸಬಹುದು."
                                else "How to add: Long press on your home screen, select 'Widgets', search for 'HoraJnana', and drag the widget to your screen. You can then resize it as you like.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                showWidgetPreview = false
                                scope.launch { dataStoreManager.saveTutorialShown(true) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (lang == "kn") "ಮುಗಿಸಿ" else "Finish")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(quantizedLocation, locationName, locationMode, lang) {
        if (locationMode == "gps") {
            viewModel.refresh(context, quantizedLocation?.first, quantizedLocation?.second, null, force = false)
        } else {
            viewModel.refresh(context, null, null, locationName, force = false)
        }
    }
}

@Composable
fun LocationCard(name: String?, mode: String, lang: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (mode == "gps") TranslationUtils.translate("Current Location", lang) else name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (mode == "gps") {
                    Text("GPS Tracking", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

data class MenuItem(val title: String, val icon: ImageVector, val route: String)

@Composable
fun MenuCard(item: MenuItem, enabled: Boolean = true, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(enabled = enabled) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SummaryCard(state: PanchangaState, lang: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem(
                    TranslationUtils.translate("Tithi", lang), 
                    state.tithi
                )
                InfoItem(
                    TranslationUtils.translate("Vara", lang), 
                    state.vara
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem(TranslationUtils.translate("Sunrise", lang), state.sunrise)
                InfoItem(TranslationUtils.translate("Sunset", lang), state.sunset)
            }
        }
    }
}

@Composable
fun ColumnScope.InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun RowScope.InfoItem(label: String, value: String) {
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun TutorialOverlay(
    currentStep: Int,
    rects: List<Rect>,
    lang: String,
    topPadding: androidx.compose.ui.unit.Dp,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val highlightRect = rects.getOrNull(currentStep) ?: Rect.Zero
    val isSettingsStep = currentStep == 4
    val padding = 8.dp
    val cornerRadius = 12.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingPx = padding.toPx()
            val radiusPx = cornerRadius.toPx()
            val topPaddingPx = topPadding.toPx()
            
            val paddedRect = highlightRect.inflate(paddingPx)

            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                if (!isSettingsStep) {
                    addRoundRect(
                        RoundRect(
                            rect = paddedRect,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx)
                        )
                    )
                }
                fillType = PathFillType.EvenOdd
            }
            
            clipRect(top = if (!isSettingsStep) topPaddingPx else 0f) {
                drawPath(path, color = Color.Black.copy(alpha = 0.7f))
            }

            if (isSettingsStep && highlightRect != Rect.Zero) {
                // Draw a straight diagonal arrow pointing to settings icon
                val tipX = highlightRect.center.x - 15.dp.toPx()
                val tipY = highlightRect.bottom + 15.dp.toPx()
                val tailLength = 100.dp.toPx() 
                
                val arrowPath = Path().apply {
                    // Tip of the arrow
                    moveTo(tipX, tipY)
                    // Perfectly diagonal tail going down and left (45 degrees)
                    // dx = dy = length / sqrt(2) ~= length * 0.707
                    val delta = tailLength * 0.707f
                    lineTo(tipX - delta, tipY + delta)
                    
                    // Arrow head segments - perfectly symmetrical around the 45-degree axis
                    val mainHeadOffset = 25.dp.toPx()
                    val sideHeadOffset = 6.dp.toPx()
                    
                    // Segment 1: Mostly horizontal (going left)
                    moveTo(tipX, tipY)
                    lineTo(tipX - mainHeadOffset, tipY + sideHeadOffset)
                    
                    // Segment 2: Mostly vertical (going down)
                    moveTo(tipX, tipY)
                    lineTo(tipX - sideHeadOffset, tipY + mainHeadOffset)
                }
                drawPath(
                    arrowPath,
                    color = Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 6.dp.toPx(), 
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }
        }

        TutorialTooltip(
            step = currentStep,
            highlightRect = highlightRect,
            lang = lang,
            onNext = onNext,
            onBack = onBack,
            onSkip = onSkip
        )
    }
}

@Composable
fun TutorialTooltip(
    step: Int,
    highlightRect: Rect,
    lang: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    val description = when (step) {
        0 -> if (lang == "kn") "ಇಲ್ಲಿ ನಿಮ್ಮ ಪ್ರಸ್ತುತ ಸ್ಥಳವನ್ನು ನೋಡಬಹುದು. ಸ್ಥಳವನ್ನು ಬದಲಾಯಿಸಲು ಇದರ ಮೇಲೆ ಕ್ಲಿಕ್ ಮಾಡಿ." else "Here you can see your current location. Click to change it."
        1 -> if (lang == "kn") "ಇದು ಪ್ರಸ್ತುತ ಹೋರಾ ಮತ್ತು ಅದು ಮುಗಿಯಲು ಉಳಿದಿರುವ ಸಮಯವನ್ನು ತೋರಿಸುತ್ತದೆ." else "This shows the current Hora and the time remaining for it to end."
        2 -> if (lang == "kn") "ಇದು ಇಂದಿನ ತಿಥಿ, ವಾರ, ಸೂರ್ಯೋದಯ ಮತ್ತು ಸೂರ್ಯಾಸ್ತದ ವಿವರಗಳನ್ನು ನೀಡುತ್ತದೆ." else "This provides today's Tithi, Vara, Sunrise, and Sunset details."
        3 -> if (lang == "kn") "ವಿವಿಧ ಜ್ಯೋತಿಷ್ಯ ವೈಶಿಷ್ಟ್ಯಗಳನ್ನು ಅನ್ವೇಷಿಸಲು ಈ ಬಟನ್‌ಗಳನ್ನು ಬಳಸಿ." else "Use these buttons to explore various astrological features."
        4 -> if (lang == "kn") "ಅಪ್ಲಿಕೇಶನ್ ಸೆಟ್ಟಿಂಗ್‌ಗಳನ್ನು ಬದಲಾಯಿಸಲು ಇಲ್ಲಿ ಕ್ಲಿಕ್ ಮಾಡಿ. ಇಲ್ಲಿಂದ ನೀವು ಮತ್ತೆ ಟ್ಯುಟೋರಿಯಲ್ ನೋಡಬಹುದು." else "Click here to change app settings. You can watch the tutorial again from here."
        else -> ""
    }

    val centerInDp = with(density) { highlightRect.center.y.toDp() }
    val isTopHalf = centerInDp < (screenHeight / 2)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .align(if (isTopHalf) Alignment.BottomCenter else Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .padding(top = if (!isTopHalf) 100.dp else 0.dp) // Avoid merging with TopAppBar/Status bar
                .padding(bottom = if (isTopHalf) 48.dp else 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text(if (lang == "kn") "ಬಿಟ್ಟುಬಿಡಿ" else "Skip")
                    }
                    Row {
                        if (step > 0) {
                            TextButton(onClick = onBack) {
                                Text(if (lang == "kn") "ಹಿಂದಕ್ಕೆ" else "Back")
                            }
                        }
                        Button(onClick = onNext) {
                            Text(if (lang == "kn") "ಮುಂದೆ" else "Next")
                        }
                    }
                }
            }
        }
    }
}
