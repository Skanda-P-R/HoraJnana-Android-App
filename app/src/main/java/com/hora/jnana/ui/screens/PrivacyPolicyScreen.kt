package com.hora.jnana.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController, lang: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (lang == "kn") "ಗೌಪ್ಯತಾ ನೀತಿ" else "Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (navController.currentDestination?.route == "privacy_policy") {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
                text = if (lang == "kn") "HoraJnana ಗೌಪ್ಯತಾ ನೀತಿ" else "Privacy Policy for HoraJnana",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            PolicySection(
                title = if (lang == "kn") "ಸ್ಥಳ ಡೇಟಾ" else "Location Data",
                content = if (lang == "kn") 
                    "ಪಂಚಾಂಗ ಮತ್ತು ಹೋರಾ ಲೆಕ್ಕಾಚಾರಗಳಿಗಾಗಿ ನಿಮ್ಮ ಸ್ಥಳವನ್ನು ಬಳಸಲಾಗುತ್ತದೆ. ಈ ಡೇಟಾವನ್ನು ನಿಮ್ಮ ಸಾಧನದಲ್ಲಿ ಮಾತ್ರ ಸಂಸ್ಕರಿಸಲಾಗುತ್ತದೆ ಮತ್ತು ನಮ್ಮ ಸರ್ವರ್‌ಗಳಿಗೆ ಎಂದಿಗೂ ಕಳುಹಿಸಲಾಗುವುದಿಲ್ಲ."
                    else "We use your location for accurate Panchanga and Hora calculations. This data is processed locally on your device and is NEVER sent to our servers."
            )

            PolicySection(
                title = if (lang == "kn") "ಲಾಗಿನ್ ಡೇಟಾ" else "Authentication",
                content = if (lang == "kn")
                    "ನಿಮ್ಮ ಸಾಧನದ ಅನನ್ಯ ಗುರುತಿಸುವಿಕೆಯನ್ನು ಬಳಸಿಕೊಂಡು ನಾವು ಸುರಕ್ಷಿತ ಪ್ರವೇಶವನ್ನು ಒದಗಿಸುತ್ತೇವೆ. ಯಾವುದೇ ವೈಯಕ್ತಿಕ ಮಾಹಿತಿಯನ್ನು ಸಂಗ್ರಹಿಸಲಾಗುವುದಿಲ್ಲ."
                    else "We use a unique device identifier for secure authentication. No personal information such as name or email is collected or stored."
            )

            PolicySection(
                title = if (lang == "kn") "ಡೇಟಾ ಸುರಕ್ಷತೆ" else "Data Security",
                content = if (lang == "kn")
                    "ಉಳಿಸಿದ ಕುಂಡಲಿ ಡೇಟಾವನ್ನು ನಿಮ್ಮ ಸಾಧನದಲ್ಲಿ ಎನ್‌ಕ್ರಿಪ್ಟ್ ಮಾಡಲಾಗುತ್ತದೆ."
                    else "Saved Kundali data is encrypted locally on your device."
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Last Updated: July 31, 2026",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun PolicySection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = content, style = MaterialTheme.typography.bodyMedium)
    }
}
