package com.alfredJenny.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.remote.DEFAULT_BASE_URL
import com.alfredJenny.app.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SettingsSection(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GENERALE("Generale", Icons.Default.Tune),
    PROVIDER_AI("Provider AI", Icons.Default.SmartToy),
    VOCE("Voce", Icons.Default.RecordVoiceOver),
    ACCOUNT("Account", Icons.Default.Person),
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

    // Triple-tap detection on settings icon
    val scope = rememberCoroutineScope()
    var tapCount by remember { mutableIntStateOf(0) }
    var resetJob by remember { mutableStateOf<Job?>(null) }

    fun onSettingsIconTap() {
        tapCount++
        resetJob?.cancel()
        resetJob = scope.launch {
            delay(1500)
            tapCount = 0
        }
        if (tapCount >= 3) {
            tapCount = 0
            resetJob?.cancel()
            viewModel.onTripleTap()
        }
    }

    if (selectedSection == null) {
        // ── Section list ─────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize().background(Background)) {
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
                Text("Impostazioni", style = MaterialTheme.typography.titleLarge, color = OnBackground, modifier = Modifier.weight(1f))
                if (state.isSaved) Icon(Icons.Default.Check, contentDescription = "Salvato", tint = SuccessGreen)
                // Triple-tap target — settings icon
                IconButton(onClick = { onSettingsIconTap() }) {
                    Icon(Icons.Default.Settings, contentDescription = "Impostazioni avanzate", tint = OnSurfaceVariant)
                }
            }

            // Password unlock field (shown after triple tap)
            AnimatedVisibility(visible = state.showPasswordField, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceVariant)
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
                                    Icon(if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = OnSurfaceVariant)
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

            // Sections list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val visibleSections = SettingsSection.entries.filter {
                    it != SettingsSection.SERVIZIO || state.serviceSectionUnlocked
                }
                visibleSections.forEach { section ->
                    SectionCard(section = section, onClick = { selectedSection = section })
                }
            }
        }
    } else {
        // ── Section content ───────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize().background(Background)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedSection = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = OnBackground)
                }
                Text(selectedSection!!.label, style = MaterialTheme.typography.titleLarge, color = OnBackground, modifier = Modifier.weight(1f))
                if (selectedSection == SettingsSection.SERVIZIO) {
                    Surface(shape = RoundedCornerShape(12.dp), color = SuccessGreen.copy(alpha = 0.2f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(SuccessGreen, RoundedCornerShape(4.dp)))
                            Text("Modalità Servizio attiva", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                        }
                    }
                }
                if (state.isSaved) Icon(Icons.Default.Check, contentDescription = "Salvato", tint = SuccessGreen)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (selectedSection) {
                    SettingsSection.GENERALE -> GeneraleSection(state, viewModel, onOpenAvatarImport)
                    SettingsSection.PROVIDER_AI -> ProviderAiSection(state, viewModel)
                    SettingsSection.VOCE -> VoceSection(state, viewModel)
                    SettingsSection.ACCOUNT -> AccountSection(state, onLogout)
                    SettingsSection.SERVIZIO -> ServizioSection(state, viewModel)
                    null -> {}
                }
            }
        }
    }

    // Clear Jenny dialog
    if (state.showClearJennyDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearJennyDialog,
            title = { Text("Cancella conversazioni Jenny", color = OnBackground) },
            text = { Text("Tutte le conversazioni con Jenny verranno eliminate definitivamente.", color = OnSurfaceVariant) },
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
        LaunchedEffect(Unit) {
            delay(2000)
            viewModel.dismissClearedNotice()
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SuccessGreen.copy(alpha = 0.9f),
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Conversazioni Jenny cancellate", modifier = Modifier.padding(12.dp), color = OnBackground)
            }
        }
    }
}

@Composable
private fun SectionCard(section: SettingsSection, onClick: () -> Unit) {
    val isServizio = section == SettingsSection.SERVIZIO
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isServizio) JennyPurple.copy(alpha = 0.15f) else SurfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                section.icon,
                contentDescription = null,
                tint = if (isServizio) JennyPurpleLight else AlfredBlueLight,
                modifier = Modifier.size(24.dp)
            )
            Text(
                section.label,
                color = OnBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
        }
    }
}

