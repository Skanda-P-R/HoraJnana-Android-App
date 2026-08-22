package com.hora.jnana.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CalendarToday
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
fun PanchangaDetailScreen(
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
            val res = repo.fetchPanchanga(
                lat = location?.first,
                lon = location?.second,
                location = locationName,
                date = sdf.format(selectedDate.time),
                lang = lang
            )
            state = if (res.isSuccess) {
                repo.mergeToState(panchangaJson = res.getOrNull()!!)
            } else {
                state.copy(isLoading = false, error = res.exceptionOrNull()?.message)
            }
        }
    }

    LaunchedEffect(selectedDate, location, locationName) {
        fetchData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(TranslationUtils.translate("Panchanga", lang)) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (navController.currentDestination?.route == "panchanga_detail") {
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
                        PanchangaSection(TranslationUtils.translate("Limbs", lang), listOf(
                            LimbData(TranslationUtils.translate("Tithi", lang), state.tithi, state.tithiEnds),
                            LimbData(TranslationUtils.translate("Nakshatra", lang), state.nakshatra, state.nakshatraEnds),
                            LimbData(TranslationUtils.translate("Yoga", lang), state.yoga, state.yogaEnds),
                            LimbData(TranslationUtils.translate("Karana", lang), state.karana, state.karanaEnds),
                            LimbData(TranslationUtils.translate("Vara", lang), state.vara, state.varaEnds)
                        ), lang)

                        Spacer(modifier = Modifier.height(16.dp))

                        PanchangaSection(TranslationUtils.translate("Calendar", lang), listOf(
                            LimbData(TranslationUtils.translate("Samvatsara", lang), state.samvatsara, state.samvatsaraEnds),
                            LimbData(TranslationUtils.translate("Ayana", lang), state.ayana, state.ayanaEnds),
                            LimbData(TranslationUtils.translate("Rutu", lang), state.rutu, state.rutuEnds),
                            LimbData(TranslationUtils.translate("Masa", lang), state.masa, state.masaEnds),
                            LimbData(TranslationUtils.translate("Paksha", lang), state.paksha, state.pakshaEnds)
                        ), lang)
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

data class LimbData(val label: String, val value: String, val ends: String = "")

@Composable
fun PanchangaSection(title: String, items: List<LimbData>, lang: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.label, style = MaterialTheme.typography.bodyMedium)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(item.value, style = MaterialTheme.typography.bodyLarge)
                        if (item.ends.isNotEmpty()) {
                            Text("${TranslationUtils.translate("Ends", lang)}: ${item.ends}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                if (items.last() != item) {
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
