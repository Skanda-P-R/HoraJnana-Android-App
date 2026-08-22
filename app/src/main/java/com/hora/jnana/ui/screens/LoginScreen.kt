package com.hora.jnana.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hora.jnana.security.DeviceUuidProvider
import com.hora.jnana.ui.login.LoginUiState
import com.hora.jnana.ui.login.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    uuidProvider: DeviceUuidProvider,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to HoraJnana",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Get started to access your astrological data.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 48.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Button(
            onClick = {
                val uuid = uuidProvider.getDeviceUuid()
                viewModel.login(uuid, onLoginSuccess)
            },
            enabled = uiState !is LoginUiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (uiState is LoginUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        if (uiState is LoginUiState.Error) {
            PersistentErrorBox(
                error = (uiState as LoginUiState.Error).message,
                lang = "en", // Default to English for login
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
