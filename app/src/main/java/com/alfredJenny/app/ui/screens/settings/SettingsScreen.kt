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
    var providerExpanded by remember { mutableStateOf(false) }
    var showAiKey by remember { mutableStateOf(false) }
    var showElevenKey by remember { mutableStateOf(false) }

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
            Text("Impostazioni", style = MaterialTheme.typography.titleLarge, color = OnBackground, modifier = Modifier.weight(1f))
            if (state.isSaved) Icon(Icons.Default.Check, contentDescription = "Salvato", tint = SuccessGreen)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Backend URL ───────────────────────────────────────────────────
            SectionLabel("Backend")
            OutlinedTextField(
                value = state.preferences.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                label = { Text("URL server") },
                placeholder = { Text(DEFAULT_BASE_URL, color = OnSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors(),
                singleLine = true
            )
            Text("Default: $DEFAULT_BASE_URL  •  Produzione: https://tuo-backend.railway.app", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)

            HorizontalDivider(color = SurfaceVariant)

            // ── AI Provider ───────────────────────────────────────────────────
            SectionLabel("Provider AI")
            ExposedDropdownMenuBox(expanded = providerExpanded, onExpandedChange = { providerExpanded = it }) {
                OutlinedTextField(
                    value = state.preferences.aiProvider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = outlinedColors()
                )
                ExposedDropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }, modifier = Modifier.background(SurfaceVariant)) {
                    AIProvider.entries.forEach { p ->
                        DropdownMenuItem(text = { Text(p.displayName, color = OnBackground) }, onClick = { viewModel.onProviderChange(p); providerExpanded = false })
                    }
                }
            }

            // AI API Key
            OutlinedTextField(
                value = state.preferences.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                label = { Text("API Key AI") },
                placeholder = { Text("sk-...", color = OnSurfaceVariant) },
                visualTransformation = if (showAiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showAiKey = !showAiKey }) { Text(if (showAiKey) "Nascondi" else "Mostra", color = AlfredBlueLight, fontSize = MaterialTheme.typography.labelSmall.fontSize) } },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors(),
                singleLine = true
            )

            HorizontalDivider(color = SurfaceVariant)

            // ── Voice / ElevenLabs ────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Sintesi vocale (ElevenLabs)")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = state.preferences.voiceEnabled,
                    onCheckedChange = viewModel::onVoiceEnabledChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = AlfredBlue, checkedThumbColor = AlfredBlueLight)
                )
            }

            OutlinedTextField(
                value = state.preferences.elevenLabsApiKey,
                onValueChange = viewModel::onElevenLabsKeyChange,
                label = { Text("ElevenLabs API Key") },
                placeholder = { Text("sk_...", color = OnSurfaceVariant) },
                visualTransformation = if (showElevenKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showElevenKey = !showElevenKey }) { Text(if (showElevenKey) "Nascondi" else "Mostra", color = AlfredBlueLight, fontSize = MaterialTheme.typography.labelSmall.fontSize) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.preferences.voiceEnabled,
                colors = outlinedColors(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.preferences.voiceId,
                onValueChange = viewModel::onVoiceIdChange,
                label = { Text("Voice ID") },
                placeholder = { Text("pNInz6obpgDQGcFmaJgB  (Adam)", color = OnSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.preferences.voiceEnabled,
                colors = outlinedColors(),
                singleLine = true
            )

            Text(
                "Trovi i Voice ID su elevenlabs.io/voice-library.\n" +
                "Adam (default): pNInz6obpgDQGcFmaJgB",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )

            // ── Save ──────────────────────────────────────────────────────────
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
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
    unfocusedTextColor = OnBackground,
    disabledBorderColor = SurfaceVariant,
    disabledTextColor = OnSurfaceVariant,
    disabledLabelColor = OnSurfaceVariant
)