@Composable
private fun GeneraleSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onOpenAvatarImport: () -> Unit
) {
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
    Text(
        "Default: $DEFAULT_BASE_URL  •  Produzione: https://tuo-backend.railway.app",
        style = MaterialTheme.typography.bodySmall,
        color = OnSurfaceVariant
    )
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Avatar")
    Text(
        "Sostituisci i frame dell'avatar animato con grafica personalizzata.",
        style = MaterialTheme.typography.bodySmall,
        color = OnSurfaceVariant
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderAiSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    var providerExpanded by remember { mutableStateOf(false) }
    var showAiKey by remember { mutableStateOf(false) }

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
        ExposedDropdownMenu(
            expanded = providerExpanded,
            onDismissRequest = { providerExpanded = false },
            modifier = Modifier.background(SurfaceVariant)
        ) {
            AIProvider.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.displayName, color = OnBackground) },
                    onClick = { viewModel.onProviderChange(p); providerExpanded = false }
                )
            }
        }
    }
    OutlinedTextField(
        value = state.preferences.apiKey,
        onValueChange = viewModel::onApiKeyChange,
        label = { Text("API Key AI") },
        placeholder = { Text("sk-...", color = OnSurfaceVariant) },
        visualTransformation = if (showAiKey) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { showAiKey = !showAiKey }) {
                Text(if (showAiKey) "Nascondi" else "Mostra", color = AlfredBlueLight, fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = outlinedColors(),
        singleLine = true
    )
    SaveButton(viewModel)
}

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
                Text(if (showElevenKey) "Nascondi" else "Mostra", color = AlfredBlueLight, fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.preferences.voiceEnabled,
        colors = outlinedColors(),
        singleLine = true
    )
    OutlinedTextField(
        value = state.preferences.voiceId,
        onValueChange = viewModel::onVoiceIdChange,
        label = { Text("Voice ID Alfred") },
        placeholder = { Text("pNInz6obpgDQGcFmaJgB  (Adam)", color = OnSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.preferences.voiceEnabled,
        colors = outlinedColors(),
        singleLine = true
    )
    Text(
        "Trovi i Voice ID su elevenlabs.io/voice-library.\nAdam (default): pNInz6obpgDQGcFmaJgB",
        style = MaterialTheme.typography.bodySmall,
        color = OnSurfaceVariant
    )
    SaveButton(viewModel)
}

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

@Composable
private fun ServizioSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    // Jenny toggle
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

    // Jenny Voice ID
    SectionLabel("Voce Jenny")
    OutlinedTextField(
        value = state.preferences.jennyVoiceId,
        onValueChange = viewModel::onJennyVoiceIdChange,
        label = { Text("Voice ID Jenny") },
        placeholder = { Text("EXAVITQu4vr4xnSDxMaL  (Bella)", color = OnSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.preferences.jennyEnabled,
        colors = outlinedColors(),
        singleLine = true
    )
    Text(
        "Bella (default): EXAVITQu4vr4xnSDxMaL\nRachel: 21m00Tcm4TlvDq8ikWAM",
        style = MaterialTheme.typography.bodySmall,
        color = OnSurfaceVariant
    )

    HorizontalDivider(color = SurfaceVariant)

    // Personality slider
    SectionLabel("Personalità Jenny")
    val level = state.preferences.jennyPersonalityLevel
    val personalityLabel = when (level) {
        1    -> "Moderata e professionale"
        2    -> "Equilibrata"
        3    -> "Vivace e diretta (default)"
        4    -> "Molto espressiva"
        5    -> "Massima espressività"
        else -> "Vivace e diretta (default)"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("1", color = OnSurfaceVariant, fontSize = 12.sp)
        Slider(
            value = level.toFloat(),
            onValueChange = { viewModel.onJennyPersonalityLevelChange(it.toInt()) },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = JennyPurpleLight,
                activeTrackColor = JennyPurple,
                inactiveTrackColor = SurfaceVariant
            ),
            enabled = state.preferences.jennyEnabled
        )
        Text("5", color = OnSurfaceVariant, fontSize = 12.sp)
    }
    Text(personalityLabel, color = JennyPurpleLight, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)

    HorizontalDivider(color = SurfaceVariant)

    // Save button
    SaveButton(viewModel)

    HorizontalDivider(color = SurfaceVariant)

    // Clear Jenny conversations
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

@Composable
private fun SaveButton(viewModel: SettingsViewModel) {
    Button(
        onClick = viewModel::save,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AlfredBlue)
    ) {
        Text("Salva", fontWeight = FontWeight.SemiBold)
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
