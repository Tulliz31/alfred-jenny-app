package com.alfredJenny.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.model.OpenRouterModel
import com.alfredJenny.app.ui.theme.*

private val PROVIDER_OPTIONS = listOf(
    Triple("openai",     "OpenAI",     "GPT-4o e altri modelli OpenAI"),
    Triple("anthropic",  "Anthropic",  "Claude 3.5 Sonnet e altri"),
    Triple("gemini",     "Gemini",     "Google Gemini 1.5 Pro e altri"),
    Triple("openrouter", "OpenRouter", "Accesso a 100+ modelli tramite un'unica API"),
    Triple("custom",     "Custom",     "Qualsiasi endpoint OpenAI-compatibile (Ollama, LM Studio…)"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIProviderConfigScreen(
    onBack: () -> Unit,
    viewModel: AIProviderConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isJenny = state.companionId == "jenny"
    val title = if (isJenny) "AI Dedicata Jenny" else "AI Dedicata Alfred"
    val accentColor = if (isJenny) JennyPurple else AlfredBlue
    val accentLight = if (isJenny) JennyPurpleLight else AlfredBlueLight

    // Model browser bottom sheet
    if (state.showModelBrowser) {
        ModelBrowserSheet(
            state = state,
            onDismiss = viewModel::dismissModelBrowser,
            onSelectModel = { model ->
                viewModel.onModelIdChange(model.id)
                viewModel.dismissModelBrowser()
            },
            onFilterChange = viewModel::onModelFilterChange,
            onLoad = viewModel::loadOpenRouterModels,
        )
    }

    // Test result dialog
    if (state.testReply != null || state.testError != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissTestResult,
            title = {
                Text(
                    if (state.testReply != null) "Test riuscito" else "Test fallito",
                    color = if (state.testReply != null) SuccessGreen else ErrorRed,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.testReply != null) {
                        Text("Provider: ${state.testProvider ?: ""}", color = OnSurfaceVariant, fontSize = 12.sp)
                        Text(state.testReply!!, color = OnBackground)
                    } else {
                        Text(state.testError ?: "Errore sconosciuto", color = ErrorRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissTestResult) { Text("OK", color = accentLight) }
            },
            containerColor = Surface,
        )
    }

    // Save error snackbar-like dialog
    if (state.saveError != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSaveError,
            title = { Text("Errore salvataggio", color = ErrorRed) },
            text = { Text(state.saveError!!, color = OnBackground) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissSaveError) { Text("OK", color = accentLight) }
            },
            containerColor = Surface,
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().background(Surface).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = OnBackground)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = OnBackground, modifier = Modifier.weight(1f))
            if (state.isSaved) {
                Icon(Icons.Default.Check, contentDescription = "Salvato", tint = SuccessGreen)
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentLight)
            }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.loadError != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = ErrorRed.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Text(state.loadError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // ── Enabled toggle ────────────────────────────────────────────────
            ConfigLabel("Stato")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Abilita provider dedicato", color = OnBackground, fontWeight = FontWeight.Medium)
                    Text(
                        "Usa questo provider invece del provider backend globale",
                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.enabled,
                    onCheckedChange = viewModel::onEnabledChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor, checkedThumbColor = accentLight),
                )
            }

            // ── Jenny use_global toggle ───────────────────────────────────────
            if (isJenny) {
                HorizontalDivider(color = SurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Usa config di Alfred", color = OnBackground, fontWeight = FontWeight.Medium)
                        Text("Se attivo, Jenny usa lo stesso provider configurato per Alfred", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                    Switch(
                        checked = state.useGlobal,
                        onCheckedChange = viewModel::onUseGlobalChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = accentColor, checkedThumbColor = accentLight),
                    )
                }
                if (state.useGlobal) {
                    Surface(shape = RoundedCornerShape(8.dp), color = accentColor.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Jenny userà la configurazione AI di Alfred.",
                            modifier = Modifier.padding(12.dp),
                            color = accentLight,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    return@Column  // hide provider config when use_global is true
                }
            }

            HorizontalDivider(color = SurfaceVariant)

            // ── Provider selector ─────────────────────────────────────────────
            ConfigLabel("Provider")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PROVIDER_OPTIONS.forEach { (id, name, desc) ->
                    ProviderOptionRow(
                        id = id, name = name, description = desc,
                        selected = state.providerType == id,
                        accentColor = accentColor,
                        onClick = { viewModel.onProviderTypeChange(id) },
                    )
                }
            }

            HorizontalDivider(color = SurfaceVariant)

            // ── API Key ───────────────────────────────────────────────────────
            var showKey by remember { mutableStateOf(false) }
            val apiKeyLabel = when (state.providerType) {
                "openai"     -> "OpenAI API Key"
                "anthropic"  -> "Anthropic API Key"
                "gemini"     -> "Google API Key"
                "openrouter" -> "OpenRouter API Key"
                else         -> "API Key (opzionale)"
            }
            ConfigLabel(apiKeyLabel)
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                placeholder = { Text("sk-...", color = OnSurfaceVariant) },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "Nascondi" else "Mostra", color = accentLight, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors(),
                singleLine = true,
            )

            // ── Model ID ──────────────────────────────────────────────────────
            ConfigLabel("Modello")
            val modelHint = when (state.providerType) {
                "openai"     -> "gpt-4o"
                "anthropic"  -> "claude-3-5-sonnet-20241022"
                "gemini"     -> "gemini-1.5-pro"
                "openrouter" -> "meta-llama/llama-3.1-70b-instruct"
                else         -> "nome-modello"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.modelId,
                    onValueChange = viewModel::onModelIdChange,
                    placeholder = { Text(modelHint, color = OnSurfaceVariant) },
                    modifier = Modifier.weight(1f),
                    colors = outlinedColors(),
                    singleLine = true,
                )
                if (state.providerType == "openrouter") {
                    OutlinedButton(
                        onClick = {
                            viewModel.showModelBrowser()
                            if (state.models.isEmpty()) viewModel.loadOpenRouterModels()
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentLight),
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = accentLight, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sfoglia", color = accentLight, fontSize = 12.sp)
                    }
                }
            }

            // ── Base URL (custom only) ────────────────────────────────────────
            if (state.providerType == "custom") {
                ConfigLabel("Base URL")
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = viewModel::onBaseUrlChange,
                    placeholder = { Text("http://localhost:11434/v1", color = OnSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedColors(),
                    singleLine = true,
                )
                Text(
                    "Endpoint OpenAI-compatibile (Ollama: /v1, LM Studio: /v1)",
                    style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant,
                )
            }

            HorizontalDivider(color = SurfaceVariant)

            // ── Actions ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel::testConfig,
                    enabled = !state.isTesting && !state.isSaving,
                    modifier = Modifier.weight(1f).height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentLight),
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentLight, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accentLight, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Test", color = accentLight, fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving && !state.isTesting,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentLight, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, tint = OnBackground, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Salva", color = OnBackground, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Provider option row ───────────────────────────────────────────────────────

