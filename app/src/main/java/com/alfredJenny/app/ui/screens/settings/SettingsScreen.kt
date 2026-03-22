package com.alfredJenny.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.model.ProviderInfo
import com.alfredJenny.app.data.remote.DEFAULT_BASE_URL
import com.alfredJenny.app.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SettingsSection(val label: String, val icon: ImageVector) {
    GENERALE("Generale", Icons.Default.Tune),
    PROVIDER_AI("Provider AI", Icons.Default.SmartToy),
    VOCE("Voce", Icons.Default.RecordVoiceOver),
    MEMORIA("Memoria", Icons.Default.Psychology),
    AVANZATE("Avanzate", Icons.Default.Code),
    ACCOUNT("Account", Icons.Default.Person),
    SMART_HOME("Smart Home", Icons.Default.Lightbulb),
    SERVIZIO("Servizio", Icons.Default.Build)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAvatarImport: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }

    val scope = rememberCoroutineScope()
    var tapCount by remember { mutableIntStateOf(0) }
    var resetJob by remember { mutableStateOf<Job?>(null) }

    fun onSettingsIconTap() {
        tapCount++
        resetJob?.cancel()
        resetJob = scope.launch { delay(1500); tapCount = 0 }
        if (tapCount >= 3) { tapCount = 0; resetJob?.cancel(); viewModel.onTripleTap() }
    }

    if (selectedSection == null) {
        Column(modifier = Modifier.fillMaxSize().background(Background)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = OnBackground)
                }
                Text("Impostazioni", style = MaterialTheme.typography.titleLarge, color = OnBackground,
                    modifier = Modifier.weight(1f))
                if (state.isSaved) Icon(Icons.Default.Check, contentDescription = "Salvato", tint = SuccessGreen)
                IconButton(onClick = { onSettingsIconTap() }) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = OnSurfaceVariant)
                }
            }

            AnimatedVisibility(visible = state.showPasswordField, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(SurfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var showPwd by remember { mutableStateOf(false) }
                    Text("Accesso area tecnica", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.passwordInput,
                            onValueChange = viewModel::onPasswordChange,
                            placeholder = { Text("Password", color = OnSurfaceVariant, fontSize = 13.sp) },
                            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPwd = !showPwd }) {
                                    Icon(if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null, tint = OnSurfaceVariant)
                                }
                            },
                            isError = state.passwordError != null,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                        if (state.isVerifyingPassword) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = AlfredBlueLight, strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = viewModel::unlockServiceSection,
                                modifier = Modifier.size(48.dp).background(AlfredBlue, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Sblocca", tint = OnBackground)
                            }
                        }
                        IconButton(onClick = viewModel::dismissPasswordField) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = OnSurfaceVariant)
                        }
                    }
                    if (state.passwordError != null) {
                        Text(state.passwordError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val visibleSections = SettingsSection.entries.filter {
                    when (it) {
                        SettingsSection.SERVIZIO   -> state.serviceSectionUnlocked
                        SettingsSection.SMART_HOME -> state.serviceSectionUnlocked
                        else -> true
                    }
                }
                visibleSections.forEach { section ->
                    SectionCard(
                        section = section,
                        badge = if (section == SettingsSection.PROVIDER_AI)
                            state.providers.firstOrNull { it.active }?.name else null,
                        onClick = { selectedSection = section }
                    )
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().background(Background)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedSection = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = OnBackground)
                }
                Text(selectedSection!!.label, style = MaterialTheme.typography.titleLarge, color = OnBackground,
                    modifier = Modifier.weight(1f))
                if (selectedSection == SettingsSection.SERVIZIO) {
                    Surface(shape = RoundedCornerShape(12.dp), color = SuccessGreen.copy(alpha = 0.2f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(SuccessGreen, CircleShape))
                            Text("Servizio", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                        }
                    }
                }
                if (state.isSaved) Icon(Icons.Default.Check, contentDescription = "Salvato", tint = SuccessGreen)
            }

            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (selectedSection) {
                    SettingsSection.GENERALE    -> GeneraleSection(state, viewModel, onOpenAvatarImport)
                    SettingsSection.PROVIDER_AI -> ProviderAiSection(state, viewModel)
                    SettingsSection.VOCE        -> VoceSection(state, viewModel)
                    SettingsSection.MEMORIA     -> MemoriaSection(state, viewModel)
                    SettingsSection.AVANZATE    -> AvanzateSection(state, viewModel)
                    SettingsSection.ACCOUNT     -> AccountSection(state, onLogout)
                    SettingsSection.SMART_HOME  -> SmartHomeAdminSection(state, viewModel)
                    SettingsSection.SERVIZIO    -> ServizioSection(state, viewModel)
                    null -> {}
                }
            }
        }
    }

    if (state.showClearJennyDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearJennyDialog,
            title = { Text("Cancella conversazioni Jenny", color = OnBackground) },
            text = { Text("Tutte le conversazioni con Jenny verranno eliminate.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmClearJennyConversations) {
                    Text("Cancella", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearJennyDialog) {
                    Text("Annulla", color = OnSurfaceVariant)
                }
            },
            containerColor = Surface
        )
    }

    if (state.jennyConversationsCleared) {
        LaunchedEffect(Unit) { delay(2000); viewModel.dismissClearedNotice() }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(shape = RoundedCornerShape(8.dp), color = SuccessGreen.copy(alpha = 0.9f),
                modifier = Modifier.padding(16.dp)) {
                Text("Conversazioni Jenny cancellate", modifier = Modifier.padding(12.dp), color = OnBackground)
            }
        }
    }

    if (state.providerError != null) {
        LaunchedEffect(state.providerError) { delay(3000); viewModel.dismissProviderError() }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(shape = RoundedCornerShape(8.dp), color = ErrorRed.copy(alpha = 0.9f),
                modifier = Modifier.padding(16.dp)) {
                Text(state.providerError!!, modifier = Modifier.padding(12.dp), color = OnBackground)
            }
        }
    }
}

