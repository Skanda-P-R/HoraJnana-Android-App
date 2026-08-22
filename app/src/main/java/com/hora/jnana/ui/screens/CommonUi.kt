package com.hora.jnana.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Error
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hora.jnana.utils.TranslationUtils
import java.util.*

@Composable
fun PersistentErrorBox(
    error: String,
    lang: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = TranslationUtils.translate(error, lang),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
fun SuccessBox(
    message: String,
    lang: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = TranslationUtils.translate(message, lang),
                color = Color(0xFF1B5E20),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    dateStr: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateSelected: (Calendar) -> Unit,
    lang: String = "en"
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    datePickerState.selectedDateMillis?.let {
                        cal.timeInMillis = it
                        onDateSelected(cal)
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

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showDatePicker = true }
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(dateStr, style = MaterialTheme.typography.titleMedium)
            }

            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelectorDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(timePickerState.hour, timePickerState.minute)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
