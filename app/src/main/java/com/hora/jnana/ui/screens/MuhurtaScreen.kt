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
fun MuhurtaScreen(
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
            val res = repo.fetchMuhurta(
                lat = location?.first,
                lon = location?.second,
                location = locationName,
                date = sdf.format(selectedDate.time),
                lang = lang
            )
            state = if (res.isSuccess) {
                repo.mergeToState(muhurtaJson = res.getOrNull()!!)
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
                title = { Text(TranslationUtils.translate("Muhurta", lang)) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (navController.currentDestination?.route == "muhurta") {
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
                        MuhurtaItem("Rahu Kalam", state.rahuKalam, lang)
                        Spacer(modifier = Modifier.height(12.dp))
                        MuhurtaItem("Gulika", state.gulika, lang)
                        Spacer(modifier = Modifier.height(12.dp))
                        MuhurtaItem("Yamaganda", state.yamaganda, lang)
                        Spacer(modifier = Modifier.height(12.dp))
                        MuhurtaItem("Abhijit", state.abhijit, lang)
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

@Composable
fun MuhurtaItem(label: String, value: String, lang: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(TranslationUtils.translate(label, lang), style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