// ── Section list card ─────────────────────────────────────────────────────────

private val SmartHomeAmberLight = androidx.compose.ui.graphics.Color(0xFFFFA726)

@Composable
private fun SectionCard(section: SettingsSection, badge: String?, onClick: () -> Unit) {
    val isServizio = section == SettingsSection.SERVIZIO
    val isSmartHome = section == SettingsSection.SMART_HOME
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isServizio  -> JennyPurple.copy(alpha = 0.15f)
            isSmartHome -> SmartHomeAmberLight.copy(alpha = 0.1f)
            else        -> SurfaceVariant
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                section.icon, contentDescription = null,
                tint = when {
                    isServizio  -> JennyPurpleLight
                    isSmartHome -> SmartHomeAmberLight
                    else        -> AlfredBlueLight
                },
                modifier = Modifier.size(24.dp)
            )
            Text(section.label, color = OnBackground, fontWeight = FontWeight.Medium,
                fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (badge != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = AlfredBlue.copy(alpha = 0.25f)) {
                    Text(badge, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = AlfredBlueLight, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(4.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
        }
    }
}

// ── Generale ──────────────────────────────────────────────────────────────────

@Composable
private fun GeneraleSection(state: SettingsUiState, viewModel: SettingsViewModel, onOpenAvatarImport: () -> Unit) {
    SectionLabel("Backend")
    OutlinedTextField(
        value = state.preferences.baseUrl,
        onValueChange = viewModel::onBaseUrlChange,
        label = { Text("URL server") },
        placeholder = { Text(DEFAULT_BASE_URL, color = OnSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(), colors = outlinedColors(), singleLine = true
    )
    Text("Default: $DEFAULT_BASE_URL", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Avatar Alfred")
    Text("Sostituisci i frame dell'avatar animato con grafica personalizzata.",
        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    OutlinedButton(
        onClick = onOpenAvatarImport,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlfredBlueLight)
    ) {
        Icon(Icons.Default.Face, contentDescription = null, tint = AlfredBlueLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sostituisci avatar", fontWeight = FontWeight.SemiBold, color = AlfredBlueLight)
    }
    SaveButton(viewModel)
}

// ── Provider AI ───────────────────────────────────────────────────────────────

@Composable
private fun ProviderAiSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    var showAiKey by remember { mutableStateOf(false) }

    // Backend provider cards
    SectionLabel("Provider attivo (backend)")
    Text("Seleziona il modello AI usato dal backend. Richiede ruolo admin.",
        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)

    if (state.isLoadingProviders) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AlfredBlueLight, modifier = Modifier.size(28.dp))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.providers.forEach { provider ->
                ProviderCard(
                    provider = provider,
                    isLoading = state.isSettingProvider,
                    onClick = { viewModel.setActiveProvider(provider.id) }
                )
            }
        }
        if (state.providers.isEmpty()) {
            Surface(shape = RoundedCornerShape(8.dp), color = SurfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Text("Nessun provider disponibile — verifica la connessione al backend.",
                    modifier = Modifier.padding(12.dp), color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = viewModel::loadProviders) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Aggiorna", color = AlfredBlueLight, fontSize = 13.sp)
            }
        }
    }

    HorizontalDivider(color = SurfaceVariant)

    SectionLabel("Chiave API locale (opzionale)")
    Text("Usata solo se il backend è configurato per delegare l'auth all'app.",
        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    OutlinedTextField(
        value = state.preferences.apiKey,
        onValueChange = viewModel::onApiKeyChange,
        label = { Text("API Key AI") },
        placeholder = { Text("sk-...", color = OnSurfaceVariant) },
        visualTransformation = if (showAiKey) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { showAiKey = !showAiKey }) {
                Text(if (showAiKey) "Nascondi" else "Mostra", color = AlfredBlueLight,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        },
        modifier = Modifier.fillMaxWidth(), colors = outlinedColors(), singleLine = true
    )
    SaveButton(viewModel)
}

@Composable
private fun ProviderCard(provider: ProviderInfo, isLoading: Boolean, onClick: () -> Unit) {
    val borderColor = if (provider.active) AlfredBlueLight else SurfaceVariant
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (provider.active) AlfredBlue.copy(alpha = 0.12f) else SurfaceVariant,
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !provider.active && !isLoading, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(provider.name, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (provider.active) {
                            Surface(shape = RoundedCornerShape(6.dp),
                                color = SuccessGreen.copy(alpha = 0.2f)) {
                                Text("ATTIVO", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(provider.description, color = OnSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (!provider.active) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp),
                            color = AlfredBlueLight, strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = onClick) {
                            Text("Seleziona", color = AlfredBlueLight, fontSize = 13.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProviderStat(
                    icon = Icons.Default.AttachMoney,
                    label = "In: $${String.format("%.3f", provider.pricePerKInput)}/1K"
                )
                ProviderStat(
                    icon = Icons.Default.AttachMoney,
                    label = "Out: $${String.format("%.3f", provider.pricePerKOutput)}/1K"
                )
                ProviderStat(
                    icon = Icons.Default.Speed,
                    label = "~${provider.avgLatencyMs}ms"
                )
            }
        }
    }
}

@Composable
private fun ProviderStat(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(12.dp))
        Text(label, color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

// ── Voce ──────────────────────────────────────────────────────────────────────

@Composable
private fun VoceSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    var showElevenKey by remember { mutableStateOf(false) }
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
        trailingIcon = {
            TextButton(onClick = { showElevenKey = !showElevenKey }) {
                Text(if (showElevenKey) "Nascondi" else "Mostra", color = AlfredBlueLight,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.preferences.voiceEnabled, colors = outlinedColors(), singleLine = true
    )
    OutlinedTextField(
        value = state.preferences.voiceId,
        onValueChange = viewModel::onVoiceIdChange,
        label = { Text("Voice ID Alfred") },
        placeholder = { Text("pNInz6obpgDQGcFmaJgB  (Adam)", color = OnSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.preferences.voiceEnabled, colors = outlinedColors(), singleLine = true
    )
    Text("Adam (default): pNInz6obpgDQGcFmaJgB",
        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    SaveButton(viewModel)
}

// ── Memoria ───────────────────────────────────────────────────────────────────

@Composable
private fun MemoriaSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val prefs = state.preferences
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Memoria lunga", color = OnBackground, fontWeight = FontWeight.Medium)
            Text("Riepiloga periodicamente la conversazione per mantenerla in contesto",
                style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(
            checked = prefs.memoryEnabled,
            onCheckedChange = viewModel::onMemoryEnabledChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AlfredBlue, checkedThumbColor = AlfredBlueLight)
        )
    }

    HorizontalDivider(color = SurfaceVariant)

    SectionLabel("Riepilogo ogni N messaggi")
    Text("Ogni ${prefs.memorySummaryInterval} messaggi viene creato un riepilogo automatico",
        style = MaterialTheme.typography.bodySmall, color = AlfredBlueLight)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("5", color = OnSurfaceVariant, fontSize = 12.sp)
        Slider(
            value = prefs.memorySummaryInterval.toFloat(),
            onValueChange = { viewModel.onMemorySummaryIntervalChange(it.toInt()) },
            valueRange = 5f..50f,
            steps = 8,
            enabled = prefs.memoryEnabled,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = AlfredBlueLight, activeTrackColor = AlfredBlue,
                inactiveTrackColor = SurfaceVariant)
        )
        Text("50", color = OnSurfaceVariant, fontSize = 12.sp)
    }

    HorizontalDivider(color = SurfaceVariant)

    SectionLabel("Max messaggi in contesto")
    Text("Ultimi ${prefs.maxContextMessages} messaggi inviati al modello per ogni richiesta",
        style = MaterialTheme.typography.bodySmall, color = AlfredBlueLight)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("20", color = OnSurfaceVariant, fontSize = 12.sp)
        Slider(
            value = prefs.maxContextMessages.toFloat(),
            onValueChange = { viewModel.onMaxContextMessagesChange(it.toInt()) },
            valueRange = 20f..100f,
            steps = 7,
            enabled = prefs.memoryEnabled,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = AlfredBlueLight, activeTrackColor = AlfredBlue,
                inactiveTrackColor = SurfaceVariant)
        )
        Text("100", color = OnSurfaceVariant, fontSize = 12.sp)
    }

    Surface(shape = RoundedCornerShape(8.dp), color = AlfredBlue.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()) {
        Text(
            "Come funziona: ogni ${prefs.memorySummaryInterval} messaggi il backend crea un riassunto. " +
            "Il riassunto viene incluso come contesto nelle conversazioni future, " +
            "permettendo conversazioni molto lunghe senza esaurire il context window.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
        )
    }

    SaveButton(viewModel)
}

// ── Avanzate ──────────────────────────────────────────────────────────────────

@Composable
private fun AvanzateSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val prefs = state.preferences

    SectionLabel("Rete")

    Text("Timeout HTTP: ${prefs.httpTimeoutSeconds}s",
        style = MaterialTheme.typography.bodySmall, color = AlfredBlueLight)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("10", color = OnSurfaceVariant, fontSize = 12.sp)
        Slider(
            value = prefs.httpTimeoutSeconds.toFloat(),
            onValueChange = { viewModel.onHttpTimeoutChange(it.toInt()) },
            valueRange = 10f..120f,
            steps = 10,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = AlfredBlueLight, activeTrackColor = AlfredBlue,
                inactiveTrackColor = SurfaceVariant)
        )
        Text("120s", color = OnSurfaceVariant, fontSize = 12.sp)
    }

    Text("Retry automatici: ${prefs.retryCount}",
        style = MaterialTheme.typography.bodySmall, color = AlfredBlueLight)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("0", color = OnSurfaceVariant, fontSize = 12.sp)
        Slider(
            value = prefs.retryCount.toFloat(),
            onValueChange = { viewModel.onRetryCountChange(it.toInt()) },
            valueRange = 0f..5f,
            steps = 4,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = AlfredBlueLight, activeTrackColor = AlfredBlue,
                inactiveTrackColor = SurfaceVariant)
        )
        Text("5", color = OnSurfaceVariant, fontSize = 12.sp)
    }

    HorizontalDivider(color = SurfaceVariant)

    SectionLabel("Fallback provider")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Fallback automatico", color = OnBackground, fontWeight = FontWeight.Medium)
            Text("Se il provider attivo fallisce, prova automaticamente il successivo",
                style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(
            checked = prefs.providerFallbackEnabled,
            onCheckedChange = viewModel::onFallbackEnabledChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AlfredBlue, checkedThumbColor = AlfredBlueLight)
        )
    }
    Text("Ordine: OpenAI → Anthropic → Gemini",
        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)

    HorizontalDivider(color = SurfaceVariant)

    SectionLabel("Debug")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Modalità debug", color = OnBackground, fontWeight = FontWeight.Medium)
            Text("Abilita log dettagliati e overlay diagnostici",
                style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(
            checked = prefs.debugMode,
            onCheckedChange = viewModel::onDebugModeChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AlfredBlue, checkedThumbColor = AlfredBlueLight)
        )
    }

    SaveButton(viewModel)
}

