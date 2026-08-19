package com.hora.jnana.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hora.jnana.models.MatchMakingResponse
import com.hora.jnana.models.KootaInfo
import com.hora.jnana.ui.components.SavedKundalisDialog
import com.hora.jnana.utils.TranslationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchMakingScreen(
    navController: NavController,
    viewModel: MatchMakingViewModel,
    lang: String = "en",
    savePath: String? = null
) {
    val state by viewModel.state.collectAsState()
    
    var showGroomDialog by remember { mutableStateOf(false) }
    var showBrideDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(TranslationUtils.translate("Match Making", lang)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.result == null) {
                // Large selection boxes
                SelectionSection(
                    groom = state.groom,
                    bride = state.bride,
                    onSelectGroom = { showGroomDialog = true; viewModel.loadSavedFiles(savePath) },
                    onSelectBride = { showBrideDialog = true; viewModel.loadSavedFiles(savePath) },
                    lang = lang
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { viewModel.submit(lang) },
                    enabled = state.groom != null && state.bride != null && !state.isLoading,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text(TranslationUtils.translate("Submit", lang))
                }
            } else {
                // Minimized selection bar at top
                MinimizedSelectionBar(
                    groomName = state.groom?.name ?: "",
                    brideName = state.bride?.name ?: "",
                    onReset = { viewModel.resetResult() },
                    lang = lang
                )
                
                MatchResultContent(state.result!!, lang)
            }
            
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
        }
    }

    if (showGroomDialog) {
        SavedKundalisDialog(
            onDismiss = { showGroomDialog = false },
            savedKundalis = state.savedProfiles,
            isListing = state.isListingFiles,
            onSelect = { uri -> viewModel.selectGroom(uri) },
            onDelete = { /* Handle delete if needed */ },
            onAddProfile = { n, d, t, p -> viewModel.savePartialProfile(n, d, t, p, savePath) },
            lang = lang,
            locations = state.locations,
            onToggleAddForm = { if (it && state.locations.isEmpty()) viewModel.fetchLocations() }
        )
    }

    if (showBrideDialog) {
        SavedKundalisDialog(
            onDismiss = { showBrideDialog = false },
            savedKundalis = state.savedProfiles,
            isListing = state.isListingFiles,
            onSelect = { uri -> viewModel.selectBride(uri) },
            onDelete = { /* Handle delete if needed */ },
            onAddProfile = { n, d, t, p -> viewModel.savePartialProfile(n, d, t, p, savePath) },
            lang = lang,
            locations = state.locations,
            onToggleAddForm = { if (it && state.locations.isEmpty()) viewModel.fetchLocations() }
        )
    }
}

@Composable
fun SelectionSection(
    groom: com.hora.jnana.models.SavedKundali?,
    bride: com.hora.jnana.models.SavedKundali?,
    onSelectGroom: () -> Unit,
    onSelectBride: () -> Unit,
    lang: String
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SelectionBox(
            label = TranslationUtils.translate("Select Groom's Kundali", lang),
            selectedName = groom?.name,
            onClick = onSelectGroom,
            icon = Icons.Default.Person
        )
        SelectionBox(
            label = TranslationUtils.translate("Select Bride's Kundali", lang),
            selectedName = bride?.name,
            onClick = onSelectBride,
            icon = Icons.Default.PersonOutline
        )
    }
}

@Composable
fun SelectionBox(label: String, selectedName: String?, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Text(selectedName ?: TranslationUtils.translate("Not Selected", "en"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MinimizedSelectionBar(groomName: String, brideName: String, onReset: () -> Unit, lang: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("$groomName & $brideName", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onReset,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Text(TranslationUtils.translate("Change", lang))
            }
        }
    }
}

@Composable
fun MatchResultContent(result: MatchMakingResponse, lang: String) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${result.gunaMilan.totalPoints} / ${result.gunaMilan.maxPoints}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = result.gunaMilan.result,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = result.gunaMilan.summaryMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Kootas
        result.kootas.values.forEach { koota ->
            KootaCard(koota, lang)
        }
    }
}

@Composable
fun KootaCard(koota: KootaInfo, lang: String) {
    val valueFontWeight = if (lang == "kn") FontWeight.Normal else FontWeight.Bold
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = TranslationUtils.translate(koota.name, lang) + " " + TranslationUtils.translate("Koota", lang)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            PanchangaRow(TranslationUtils.translate("Obtained points", lang), "${koota.obtainedPoints} / ${koota.maxPoints}", valueFontWeight)
            PanchangaRow(TranslationUtils.translate("Groom Attribute", lang), koota.groomAttribute, valueFontWeight)
            PanchangaRow(TranslationUtils.translate("Bride Attribute", lang), koota.brideAttribute, valueFontWeight)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = koota.description,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

