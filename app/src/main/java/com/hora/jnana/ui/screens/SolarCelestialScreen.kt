package com.hora.jnana.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hora.jnana.utils.TranslationUtils
import com.hora.jnana.repository.HoraRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolarCelestialScreen(
    navController: NavController,
    repo: HoraRepository,
    location: Pair<Double, Double>?,
    locationName: String?,
    lang: String = "en"
) {
    val scope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var state by remember { mutableStateOf(PanchangaState(isLoading = true)) }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displaySdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun fetchData() {
        scope.launch {
            state = PanchangaState(isLoading = true)
            
            // We need both 'day' for solar events and 'panchanga' for celestial (rasi)
            val dayRes = repo.fetchDay(
                lat = location?.first,
                lon = location?.second,
                location = locationName,
                date = sdf.format(selectedDate.time),
                lang = lang
            )
            val panRes = repo.fetchPanchanga(
                lat = location?.first,
                lon = location?.second,
                location = locationName,
                date = sdf.format(selectedDate.time),
                lang = lang
            )
            
            state = repo.mergeToState(
                dayJson = dayRes.getOrNull(),
                panchangaJson = panRes.getOrNull()
            ).copy(
                isLoading = false,
                error = if (dayRes.isFailure) dayRes.exceptionOrNull()?.message else null
            )
        }
    }

    LaunchedEffect(selectedDate, location, locationName) {
        fetchData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(TranslationUtils.translate("Solar & Celestial", lang)) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (navController.currentDestination?.route == "solar_celestial") {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                DateSelector(
                    dateStr = displaySdf.format(selectedDate.time),
                    onPrevious = {
                        val cal = selectedDate.clone() as Calendar
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        selectedDate = cal
                    },
                    onNext = {
                        val cal = selectedDate.clone() as Calendar
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        selectedDate = cal
                    },
                    onDateSelected = { selectedDate = it },
                    lang = lang
                )

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize()
                    ) {
                        SolarSection(TranslationUtils.translate("Solar Events", lang), listOf(
                            TranslationUtils.translate("Sunrise", lang) to state.sunrise,
                            TranslationUtils.translate("Sunset", lang) to state.sunset,
                            TranslationUtils.translate("Solar Noon", lang) to formatTime(state.solarNoonAt),
                            TranslationUtils.translate("Daylight Midpoint", lang) to formatTime(state.daylightMidpointAt)
                        ))

                        Spacer(modifier = Modifier.height(16.dp))

                        SolarSection(TranslationUtils.translate("Durations", lang), listOf(
                            TranslationUtils.translate("Day Duration", lang) to formatDuration(state.dayDuration),
                            TranslationUtils.translate("Night Duration", lang) to formatDuration(state.nightDuration)
                        ))

                        Spacer(modifier = Modifier.height(16.dp))

                        SolarSection(TranslationUtils.translate("Celestial", lang), listOf(
                            TranslationUtils.translate("Sun Rasi", lang) to state.sunRasi,
                            TranslationUtils.translate("Moon Rasi", lang) to state.moonRasi
                        ))
                    }
                }
            }

            if (state.error != null) {
                PersistentErrorBox(
                    error = state.error!!,
                    lang = lang,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

fun formatTime(iso: String): String {
    return iso.split("T").lastOrNull()?.take(8) ?: iso
}

fun formatDuration(secondsStr: String): String {
    val totalSeconds = secondsStr.toDoubleOrNull()?.toInt() ?: return secondsStr
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

@Composable
fun SolarSection(title: String, items: List<Pair<String, String>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(value, style = MaterialTheme.typography.bodyLarge)
                }
                if (items.last().first != label) {
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
