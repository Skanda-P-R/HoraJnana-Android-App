package com.hora.jnana.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hora.jnana.models.LocationData
import com.hora.jnana.models.SavedKundaliMeta
import com.hora.jnana.ui.screens.TimeSelectorDialog
import com.hora.jnana.utils.TranslationUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SavedKundalisDialog(
    onDismiss: () -> Unit,
    savedKundalis: List<SavedKundaliMeta>,
    isListing: Boolean,
    onSelect: (Uri) -> Unit,
    onDelete: (Uri) -> Unit,
    onAddProfile: (String, String, String, String) -> Unit,
    lang: String,
    locations: List<LocationData> = emptyList(),
    onToggleAddForm: (Boolean) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    
    val filtered = remember(searchQuery, savedKundalis) {
        if (searchQuery.isEmpty()) savedKundalis
        else {
            savedKundalis.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.date.contains(searchQuery, ignoreCase = true) ||
                it.time.contains(searchQuery, ignoreCase = true) ||
                (it.locationName?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        TranslationUtils.translate("Saved Kundalis", lang),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { 
                        showAddForm = !showAddForm
                        onToggleAddForm(showAddForm)
                    }) {
                        Icon(
                            if (showAddForm) Icons.AutoMirrored.Filled.List else Icons.Default.Add,
                            contentDescription = "Add Profile"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (showAddForm) {
                    AddProfileForm(
                        lang = lang,
                        locations = locations,
                        onAdd = { n, d, t, p -> 
                            onAddProfile(n, d, t, p)
                            showAddForm = false
                        },
                        onCancel = { showAddForm = false }
                    )
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(TranslationUtils.translate("Search Name, Date, or Place", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isListing) {
                        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text(TranslationUtils.translate("No saved kundalis found", lang))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered) { item ->
                                SavedKundaliItem(
                                    meta = item,
                                    onClick = { onSelect(item.uri); onDismiss() },
                                    onDelete = { onDelete(item.uri) }
                                )
                            }
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileForm(
    lang: String,
    locations: List<LocationData>,
    onAdd: (String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var tob by remember { mutableStateOf("") }
    var pob by remember { mutableStateOf("") }

    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedTime by remember { mutableStateOf<Calendar?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    var locationSearch by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it
                        selectedDate = cal
                        dob = sdfDate.format(cal.time)
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
                cal.set(Calendar.SECOND, 0)
                selectedTime = cal
                tob = sdfTime.format(cal.time)
                showTimePicker = false
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(TranslationUtils.translate("Add New Profile", lang), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = name, 
            onValueChange = { name = it }, 
            label = { Text(TranslationUtils.translate("Name", lang)) }, 
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = dob,
                    onValueChange = { },
                    label = { Text(TranslationUtils.translate("Birth Date", lang)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = Color.Transparent
                    )
                )
                Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = tob,
                    onValueChange = { },
                    label = { Text(TranslationUtils.translate("Birth Time", lang)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = Color.Transparent
                    )
                )
                Box(modifier = Modifier.matchParentSize().clickable { showTimePicker = true })
            }
        }

        val filteredLocations = locations.filter { it.name.contains(locationSearch, ignoreCase = true) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = if (expanded) locationSearch else pob,
                onValueChange = { 
                    locationSearch = it
                    if (!expanded) expanded = true
                },
                label = { Text(TranslationUtils.translate("Place of Birth", lang)) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                placeholder = { Text(TranslationUtils.translate("Search Location", lang)) }
            )
            
            if (filteredLocations.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filteredLocations.take(10).forEach { loc ->
                        DropdownMenuItem(
                            text = { Text(loc.name) },
                            onClick = {
                                pob = loc.name
                                locationSearch = loc.name
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text(TranslationUtils.translate("Cancel", lang)) }
            Button(
                onClick = { onAdd(name, dob, tob, pob) }, 
                enabled = name.isNotEmpty() && dob.isNotEmpty() && tob.isNotEmpty() && pob.isNotEmpty()
            ) {
                Text(TranslationUtils.translate("Add", lang))
            }
        }
    }
}


@Composable
fun SavedKundaliItem(
    meta: SavedKundaliMeta,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meta.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${meta.date}, ${meta.time} | ${meta.locationName ?: "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