// ── Account ───────────────────────────────────────────────────────────────────

@Composable
private fun AccountSection(state: SettingsUiState, onLogout: () -> Unit) {
    SectionLabel("Informazioni account")
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountRow("Utente", state.preferences.username.ifBlank { "—" })
            AccountRow("Ruolo", state.preferences.userRole.ifBlank { "—" })
        }
    }
    HorizontalDivider(color = SurfaceVariant)
    Button(
        onClick = onLogout,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
    ) {
        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Logout", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, color = OnBackground, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

// ── Servizio ──────────────────────────────────────────────────────────────────

@Composable
private fun ServizioSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SectionLabel("Companion Jenny")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Abilita companion Jenny", color = OnBackground, fontWeight = FontWeight.Medium)
            Text("Aggiunge Jenny al menu di navigazione", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(
            checked = state.preferences.jennyEnabled,
            onCheckedChange = viewModel::onJennyEnabledChange,
            colors = SwitchDefaults.colors(checkedTrackColor = JennyPurple, checkedThumbColor = JennyPurpleLight)
        )
    }
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Voce Jenny")
    OutlinedTextField(
        value = state.preferences.jennyVoiceId,
        onValueChange = viewModel::onJennyVoiceIdChange,
        label = { Text("Voice ID Jenny") },
        placeholder = { Text("EXAVITQu4vr4xnSDxMaL  (Bella)", color = OnSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.preferences.jennyEnabled, colors = outlinedColors(), singleLine = true
    )
    Text("Bella (default): EXAVITQu4vr4xnSDxMaL",
        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Personalità Jenny")
    val level = state.preferences.jennyPersonalityLevel
    val personalityLabel = when (level) {
        1 -> "Moderata e professionale"; 2 -> "Equilibrata"
        3 -> "Vivace e diretta (default)"; 4 -> "Molto espressiva"
        else -> "Massima espressività"
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("1", color = OnSurfaceVariant, fontSize = 12.sp)
        Slider(
            value = level.toFloat(),
            onValueChange = { viewModel.onJennyPersonalityLevelChange(it.toInt()) },
            valueRange = 1f..5f, steps = 3,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = JennyPurpleLight, activeTrackColor = JennyPurple,
                inactiveTrackColor = SurfaceVariant),
            enabled = state.preferences.jennyEnabled
        )
        Text("5", color = OnSurfaceVariant, fontSize = 12.sp)
    }
    Text(personalityLabel, color = JennyPurpleLight, style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium)
    HorizontalDivider(color = SurfaceVariant)
    SaveButton(viewModel)
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Gestione dati")
    OutlinedButton(
        onClick = viewModel::requestClearJennyConversations,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
    ) {
        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Cancella conversazioni Jenny", fontWeight = FontWeight.SemiBold, color = ErrorRed)
    }
}

// ── Smart Home Admin ──────────────────────────────────────────────────────────

@Composable
private fun SmartHomeAdminSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SectionLabel("Tuya Cloud credentials")
    Text(
        "Le credenziali Tuya vengono salvate nel file .env del backend.",
        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
    )

    SectionLabel("Abilita Smart Home")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Mostra tab Casa nella navigazione", color = OnSurface, modifier = Modifier.weight(1f))
        Switch(
            checked = state.preferences.smartHomeEnabled,
            onCheckedChange = viewModel::onSmartHomeEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SmartHomeAmberLight,
                checkedTrackColor = SmartHomeAmberLight.copy(alpha = 0.4f),
            )
        )
    }

    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Dispositivi Tuya")

    if (state.isDiscoveringDevices) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SmartHomeAmberLight, modifier = Modifier.size(28.dp))
        }
    } else {
        OutlinedButton(
            onClick = viewModel::discoverDevices,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SmartHomeAmberLight)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = SmartHomeAmberLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Scopri dispositivi", fontWeight = FontWeight.SemiBold, color = SmartHomeAmberLight)
        }
    }

    if (state.discoveryError != null) {
        Text(state.discoveryError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
    }

    if (state.discoveredDevices.isNotEmpty()) {
        Text(
            "${state.discoveredDevices.size} dispositivi trovati:",
            color = SuccessGreen, fontWeight = FontWeight.Medium
        )
        state.discoveredDevices.forEach { device ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(device.name, color = OnBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("ID: ${device.id}  •  ${device.category}", color = OnSurfaceVariant, fontSize = 11.sp)
                        Text(if (device.online) "online" else "offline",
                            color = if (device.online) SuccessGreen else OnSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    HorizontalDivider(color = SurfaceVariant)
    Text(
        "💡 Nota: le credenziali Tuya (TUYA_CLIENT_ID, TUYA_CLIENT_SECRET, TUYA_BASE_URL) " +
        "devono essere configurate nel file .env del server backend.",
        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
    )

    // TODO: Google Home integration
    // Google Home Local API (local SDK) or Google Home API (cloud) support is planned.
    // Requires Google Home Developer Console setup and OAuth2 flow.
    // See: https://developers.home.google.com/
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Home, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Google Home", color = OnSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("TODO: supporto Google Home via Google Home API (OAuth2 + SDK)", color = OnSurfaceVariant, fontSize = 11.sp)
            }
        }
    }

    SaveButton(viewModel)
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SaveButton(viewModel: SettingsViewModel) {
    Button(
        onClick = viewModel::save,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AlfredBlue)
    ) { Text("Salva", fontWeight = FontWeight.SemiBold) }
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
