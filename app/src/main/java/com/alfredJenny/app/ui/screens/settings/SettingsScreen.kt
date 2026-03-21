package com.alfredJenny.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.remote.DEFAULT_BASE_URL
import com.alfredJenny.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var providerDropdownExpanded by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = OnBackground)
            }
            Text(
                "Impostazioni",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.weight(1f)
            )
            if (state.isSaved) {
                Icon(Icons.Default.Check, contentDescription = "Salvato", tint = SuccessGreen)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Backend URL ───────────────────────────────────────────────────
            SectionLabel("URL Backend")

            OutlinedTextField(
                value = state.preferences.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                label = { Text("Indirizzo server") },
                placeholder = { Text(DEFAULT_BASE_URL, color = OnSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors(),
                singleLine = true
            )
            Text(
                "Default: $DEFAULT_BASE_URL  (emulatore → localhost)\n" +
                "Produzione: https://tuo-backend.railway.app",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )

            HorizontalDivider(color = SurfaceVariant)

            // ── Provider AI ───────────────────────────────────────────────────
            SectionLabel("Provider AI (backend)")

            ExposedDropdownMenuBox(
                expanded = providerDropdownExpanded,
                onExpandedChange = { providerDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.preferences.aiProvider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Seleziona provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = outlinedColors()
                )
                ExposedDropdownMenu(
                    expanded = providerDropdownExpanded,
                    onDismissRequest = { providerDropdownExpanded = false },
                    modifier = Modifier.background(SurfaceVariant)
                ) {
                    AIProvider.entries.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.displayName, color = OnBackground) },
                            onClick = {
                                viewModel.onProviderChange(provider)
                                providerDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = SurfaceVariant)

            // ── API Key ───────────────────────────────────────────────────────
            SectionLabel("API Key")

            OutlinedTextField(
                value = state.preferences.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                label = { Text("API Key") },
                placeholder = { Text("sk-...", color = OnSurfaceVariant) },
                visualTransformation = if (showApiKey) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showApiKey = !showApiKey }) {
                        Text(
                            if (showApiKey) "Nascondi" else "Mostra",
                            color = AlfredBlueLight,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors(),
                singleLine = true
            )
            Text(
                "La chiave viene salvata localmente sul dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )

            // ── Save ──────────────────────────────────────────────────────────
            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlfredBlue)
            ) {
                Text("Salva impostazioni", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, color = OnSurface)
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AlfredBlueLight,
    unfocusedBorderColor = OnSurfaceVariant,
    focusedLabelColor = AlfredBlueLight,
    cursorColor = AlfredBlueLight,
    focusedTextColor = OnBackground,
    unfocusedTextColor = OnBackground
)
