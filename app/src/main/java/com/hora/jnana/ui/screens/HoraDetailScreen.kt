package com.hora.jnana.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hora.jnana.utils.TranslationUtils
import com.hora.jnana.repository.HoraRepository
import com.hora.jnana.utils.NetworkUtils
import com.hora.jnana.utils.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoraDetailScreen(
    navController: NavController,
    repo: HoraRepository,
    location: Pair<Double, Double>?,
    locationName: String?,
    lang: String = "en"
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedTime by remember { mutableStateOf(Calendar.getInstance()) }
    
    var state by remember { mutableStateOf(PanchangaState(isLoading = true)) }
    var remainingDisplay by remember { mutableStateOf(state.remaining) }
    
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val displayDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun fetchData(force: Boolean = false, silent: Boolean = false) {
        scope.launch {
            if (!silent) state = PanchangaState(isLoading = true)
            val res = repo.fetchHora(
                lat = location?.first,
                lon = location?.second,
                location = locationName,
                date = sdfDate.format(selectedDate.time),
                time = sdfTime.format(selectedTime.time),
                lang = lang,
                force = force
            )
            val newState = if (res.isSuccess) {
                repo.mergeToState(
                    horaJson = res.getOrNull(),
                    targetTimeMillis = selectedTime.timeInMillis
                )
            } else {
                state.copy(isLoading = false, error = res.exceptionOrNull()?.message)
            }
            state = newState
            remainingDisplay = newState.remaining
        }
    }

    LaunchedEffect(selectedDate, selectedTime, location, locationName) {
        fetchData()
    }

    LaunchedEffect(state.horaEndsAt) {
        while (true) {
            val today = Calendar.getInstance()
            val isCurrentTime = sdfDate.format(selectedDate.time) == sdfDate.format(today.time) && 
                               Math.abs(selectedTime.timeInMillis - today.timeInMillis) < 600000 

            if (isCurrentTime) {
                fetchData(force = false, silent = true)
            } else {
                remainingDisplay = state.remaining
            }
            delay(10000)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(TranslationUtils.translate("Hora", lang)) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (navController.currentDestination?.route == "hora_detail") {
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Card(modifier = Modifier.weight(1f).padding(4.dp).clickable { showDatePicker = true }) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                            Text(displayDate.format(selectedDate.time), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(modifier = Modifier.weight(1f).padding(4.dp).clickable { showTimePicker = true }) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Text(sdfTime.format(selectedTime.time), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(TranslationUtils.translate("Current Hora", lang), style = MaterialTheme.typography.titleMedium)
                            Text(state.horaSymbol + " " + state.hora, style = MaterialTheme.typography.displayLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                DetailItem(TranslationUtils.translate("Ends", lang), state.horaEnds)
                                DetailItem(TranslationUtils.translate("Remaining", lang), remainingDisplay)
                            }
                            if (remainingDisplay == "0 min" && !NetworkUtils.isOnline(context)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = TranslationUtils.translate("Updated", lang) + ": " + state.lastUpdated,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HoraListSection(
                        title = TranslationUtils.translate("Day Hora", lang),
                        list = state.dayHoraList,
                        currentHora = state.hora
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HoraListSection(
                        title = TranslationUtils.translate("Night Hora", lang),
                        list = state.nightHoraList,
                        currentHora = state.hora
                    )
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

@Composable
fun HoraListSection(
    title: String,
    list: List<com.hora.jnana.models.HoraListItem>,
    currentHora: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val now = System.currentTimeMillis()
            list.forEach { item ->
                val start = DateUtils.parseToMillis(item.startsAt)
                val end = DateUtils.parseToMillis(item.endsAt)
                val isCurrent = if (start > 0 && end > 0) {
                    now in start until end
                } else {
                    item.planet == currentHora
                }
                HoraRow(item, isCurrent)
            }
        }
    }
}

@Composable
fun HoraRow(item: com.hora.jnana.models.HoraListItem, isCurrent: Boolean) {
    val bgColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = MaterialTheme.shapes.small,
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.symbol} ${item.planet}",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Text(
                text = "${item.starts ?: ""} - ${item.ends}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
