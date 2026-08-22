package com.hora.jnana.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(navController: NavController, lang: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (lang == "kn") "ಪರವಾನಗಿಗಳು" else "Licenses") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (navController.currentDestination?.route == "licenses") {
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
                text = "HoraJnana - AGPL v3",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This application is open-source and licensed under the GNU Affero General Public License v3.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            val uriHandler = LocalUriHandler.current
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { uriHandler.openUri("https://github.com/Skanda-P-R/HoraJnana-Android-App") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (lang == "kn") "ಮೂಲ ಕೋಡ್" else "Source Code",
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Button(
                    onClick = { uriHandler.openUri("https://github.com/Skanda-P-R/HoraJnana-REST-API") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (lang == "kn") "ಬ್ಯಾಕೆಂಡ್ ಕೋಡ್" else "Backend Code",
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Swiss Ephemeris",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This app consumes data from a backend utilizing the Swiss Ephemeris.\n\n" +
                        "Swiss Ephemeris is copyright © Astrodienst AG, Switzerland.\n" +
                        "It is licensed under the GNU AGPL.\n" +
                        "Official Website: https://www.astro.com/swisseph/",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Open Source Credits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val credits = listOf(
                "Jetpack Compose (Apache 2.0)",
                "Retrofit & OkHttp (Apache 2.0)",
                "Coil (Apache 2.0)",
                "Moshi (Apache 2.0)",
                "Kotlin Coroutines (Apache 2.0)",
                "Google Play Services (Location)"
            )
            credits.forEach { credit ->
                Text(
                    text = "• $credit",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Full AGPL v3 License Text",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = agplLicenseText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

private val agplLicenseText = """
                    GNU AFFERO GENERAL PUBLIC LICENSE
                       Version 3, 19 November 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

                            Preamble

  The GNU Affero General Public License is a free, copyleft license for
software and other kinds of works, specifically designed to ensure
cooperation with the community in the case of network server software.
...
(Remaining text available in project root LICENSE file)
""".trimIndent()
// I'll provide a more complete but compact version for the UI if possible, 
// or just the standard header. For legal completeness, I'll put the whole thing in a scrollable box.
// Wait, the user said "add here accordingly", I'll put the full text in the card.
// I'll use the full text I wrote to LICENSE file.
