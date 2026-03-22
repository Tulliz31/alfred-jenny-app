package com.alfredJenny.app.ui.screens.jenny

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.model.OpenRouterModel
import com.alfredJenny.app.ui.theme.*

@Composable
fun JennyAIScreen(
    onBack: () -> Unit,
    viewModel: JennyAIConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar
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
                "AI Dedicata Jenny",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.weight(1f)
            )
            if (state.isSaved) {
                Icon(Icons.Default.Check, contentDescription = "Salvato", tint = SuccessGreen,
                    modifier = Modifier.padding(end = 8.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Enable toggle ─────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (state.enabled) JennyPurple.copy(alpha = 0.15f) else SurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (state.enabled) JennyPurple else SurfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Usa AI separata per Jenny", color = OnBackground, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (state.enabled) "Jenny usa il provider configurato qui sotto"
                            else "Jenny usa lo stesso provider AI di Alfred",
                            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = viewModel::onEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = JennyPurple,
                            checkedThumbColor = JennyPurpleLight
                        )
                    )
                }
            }

            AnimatedVisibility(visible = state.enabled, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

                    // ── Provider type ─────────────────────────────────────────
                    ProviderTypeSelector(
                        selected = state.providerType,
                        onSelect = viewModel::onProviderTypeChange,
                    )

                    HorizontalDivider(color = SurfaceVariant)

                    // ── API Key ───────────────────────────────────────────────
                    var showKey by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            if (state.providerType == "openrouter") "OpenRouter API Key"
                            else "API Key (opzionale per Ollama)",
                            fontWeight = FontWeight.SemiBold,
                            color = JennyPurpleLight, fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.providerType == "openrouter") {
                            IconButton(onClick = viewModel::showKeyInfo, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.HelpOutline, contentDescription = "Info",
                                    tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = viewModel::onApiKeyChange,
                        label = { Text("API Key") },
                        placeholder = { Text("sk-or-...", color = OnSurfaceVariant) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showKey = !showKey }) {
                                Text(if (showKey) "Nascondi" else "Mostra", color = JennyPurpleLight, fontSize = 12.sp)
                            }
                        },
                        colors = jennyOutlinedColors()
                    )

                    // ── Custom URL (only for "custom" provider type) ───────────
                    if (state.providerType == "custom") {
                        OutlinedTextField(
                            value = state.baseUrl,
                            onValueChange = viewModel::onBaseUrlChange,
                            label = { Text("URL base") },
                            placeholder = { Text("http://localhost:11434/v1", color = OnSurfaceVariant) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = jennyOutlinedColors()
                        )
                    }

                    HorizontalDivider(color = SurfaceVariant)

                    // ── Model selection ───────────────────────────────────────
                    Text("Modello AI", fontWeight = FontWeight.SemiBold, color = JennyPurpleLight, fontSize = 14.sp)

                    if (state.providerType == "openrouter") {
                        // Load model list button
                        if (state.models.isEmpty() && !state.isLoadingModels) {
                            OutlinedButton(
                                onClick = viewModel::loadOpenRouterModels,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, JennyPurpleLight)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null,
                                    tint = JennyPurpleLight, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Carica lista modelli OpenRouter", color = JennyPurpleLight,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (state.isLoadingModels) {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = JennyPurpleLight, modifier = Modifier.size(28.dp))
                            }
                        }

                        state.modelsError?.let { err ->
                            Surface(shape = RoundedCornerShape(8.dp), color = ErrorRed.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()) {
                                Text(err, modifier = Modifier.padding(12.dp), color = ErrorRed,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (state.models.isNotEmpty()) {
                            // Search bar
                            OutlinedTextField(
                                value = state.modelFilter,
                                onValueChange = viewModel::onModelFilterChange,
                                placeholder = { Text("Filtra modelli…", color = OnSurfaceVariant) },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceVariant) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = jennyOutlinedColors()
                            )

                            // Model list (non-lazy, scrollable within the outer scroll)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                state.filteredModels.forEach { model ->
                                    ModelCard(
                                        model = model,
                                        isSelected = model.id == state.modelId,
                                        onClick = { viewModel.onModelIdChange(model.id) },
                                    )
                                }
                            }
                        }
                    } else {
                        // Custom: manual model ID field
                        OutlinedTextField(
                            value = state.modelId,
                            onValueChange = viewModel::onModelIdChange,
                            label = { Text("Model ID") },
                            placeholder = { Text("mistral, llama3, gemma…", color = OnSurfaceVariant) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = jennyOutlinedColors()
                        )
                    }

                    if (state.modelId.isNotBlank() && state.providerType == "openrouter") {
                        Surface(shape = RoundedCornerShape(8.dp), color = JennyPurple.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = JennyPurpleLight, modifier = Modifier.size(16.dp))
                                Text("Modello selezionato: ${state.modelId}",
                                    color = JennyPurpleLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    HorizontalDivider(color = SurfaceVariant)

                    // ── Test ──────────────────────────────────────────────────
                    OutlinedButton(
                        onClick = viewModel::testJenny,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !state.isTesting,
                        border = androidx.compose.foundation.BorderStroke(1.dp, JennyPurpleLight)
                    ) {
                        if (state.isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp),
                                color = JennyPurpleLight, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Test in corso…", color = JennyPurpleLight)
                        } else {
                            Icon(Icons.Default.PlayArrow, null, tint = JennyPurpleLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Test risposta Jenny", color = JennyPurpleLight, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    state.testError?.let { err ->
                        Surface(shape = RoundedCornerShape(8.dp), color = ErrorRed.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.dismissTestResult() }) {
                            Text("❌ $err", modifier = Modifier.padding(12.dp), color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // ── Save ─────────────────────────────────────────────────────────
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JennyPurple)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Salva configurazione", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Test result dialog ────────────────────────────────────────────────────
    if (state.testReply != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissTestResult,
            title = { Text("Risposta Jenny", color = OnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.testReply!!, color = OnBackground)
                    HorizontalDivider(color = SurfaceVariant)
                    state.testProvider?.let { p ->
                        Text("Provider: $p", color = OnSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissTestResult) {
                    Text("Chiudi", color = JennyPurpleLight)
                }
            },
            containerColor = Surface,
        )
    }

    // ── OpenRouter info dialog ────────────────────────────────────────────────
    if (state.showKeyInfoDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissKeyInfo,
            title = { Text("Come ottenere la API key OpenRouter", color = OnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "1. Vai su openrouter.ai",
                        "2. Crea un account gratuito",
                        "3. Vai su Keys → Create Key",
                        "4. Incolla la key nel campo API Key",
                        "5. Hai credito gratuito iniziale per testare",
                    ).forEach { step ->
                        Text(step, color = OnBackground, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = JennyPurple.copy(alpha = 0.12f)) {
                        Text(
                            "OpenRouter ti dà accesso a decine di modelli AI " +
                            "(Mistral, LLaMA, Claude, Gemini…) con una sola key.",
                            modifier = Modifier.padding(10.dp),
                            color = JennyPurpleLight,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissKeyInfo) {
                    Text("Capito", color = JennyPurpleLight)
                }
            },
            containerColor = Surface,
        )
    }
}

// ── Provider type selector ────────────────────────────────────────────────────

@Composable
private fun ProviderTypeSelector(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Provider", fontWeight = FontWeight.SemiBold, color = JennyPurpleLight, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProviderTypeCard(
                label = "OpenRouter",
                subtitle = "Consigliato — molti modelli, 1 key",
                isSelected = selected == "openrouter",
                onClick = { onSelect("openrouter") },
                modifier = Modifier.weight(1f)
            )
            ProviderTypeCard(
                label = "URL Custom",
                subtitle = "Ollama locale, LM Studio, ecc.",
                isSelected = selected == "custom",
                onClick = { onSelect("custom") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProviderTypeCard(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) JennyPurple.copy(alpha = 0.18f) else SurfaceVariant,
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) JennyPurpleLight else SurfaceVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isSelected) {
                    Icon(Icons.Default.RadioButtonChecked, null, tint = JennyPurpleLight, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.RadioButtonUnchecked, null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Text(label, color = if (isSelected) JennyPurpleLight else OnBackground,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
            }
            Text(subtitle, color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ── Model card ────────────────────────────────────────────────────────────────

@Composable
private fun ModelCard(
    model: OpenRouterModel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = when {
            isSelected -> JennyPurple.copy(alpha = 0.2f)
            else       -> SurfaceVariant
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) JennyPurpleLight else SurfaceVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        model.modelName,
                        color = if (isSelected) JennyPurpleLight else OnBackground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    if (model.isFree) {
                        ModelBadge("Gratuito", SuccessGreen)
                    } else if (model.isRecommended) {
                        ModelBadge("Consigliato", JennyPurpleLight)
                    }
                }
                Text(
                    model.providerLabel,
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                if (model.description.isNotBlank()) {
                    Text(
                        model.description.take(80).let { if (model.description.length > 80) "$it…" else it },
                        color = OnSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!model.isFree) {
                    Text(
                        "${"%.2f".format(model.promptCostPer1M)}$/1M",
                        color = OnSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
                if (model.contextLength > 0) {
                    Text(
                        "${formatContext(model.contextLength)}K ctx",
                        color = OnSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = JennyPurpleLight, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ModelBadge(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(label, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatContext(ctx: Int): String {
    return if (ctx >= 1000) "${ctx / 1000}" else "$ctx"
}

// ── Outlined field colors for Jenny purple theme ──────────────────────────────

@Composable
private fun jennyOutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = JennyPurpleLight,
    unfocusedBorderColor = OnSurfaceVariant,
    focusedLabelColor = JennyPurpleLight,
    cursorColor = JennyPurpleLight,
    focusedTextColor = OnBackground,
    unfocusedTextColor = OnBackground,
    disabledBorderColor = SurfaceVariant,
    disabledTextColor = OnSurfaceVariant,
    disabledLabelColor = OnSurfaceVariant,
)