@Composable
private fun ProviderOptionRow(
    id: String,
    name: String,
    description: String,
    selected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accentColor.copy(alpha = 0.18f) else SurfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = accentColor),
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = OnBackground, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                Text(description, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── Model browser bottom sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelBrowserSheet(
    state: AIProviderConfigUiState,
    onDismiss: () -> Unit,
    onSelectModel: (OpenRouterModel) -> Unit,
    onFilterChange: (String) -> Unit,
    onLoad: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Seleziona modello OpenRouter", style = MaterialTheme.typography.titleMedium, color = OnBackground)

            OutlinedTextField(
                value = state.modelFilter,
                onValueChange = onFilterChange,
                placeholder = { Text("Cerca modello...", color = OnSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors(),
                singleLine = true,
            )

            when {
                state.isLoadingModels -> Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AlfredBlueLight)
                }
                state.modelsError != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.modelsError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onLoad, modifier = Modifier.fillMaxWidth()) {
                        Text("Riprova", color = AlfredBlueLight)
                    }
                }
                state.filteredModels.isEmpty() && state.models.isEmpty() -> OutlinedButton(
                    onClick = onLoad,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlfredBlueLight),
                ) {
                    Text("Carica modelli", color = AlfredBlueLight)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.filteredModels, key = { it.id }) { model ->
                        ModelRow(model = model, onClick = { onSelectModel(model) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelRow(model: OpenRouterModel, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(model.modelName, color = OnBackground, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(model.providerLabel, color = OnSurfaceVariant, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (model.isFree) {
                    Surface(shape = RoundedCornerShape(4.dp), color = SuccessGreen.copy(alpha = 0.2f)) {
                        Text("Free", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (model.isRecommended) {
                    Surface(shape = RoundedCornerShape(4.dp), color = AlfredBlue.copy(alpha = 0.2f)) {
                        Text("★", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = AlfredBlueLight, fontSize = 10.sp)
                    }
                }
                if (model.contextLength > 0) {
                    val ctx = if (model.contextLength >= 1000) "${model.contextLength / 1000}k" else "${model.contextLength}"
                    Text(ctx, color = OnSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun ConfigLabel(text: String) {
    Text(text, color = OnSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AlfredBlueLight,
    unfocusedBorderColor = SurfaceVariant,
    focusedLabelColor = AlfredBlueLight,
    cursorColor = AlfredBlueLight,
    focusedTextColor = OnBackground,
    unfocusedTextColor = OnBackground,
)
