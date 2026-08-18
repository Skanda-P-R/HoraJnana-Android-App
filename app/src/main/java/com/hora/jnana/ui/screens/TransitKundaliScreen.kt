package com.hora.jnana.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hora.jnana.models.DashaPeriod
import com.hora.jnana.utils.TranslationUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransitKundaliScreen(
    navController: NavController,
    viewModel: TransitViewModel,
    location: Pair<Double, Double>?,
    locationName: String?,
    apiBase: String,
    sessionToken: String?,
    lang: String = "en",
    chartStyle: String = "south"
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedTime by remember { mutableStateOf(Calendar.getInstance()) }
    
    // Local overrides
    var localLat by remember { mutableStateOf(location?.first) }
    var localLon by remember { mutableStateOf(location?.second) }
    var localLocName by remember { mutableStateOf(locationName) }
    var localChartStyle by remember { mutableStateOf(chartStyle) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showLocationSelector by remember { mutableStateOf(false) }
    var showStyleSelector by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    val state by viewModel.state.collectAsState()
    val selectedL1 by viewModel.selectedL1.collectAsState()
    val selectedL2 by viewModel.selectedL2.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val displayDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val valueFontWeight = if (lang == "kn") FontWeight.Normal else FontWeight.Bold


    // Initial and on-change fetch
    LaunchedEffect(selectedDate, selectedTime, localLat, localLon, localLocName, localChartStyle) {
        viewModel.fetchData(
            lat = localLat,
            lon = localLon,
            location = localLocName,
            date = sdfDate.format(selectedDate.time),
            time = sdfTime.format(selectedTime.time),
            lang = lang,
            apiBase = apiBase,
            depth = 3,
            chartStyle = localChartStyle,
            sessionToken = sessionToken
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it
                        selectedDate = cal
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimeSelectorDialog(
            onDismiss = { showTimePicker = false },
            onTimeSelected = { h, m ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                selectedTime = cal
            }
        )
    }

    if (showLocationSelector) {
        TransitLocationSelectorDialog(
            onDismiss = { showLocationSelector = false },
            locations = state.locations,
            isFetching = state.isFetchingLocations,
            onLocationSelected = { loc ->
                localLat = loc.latitude
                localLon = loc.longitude
                localLocName = loc.name
                showLocationSelector = false
            },
            lang = lang
        )
    }

    if (showStyleSelector) {
        TransitStyleSelectorDialog(
            onDismiss = { showStyleSelector = false },
            currentStyle = localChartStyle,
            onStyleSelected = { style ->
                localChartStyle = style
                showStyleSelector = false
            },
            lang = lang
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(TranslationUtils.translate("Transit Kundali", lang)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Selectors
            Column(modifier = Modifier.padding(8.dp)) {
                // First Row: Date & Time
                Row(modifier = Modifier.fillMaxWidth()) {
                    Card(modifier = Modifier.weight(1f).padding(4.dp).clickable { showDatePicker = true }) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(displayDate.format(selectedDate.time), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Card(modifier = Modifier.weight(1f).padding(4.dp).clickable { showTimePicker = true }) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(sdfTime.format(selectedTime.time), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                // Second Row: Location & Chart Style
                Row(modifier = Modifier.fillMaxWidth()) {
                    Card(modifier = Modifier.weight(1f).padding(4.dp).clickable { 
                        viewModel.fetchLocations()
                        showLocationSelector = true 
                    }) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = localLocName ?: TranslationUtils.translate("Location", lang),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                    Card(modifier = Modifier.weight(1f).padding(4.dp).clickable { showStyleSelector = true }) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Grid3x3, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = TranslationUtils.translate(localChartStyle.replaceFirstChar { it.uppercase() }, lang),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 0.dp,
                divider = {}
            ) {
                val tabs = listOf("Info", "Kundali", "Dasha", "Karakas")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                    ) {
                        Text(
                            text = TranslationUtils.translate(title, lang),
                            modifier = Modifier.padding(16.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        when (page) {
                            0 -> InfoTab(state, viewModel, lang, valueFontWeight)
                            1 -> KundaliTab(state, sessionToken)
                            2 -> DashaTab(state, viewModel, selectedL1, selectedL2, valueFontWeight)
                            3 -> KarakasTab(state, viewModel, lang, valueFontWeight)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KarakasTab(state: TransitState, viewModel: TransitViewModel, lang: String, valueFontWeight: FontWeight) {
    val karakas = state.kundaliResponse?.charaKarakas ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        karakas.forEach { karaka ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = TranslationUtils.translate(karaka.karaka, lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(TranslationUtils.translate("Planet", lang) + ":")
                        Text(karaka.planet, fontWeight = valueFontWeight)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(TranslationUtils.translate("Degree", lang) + ":")
                        Text(viewModel.formatDegrees(karaka.degreeInRasi), fontWeight = valueFontWeight)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(TranslationUtils.translate("Rashi", lang) + ":")
                        Text(karaka.rasi, fontWeight = valueFontWeight)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(TranslationUtils.translate("Nakshatra, Pada", lang) + ":")
                        Text("${karaka.nakshatra}, ${karaka.pada}", fontWeight = valueFontWeight)
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = karaka.signification,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun TransitLocationSelectorDialog(
    onDismiss: () -> Unit,
    locations: List<com.hora.jnana.models.LocationData>,
    isFetching: Boolean = false,
    onLocationSelected: (com.hora.jnana.models.LocationData) -> Unit,
    lang: String
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, locations) {
        locations.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(TranslationUtils.translate("Select Location", lang)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(TranslationUtils.translate("Search Location", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (isFetching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    filtered.take(20).forEach { loc ->
                        ListItem(
                            headlineContent = { Text(loc.name) },
                            modifier = Modifier.clickable { onLocationSelected(loc) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(TranslationUtils.translate("Cancel", lang)) }
        }
    )
}

@Composable
fun TransitStyleSelectorDialog(
    onDismiss: () -> Unit,
    currentStyle: String,
    onStyleSelected: (String) -> Unit,
    lang: String
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    TranslationUtils.translate("Chart Style", lang),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val styles = listOf("north", "south", "east")
                    styles.forEach { style ->
                        TransitStyleOption(
                            style = style,
                            isSelected = style == currentStyle,
                            onClick = { onStyleSelected(style) },
                            lang = lang,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(TranslationUtils.translate("Cancel", lang))
                }
            }
        }
    }
}

@Composable
fun TransitStyleOption(
    style: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = isSelected,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(48.dp).padding(4.dp), contentAlignment = Alignment.Center) {
                when (style) {
                    "north" -> NorthIndianChartIcon(color = LocalContentColor.current)
                    "south" -> SouthIndianChartIcon(color = LocalContentColor.current)
                    "east" -> EastIndianChartIcon(color = LocalContentColor.current)
                }
            }
            Text(
                text = TranslationUtils.translate(style.replaceFirstChar { it.uppercase() }, lang),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun InfoTab(state: TransitState, viewModel: TransitViewModel, lang: String, valueFontWeight: FontWeight) {
    val dasha = state.dashaResponse ?: return
    val kundali = state.kundaliResponse
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(TranslationUtils.translate("Moon Information", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PanchangaRow(TranslationUtils.translate("Rashi", lang), dasha.moon.rasi, valueFontWeight)
                
                val moonInfo = dasha.moon
                val padaText = if (moonInfo.nakshatraPada != null) ", ${moonInfo.nakshatraPada}" else ""
                PanchangaRow(TranslationUtils.translate("Nakshatra, Pada", lang), "${moonInfo.nakshatra}$padaText", valueFontWeight)
                
                kundali?.lagna?.let { lagna ->
                    PanchangaRow(TranslationUtils.translate("Lagna", lang), viewModel.formatDegrees(lagna.degreeInRasi), valueFontWeight)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(TranslationUtils.translate("Balance of Dasha", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PanchangaRow(TranslationUtils.translate("Lord", lang), dasha.dashaBalance.lord, valueFontWeight)
                PanchangaRow(TranslationUtils.translate("Remaining", lang), viewModel.formatDecimalYears(dasha.dashaBalance.remainingYears), valueFontWeight)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(TranslationUtils.translate("Active Dasha", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ActiveDashaRow(TranslationUtils.translate("Mahadasha", lang), dasha.activeDasha.mahadasha, valueFontWeight)
                dasha.activeDasha.antardasha?.let { 
                    ActiveDashaRow(TranslationUtils.translate("Antardasha", lang), it, valueFontWeight) 
                }
                dasha.activeDasha.pratyantardasha?.let { 
                    ActiveDashaRow(TranslationUtils.translate("Pratyantardasha", lang), it, valueFontWeight) 
                }
            }
        }

        if (kundali != null) {
            val yogi = kundali.yogiAvayogi
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(TranslationUtils.translate("Yogi and Aviyogi", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    PanchangaRow(TranslationUtils.translate("Yogi", lang), yogi.yogiPlanet, valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Avayogi", lang), yogi.avayogiPlanet, valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Duplicate Yogi", lang), yogi.duplicateYogi, valueFontWeight)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(TranslationUtils.translate("Chara Karakas", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    PanchangaRow(TranslationUtils.translate("Atmakaraka", lang), kundali.atmakaraka.planet, valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Darakaraka", lang), kundali.darakaraka.planet, valueFontWeight)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                val p = kundali.panchanga
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(TranslationUtils.translate("Panchanga", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    PanchangaRow(TranslationUtils.translate("Samvatsara", lang), p["samvatsara"] ?: "--", valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Ayana", lang), p["ayana"] ?: "--", valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Rutu", lang), p["rutu"] ?: "--", valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Masa", lang), p["masa"] ?: "--", valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Paksha", lang), p["paksha"] ?: "--", valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Vara", lang), p["vara"] ?: "--", valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Tithi", lang), p["tithi"] ?: "--", valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Nakshatra", lang), p["nakshatra"] ?: "--", valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Yoga", lang), p["yoga"] ?: "--", valueFontWeight)
                    PanchangaRow(TranslationUtils.translate("Karana", lang), p["karana"] ?: "--", valueFontWeight)
                }
            }
        }
    }
}

@Composable
fun PanchangaRow(label: String, value: String, valueFontWeight: FontWeight) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(if (label.endsWith(":")) label else "$label:")
        Text(value, fontWeight = valueFontWeight)
    }
}

@Composable
fun ActiveDashaRow(label: String, value: String, valueFontWeight: FontWeight) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(if (label.endsWith(":")) label else "$label:")
        Text(value, fontWeight = valueFontWeight)
    }
}

@Composable
fun KundaliTab(state: TransitState, sessionToken: String?) {
    val url = state.chartUrl ?: return
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .addHeader("Authorization", "Bearer $sessionToken")
                .build(),
            contentDescription = "Transit Kundali",
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun DashaTab(
    state: TransitState,
    viewModel: TransitViewModel,
    selectedL1: DashaPeriod?,
    selectedL2: DashaPeriod?,
    valueFontWeight: FontWeight
) {
    val timeline = state.dashaResponse?.timeline ?: return
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Level 1 Breadcrumb
        if (selectedL1 != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(8.dp)
            ) {
                Text(
                    text = "L1: ${selectedL1.lord}",
                    modifier = Modifier.clickable { viewModel.selectL1(null) }.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
                if (selectedL2 != null) {
                    Text(" > ", modifier = Modifier.align(Alignment.CenterVertically))
                    Text(
                        text = "L2: ${selectedL2.lord}",
                        modifier = Modifier.clickable { viewModel.selectL2(null) }.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val displayList = when {
            selectedL2 != null -> selectedL2.subPeriods
            selectedL1 != null -> selectedL1.subPeriods
            else -> timeline
        }

        val canClick = selectedL2 == null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            displayList.forEach { period ->
                DashaPeriodItem(period, canClick) {
                    if (period.level == 1) {
                        viewModel.selectL1(period)
                    } else if (period.level == 2) {
                        viewModel.selectL2(period)
                    }
                }
            }
        }
    }
}

@Composable
fun DashaPeriodItem(period: DashaPeriod, clickable: Boolean, onClick: () -> Unit) {
    val rawStart = period.start.split("T").first()
    val rawEnd = period.end.split("T").first()

    // Reformat YYYY-MM-DD to DD-MM-YYYY
    val formatRaw = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val formatDisplay = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    
    val startDate = try {
        val date = formatRaw.parse(rawStart)
        if (date != null) formatDisplay.format(date) else rawStart
    } catch (_: Exception) { rawStart }

    val endDate = try {
        val date = formatRaw.parse(rawEnd)
        if (date != null) formatDisplay.format(date) else rawEnd
    } catch (_: Exception) { rawEnd }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = period.lord,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$startDate to $endDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (clickable) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
