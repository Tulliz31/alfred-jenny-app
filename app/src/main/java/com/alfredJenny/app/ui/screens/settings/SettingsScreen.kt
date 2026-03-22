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
import com.alfredJenny.app.data.model.SmartHomeDevice
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.model.ProviderInfo
import com.alfredJenny.app.data.remote.DEFAULT_BASE_URL
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.alfredJenny.app.ui.components.JennyOutfit
import com.alfredJenny.app.ui.components.rememberAssetBitmap
import com.alfredJenny.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

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
    onOpenJennyAvatar: () -> Unit = {},
    onOpenJennyAI: () -> Unit = {},
    onOpenAlfredAI: () -> Unit = {},
    onOpenAlfredVoice: () -> Unit = {},
    onOpenJennyVoice: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }

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
                Icon(Icons.Default.Settings, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(24.dp).padding(4.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val visibleSections = SettingsSection.entries.filter {
                    when (it) {
                        SettingsSection.SERVIZIO   -> state.isAdmin
                        SettingsSection.SMART_HOME -> state.isAdmin
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
                    SettingsSection.GENERALE    -> GeneraleSection(state, viewModel, onOpenAvatarImport, onOpenJennyAvatar, onOpenAlfredAI)
                    SettingsSection.PROVIDER_AI -> ProviderAiSection(state, viewModel)
                    SettingsSection.VOCE        -> VoceSection(state, viewModel, onOpenAlfredVoice)
                    SettingsSection.MEMORIA     -> MemoriaSection(state, viewModel)
                    SettingsSection.AVANZATE    -> AvanzateSection(state, viewModel)
                    SettingsSection.ACCOUNT     -> AccountSection(state, viewModel, onLogout)
                    SettingsSection.SMART_HOME  -> SmartHomeAdminSection(state, viewModel)
                    SettingsSection.SERVIZIO    -> ServizioSection(state, viewModel, onOpenJennyAvatar, onOpenJennyAI, onOpenJennyVoice)
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

    // ── Change password dialog ─────────────────────────────────────────────────
    if (state.showChangePasswordDialog) {
        var showCurrent by remember { mutableStateOf(false) }
        var showNew by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = viewModel::dismissChangePassword,
            containerColor = Surface,
            title = { Text("Cambia password", color = OnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.currentPassword,
                        onValueChange = viewModel::onCurrentPasswordChange,
                        label = { Text("Password attuale") },
                        visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurrent = !showCurrent }) {
                                Icon(if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = OnSurfaceVariant)
                            }
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), colors = outlinedColors()
                    )
                    OutlinedTextField(
                        value = state.newPassword,
                        onValueChange = viewModel::onNewPasswordChange,
                        label = { Text("Nuova password") },
                        visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNew = !showNew }) {
                                Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = OnSurfaceVariant)
                            }
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), colors = outlinedColors()
                    )
                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label = { Text("Conferma password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth(), colors = outlinedColors()
                    )
                    if (state.changePasswordError != null) {
                        Text(state.changePasswordError!!, color = ErrorRed,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                if (state.isChangingPassword) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp),
                        color = AlfredBlueLight, strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = viewModel::submitChangePassword) {
                        Text("Aggiorna", color = AlfredBlueLight, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissChangePassword) {
                    Text("Annulla", color = OnSurfaceVariant)
                }
            }
        )
    }

    // ── Create user dialog ────────────────────────────────────────────────────
    if (state.showCreateUserDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCreateUser,
            containerColor = Surface,
            title = { Text("Nuovo utente", color = OnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.newUserName,
                        onValueChange = viewModel::onNewUserNameChange,
                        label = { Text("Username") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), colors = outlinedColors()
                    )
                    OutlinedTextField(
                        value = state.newUserPassword,
                        onValueChange = viewModel::onNewUserPasswordChange,
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth(), colors = outlinedColors()
                    )
                    var showRoleMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { showRoleMenu = true }, modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OnSurfaceVariant)) {
                            Text("Ruolo: ${state.newUserRole}", color = OnBackground, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, tint = OnSurfaceVariant)
                        }
                        DropdownMenu(expanded = showRoleMenu, onDismissRequest = { showRoleMenu = false }) {
                            DropdownMenuItem(text = { Text("user", color = OnBackground) },
                                onClick = { viewModel.onNewUserRoleChange("user"); showRoleMenu = false })
                            DropdownMenuItem(text = { Text("admin", color = OnBackground) },
                                onClick = { viewModel.onNewUserRoleChange("admin"); showRoleMenu = false })
                        }
                    }
                    if (state.userActionError != null) {
                        Text(state.userActionError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::submitCreateUser) {
                    Text("Crea", color = AlfredBlueLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCreateUser) {
                    Text("Annulla", color = OnSurfaceVariant)
                }
            }
        )
    }

    // ── Edit user dialog ──────────────────────────────────────────────────────
    if (state.showEditUserDialog && state.editingUser != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEditUser,
            containerColor = Surface,
            title = { Text("Modifica utente: ${state.editingUser!!.username}", color = OnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.newUserPassword,
                        onValueChange = viewModel::onNewUserPasswordChange,
                        label = { Text("Nuova password (lascia vuoto per non cambiare)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth(), colors = outlinedColors()
                    )
                    var showRoleMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { showRoleMenu = true }, modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OnSurfaceVariant)) {
                            Text("Ruolo: ${state.newUserRole}", color = OnBackground, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, tint = OnSurfaceVariant)
                        }
                        DropdownMenu(expanded = showRoleMenu, onDismissRequest = { showRoleMenu = false }) {
                            DropdownMenuItem(text = { Text("user", color = OnBackground) },
                                onClick = { viewModel.onNewUserRoleChange("user"); showRoleMenu = false })
                            DropdownMenuItem(text = { Text("admin", color = OnBackground) },
                                onClick = { viewModel.onNewUserRoleChange("admin"); showRoleMenu = false })
                        }
                    }
                    if (state.userActionError != null) {
                        Text(state.userActionError!!, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::submitEditUser) {
                    Text("Salva", color = AlfredBlueLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEditUser) {
                    Text("Annulla", color = OnSurfaceVariant)
                }
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneraleSection(state: SettingsUiState, viewModel: SettingsViewModel, onOpenAvatarImport: () -> Unit, onOpenJennyAvatar: () -> Unit = {}, onOpenAlfredAI: () -> Unit = {}) {
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
    if (!state.isAdmin) {
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
        HorizontalDivider(color = SurfaceVariant)
    }
    if (state.isAdmin) {
        SectionLabel("Avatar Jenny")
        Text("Sostituisci i PNG di body, occhi e bocca con immagini personalizzate.",
            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        OutlinedButton(
            onClick = onOpenJennyAvatar,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, JennyPurpleLight)
        ) {
            Icon(Icons.Default.Face, contentDescription = null, tint = JennyPurpleLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Gestisci avatar Jenny", fontWeight = FontWeight.SemiBold, color = JennyPurpleLight)
        }
        HorizontalDivider(color = SurfaceVariant)
    }
    SectionLabel("AI Dedicata Alfred")
    Text("Configura un provider AI separato usato solo da Alfred.",
        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    OutlinedButton(
        onClick = onOpenAlfredAI,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlfredBlueLight)
    ) {
        Icon(Icons.Default.SmartToy, contentDescription = null, tint = AlfredBlueLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Configura AI Alfred", fontWeight = FontWeight.SemiBold, color = AlfredBlueLight)
    }
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Tema")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Tema chiaro", color = OnBackground, fontWeight = FontWeight.Medium)
            Text("Passa al tema chiaro dell'interfaccia", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(
            checked = state.preferences.lightTheme,
            onCheckedChange = viewModel::toggleLightTheme,
            colors = SwitchDefaults.colors(checkedTrackColor = AlfredBlue, checkedThumbColor = AlfredBlueLight)
        )
    }
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Note & Calendario")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Abilita Note", color = OnBackground, fontWeight = FontWeight.Medium)
            Text("Mostra il tab Note nella barra di navigazione", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(
            checked = state.preferences.notesEnabled,
            onCheckedChange = viewModel::onNotesEnabledChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AlfredBlue, checkedThumbColor = AlfredBlueLight)
        )
    }
    // ── Google Calendar connect ────────────────────────────────────────────────
    Spacer(Modifier.height(4.dp))
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            runCatching {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                viewModel.onGoogleSignInSuccess(account)
            }
        }
    }
    val googleEmail = state.preferences.googleCalendarEmail
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (googleEmail.isNotBlank()) Color(0xFF1A3A1A) else SurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🗓️", fontSize = 18.sp)
                Text("Google Calendar",
                    color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    modifier = Modifier.weight(1f))
                if (googleEmail.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF2E7D32)) {
                        Text("Connesso", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            if (googleEmail.isNotBlank()) {
                Text(googleEmail, color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { viewModel.disconnectGoogleCalendar() },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null, tint = ErrorRed,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Disconnetti account Google", color = ErrorRed, fontSize = 13.sp)
                }
            } else {
                Text("Connetti il tuo account Google per usare Google Calendar.",
                    color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { googleSignInLauncher.launch(viewModel.getGoogleSignInIntent()) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4285F4))
                ) {
                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Connetti Google Calendar", color = Color(0xFF4285F4), fontSize = 13.sp,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (state.availableCalendars.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Calendario predefinito", color = OnBackground, fontWeight = FontWeight.Medium)
        Text("Il calendario dove Alfred e Jenny aggiungono gli eventi",
            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        var calExpanded by remember { mutableStateOf(false) }
        val selectedCal = state.availableCalendars.find { it.id == state.preferences.defaultCalendarId }
        ExposedDropdownMenuBox(expanded = calExpanded, onExpandedChange = { calExpanded = it }) {
            OutlinedTextField(
                value = selectedCal?.name ?: "Seleziona calendario",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = calExpanded) },
                colors = outlinedColors(),
            )
            ExposedDropdownMenu(expanded = calExpanded, onDismissRequest = { calExpanded = false }) {
                state.availableCalendars.forEach { cal ->
                    DropdownMenuItem(
                        text = { Text(cal.name) },
                        onClick = { viewModel.onDefaultCalendarIdChange(cal.id); calExpanded = false },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Chiedi conferma", color = OnBackground, fontWeight = FontWeight.Medium)
                Text("Mostra un dialogo prima di aggiungere un evento", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            Switch(
                checked = state.preferences.calendarConfirmBeforeAdd,
                onCheckedChange = viewModel::onCalendarConfirmChange,
                colors = SwitchDefaults.colors(checkedTrackColor = AlfredBlue, checkedThumbColor = AlfredBlueLight)
            )
        }
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
private fun VoceSection(state: SettingsUiState, viewModel: SettingsViewModel, onOpenAlfredVoice: () -> Unit = {}) {
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
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Voce Alfred")
    if (state.preferences.voiceId.isNotBlank()) {
        Surface(shape = RoundedCornerShape(8.dp), color = AlfredBlue.copy(0.1f), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, null, tint = AlfredBlueLight, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Voice ID: ${state.preferences.voiceId}", color = OnBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
            }
        }
    }
    OutlinedButton(
        onClick = onOpenAlfredVoice,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        enabled = state.preferences.voiceEnabled,
        border = androidx.compose.foundation.BorderStroke(1.dp, AlfredBlueLight)
    ) {
        Icon(Icons.Default.RecordVoiceOver, null, tint = AlfredBlueLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sfoglia voci Alfred →", fontWeight = FontWeight.SemiBold, color = AlfredBlueLight)
    }
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
private fun AccountSection(state: SettingsUiState, viewModel: SettingsViewModel, onLogout: () -> Unit) {
    SectionLabel("Informazioni account")
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountRow("Utente", state.preferences.username.ifBlank { "—" })
            AccountRow("Ruolo", state.preferences.userRole.ifBlank { "—" })
        }
    }
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Sicurezza")
    OutlinedButton(
        onClick = viewModel::showChangePassword,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlfredBlueLight)
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = AlfredBlueLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Cambia password", fontWeight = FontWeight.SemiBold, color = AlfredBlueLight)
    }
    if (state.changePasswordSuccess) {
        LaunchedEffect(Unit) { delay(2000); viewModel.dismissChangePasswordSuccess() }
        Surface(shape = RoundedCornerShape(8.dp), color = SuccessGreen.copy(0.15f), modifier = Modifier.fillMaxWidth()) {
            Text("Password aggiornata!", modifier = Modifier.padding(12.dp), color = SuccessGreen)
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
private fun ServizioSection(state: SettingsUiState, viewModel: SettingsViewModel, onOpenJennyAvatar: () -> Unit, onOpenJennyAI: () -> Unit = {}, onOpenJennyVoice: () -> Unit = {}) {
    SectionLabel("Companion Jenny")
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("Voce Jenny")
    if (state.preferences.jennyVoiceId.isNotBlank()) {
        Surface(shape = RoundedCornerShape(8.dp), color = JennyPurple.copy(0.1f), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, null, tint = JennyPurpleLight, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Voice ID: ${state.preferences.jennyVoiceId}", color = OnBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
            }
        }
    }
    OutlinedButton(
        onClick = onOpenJennyVoice,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, JennyPurpleLight)
    ) {
        Icon(Icons.Default.RecordVoiceOver, null, tint = JennyPurpleLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sfoglia voci Jenny →", fontWeight = FontWeight.SemiBold, color = JennyPurpleLight)
    }
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
                inactiveTrackColor = SurfaceVariant)
        )
        Text("5", color = OnSurfaceVariant, fontSize = 12.sp)
    }
    Text(personalityLabel, color = JennyPurpleLight, style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium)
    HorizontalDivider(color = SurfaceVariant)

    // ── Outfit automatico ─────────────────────────────────────────────────────
    SectionLabel("Outfit automatico")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Cambio outfit automatico", color = OnBackground, fontWeight = FontWeight.Medium)
            Text("Jenny cambia abito in base al contesto della conversazione",
                style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(
            checked = state.preferences.jennyAutoOutfit,
            onCheckedChange = viewModel::onJennyAutoOutfitChange,
            colors = SwitchDefaults.colors(checkedTrackColor = JennyPurple, checkedThumbColor = JennyPurpleLight)
        )
    }

    Spacer(Modifier.height(12.dp))
    Text("Outfit attivo", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(bottom = 8.dp))

    // Current outfit preview + force buttons
    val currentOutfit = runCatching { JennyOutfit.valueOf(state.preferences.jennyOutfit) }
        .getOrDefault(JennyOutfit.CASUAL)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JennyOutfit.builtIn().forEach { o ->
            val thumb = rememberAssetBitmap(o.assetFile, sampleSize = 4)
            val isActive = (o == currentOutfit)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(90.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) JennyPurple.copy(alpha = 0.4f) else SurfaceVariant)
                        .border(
                            width = if (isActive) 2.dp else 0.5.dp,
                            color = if (isActive) JennyPurpleLight else SurfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumb != null) {
                        Image(
                            bitmap = thumb,
                            contentDescription = o.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = JennyPurpleLight,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                        )
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.forceJennyOutfit(o) },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isActive) JennyPurpleLight else SurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = o.label,
                        fontSize = 11.sp,
                        color = if (isActive) JennyPurpleLight else OnSurfaceVariant,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }

    HorizontalDivider(color = SurfaceVariant, modifier = Modifier.padding(top = 12.dp))
    SaveButton(viewModel)
    HorizontalDivider(color = SurfaceVariant)
    SectionLabel("AI Dedicata Jenny")
    Text("Configura un provider AI separato (OpenRouter o URL custom) usato solo da Jenny.",
        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    OutlinedButton(
        onClick = onOpenJennyAI,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, JennyPurple)
    ) {
        Icon(Icons.Default.SmartToy, contentDescription = null, tint = JennyPurple, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Configura AI Jenny", fontWeight = FontWeight.SemiBold, color = JennyPurple)
    }
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
    HorizontalDivider(color = SurfaceVariant)
    AdminUserManagerSection(state, viewModel)
}

// ── Smart Home Admin ──────────────────────────────────────────────────────────

@Composable
private fun SmartHomeAdminSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val prefs = state.preferences
    var showSecret by remember { mutableStateOf(false) }

    // ── General toggle ────────────────────────────────────────────────────────
    SectionLabel("Generali")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Mostra tab Smart Home", color = OnBackground, fontWeight = FontWeight.Medium)
            Text("Aggiunge il tab Casa alla navigazione", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(
            checked = prefs.smartHomeEnabled,
            onCheckedChange = viewModel::onSmartHomeEnabledChange,
            colors = SwitchDefaults.colors(checkedThumbColor = SmartHomeAmberLight, checkedTrackColor = SmartHomeAmberLight.copy(0.4f))
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Alfred e Jenny controllano i dispositivi", color = OnBackground, fontWeight = FontWeight.Medium)
            Text("Permette agli assistenti di eseguire comandi smart home", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(
            checked = prefs.smartHomeAiControl,
            onCheckedChange = viewModel::onSmartHomeAiControlChange,
            colors = SwitchDefaults.colors(checkedThumbColor = SmartHomeAmberLight, checkedTrackColor = SmartHomeAmberLight.copy(0.4f))
        )
    }

    HorizontalDivider(color = SurfaceVariant)

    // ── Tuya connector ────────────────────────────────────────────────────────
    SectionLabel("TUYA CONNECTOR")

    // Region dropdown
    var showRegionMenu by remember { mutableStateOf(false) }
    val regions = listOf("EU", "US", "CN", "IN")
    val regionLabels = mapOf(
        "EU" to "Europa — openapi.tuyaeu.com",
        "US" to "America — openapi.tuyaus.com",
        "CN" to "Cina — openapi.tuyacn.com",
        "IN" to "India — openapi.tuyain.com",
    )
    Box {
        OutlinedButton(
            onClick = { showRegionMenu = true },
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, OnSurfaceVariant),
        ) {
            Text("Regione: ${regionLabels[prefs.tuyaRegion] ?: prefs.tuyaRegion}", color = OnBackground, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, tint = OnSurfaceVariant)
        }
        DropdownMenu(
            expanded = showRegionMenu,
            onDismissRequest = { showRegionMenu = false },
        ) {
            regions.forEach { r ->
                DropdownMenuItem(
                    text = { Text(regionLabels[r] ?: r, color = OnBackground) },
                    onClick = { viewModel.onTuyaRegionChange(r); showRegionMenu = false },
                )
            }
        }
    }

    OutlinedTextField(
        value = prefs.tuyaClientId,
        onValueChange = viewModel::onTuyaClientIdChange,
        label = { Text("Client ID") },
        placeholder = { Text("xxxxxxxxxxxxxxxxxxxxxxxxx", color = OnSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = outlinedColors(),
    )

    OutlinedTextField(
        value = prefs.tuyaClientSecret,
        onValueChange = viewModel::onTuyaClientSecretChange,
        label = { Text("Client Secret") },
        visualTransformation = if (showSecret) androidx.compose.ui.text.input.VisualTransformation.None
                               else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { showSecret = !showSecret }) {
                Text(if (showSecret) "Nascondi" else "Mostra", color = SmartHomeAmberLight,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = outlinedColors(),
    )

    OutlinedTextField(
        value = prefs.tuyaUserId,
        onValueChange = viewModel::onTuyaUserIdChange,
        label = { Text("User UID (opzionale)") },
        placeholder = { Text("Lascia vuoto per usare tutti i dispositivi", color = OnSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = outlinedColors(),
    )

    // Test + sync buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.isTuyaTesting) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SmartHomeAmberLight, modifier = Modifier.size(28.dp))
            }
        } else {
            OutlinedButton(
                onClick = { viewModel.save(); viewModel.testTuyaConnection() },
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SmartHomeAmberLight),
            ) {
                Icon(Icons.Default.NetworkCheck, null, tint = SmartHomeAmberLight, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Test", color = SmartHomeAmberLight, fontSize = 13.sp)
            }
        }
        OutlinedButton(
            onClick = viewModel::discoverDevices,
            modifier = Modifier.weight(1f),
            enabled = !state.isDiscoveringDevices,
            border = androidx.compose.foundation.BorderStroke(1.dp, SmartHomeAmberLight),
        ) {
            if (state.isDiscoveringDevices) {
                CircularProgressIndicator(color = SmartHomeAmberLight, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Sync, null, tint = SmartHomeAmberLight, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text("Sincronizza", color = SmartHomeAmberLight, fontSize = 13.sp)
        }
    }

    // Test result
    if (state.tuyaTestResult != null) {
        Surface(shape = RoundedCornerShape(8.dp), color = SuccessGreen.copy(0.12f), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(state.tuyaTestResult!!, color = SuccessGreen, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::dismissTuyaTestResult, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
    if (state.tuyaTestError != null) {
        Surface(shape = RoundedCornerShape(8.dp), color = ErrorRed.copy(0.12f), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(state.tuyaTestError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::dismissTuyaTestResult, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
    if (state.discoveryError != null) {
        Text(state.discoveryError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
    }

    HorizontalDivider(color = SurfaceVariant)

    // ── Device list (discovered) ──────────────────────────────────────────────
    SectionLabel("GESTIONE DISPOSITIVI")
    if (state.discoveredDevices.isEmpty()) {
        Text(
            "Clicca 'Test' o 'Sincronizza' per trovare i dispositivi.",
            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant,
        )
    } else {
        Text(
            "${state.discoveredDevices.size} dispositivi trovati:",
            color = SuccessGreen, fontWeight = FontWeight.Medium,
        )
        state.discoveredDevices.forEach { device ->
            var editName by remember(device.id) { mutableStateOf(device.displayName) }
            var isEditing by remember(device.id) { mutableStateOf(false) }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (device.visible) SurfaceVariant else SurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.displayName, color = if (device.visible) OnBackground else OnSurfaceVariant,
                                fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("ID: ${device.id}  •  ${device.type}", color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (device.online) SuccessGreen.copy(0.15f) else SurfaceVariant,
                        ) {
                            Text(
                                if (device.online) "online" else "offline",
                                color = if (device.online) SuccessGreen else OnSurfaceVariant,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        // Visibility toggle (controls if device is visible to AI)
                        Switch(
                            checked = device.visible,
                            onCheckedChange = { viewModel.toggleDeviceVisible(device.id, it) },
                            modifier = Modifier.height(24.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SmartHomeAmberLight,
                                checkedTrackColor = SmartHomeAmberLight.copy(0.4f),
                                uncheckedThumbColor = OnSurfaceVariant,
                                uncheckedTrackColor = SurfaceVariant,
                            ),
                        )
                        IconButton(onClick = { isEditing = !isEditing }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, null, tint = if (isEditing) SmartHomeAmberLight else OnSurfaceVariant,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                    // Inline rename field
                    if (isEditing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Nome custom", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SmartHomeAmberLight,
                                    unfocusedBorderColor = OnSurfaceVariant,
                                    focusedLabelColor = SmartHomeAmberLight,
                                    cursorColor = SmartHomeAmberLight,
                                    focusedTextColor = OnBackground,
                                    unfocusedTextColor = OnBackground,
                                ),
                            )
                            IconButton(
                                onClick = {
                                    viewModel.renameSettingsDevice(device.id, editName.trim())
                                    isEditing = false
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(Icons.Default.Save, null, tint = SmartHomeAmberLight, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(
                            if (device.visible) "Visibile ad Alfred e Jenny" else "Nascosto agli assistenti",
                            color = if (device.visible) SmartHomeAmberLight else OnSurfaceVariant,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }

    HorizontalDivider(color = SurfaceVariant)

    // ── Future integrations ───────────────────────────────────────────────────
    SectionLabel("ALEXA / GOOGLE HOME")

    Surface(shape = RoundedCornerShape(10.dp), color = SurfaceVariant.copy(0.5f), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🔊", fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Amazon Alexa", color = OnSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("Integrazione in sviluppo", color = OnSurfaceVariant.copy(0.6f), fontSize = 11.sp)
            }
            Surface(shape = RoundedCornerShape(4.dp), color = OnSurfaceVariant.copy(0.1f)) {
                Text("Prossimamente", color = OnSurfaceVariant, fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }

    Surface(shape = RoundedCornerShape(10.dp), color = SurfaceVariant.copy(0.5f), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🏠", fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Google Home", color = OnSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("Integrazione in sviluppo", color = OnSurfaceVariant.copy(0.6f), fontSize = 11.sp)
            }
            Surface(shape = RoundedCornerShape(4.dp), color = OnSurfaceVariant.copy(0.1f)) {
                Text("Prossimamente", color = OnSurfaceVariant, fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }

    HorizontalDivider(color = SurfaceVariant)
    SaveButton(viewModel)
}

// ── Admin User Manager ────────────────────────────────────────────────────────

@Composable
private fun AdminUserManagerSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    // Load on first composition
    LaunchedEffect(Unit) { viewModel.loadUsers() }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("GESTIONE UTENTI")
        Spacer(Modifier.weight(1f))
        IconButton(onClick = viewModel::loadUsers) {
            Icon(Icons.Default.Refresh, contentDescription = "Aggiorna", tint = AlfredBlueLight, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = viewModel::showCreateUser) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Aggiungi utente", tint = AlfredBlueLight, modifier = Modifier.size(20.dp))
        }
    }

    if (state.userActionSuccess != null) {
        LaunchedEffect(state.userActionSuccess) { delay(2000); viewModel.dismissUserActionFeedback() }
        Surface(shape = RoundedCornerShape(8.dp), color = SuccessGreen.copy(0.12f), modifier = Modifier.fillMaxWidth()) {
            Text(state.userActionSuccess!!, modifier = Modifier.padding(10.dp), color = SuccessGreen,
                style = MaterialTheme.typography.bodySmall)
        }
    }
    if (state.usersError != null) {
        Text(state.usersError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
    }

    if (state.isLoadingUsers) {
        Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AlfredBlueLight, modifier = Modifier.size(24.dp))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.users.forEach { user ->
                Surface(shape = RoundedCornerShape(10.dp), color = SurfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.username, color = OnBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(user.role, color = if (user.role == "admin") AlfredBlueLight else OnSurfaceVariant,
                                fontSize = 11.sp)
                        }
                        IconButton(onClick = { viewModel.showEditUser(user) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = OnSurfaceVariant,
                                modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { viewModel.deleteUser(user.username) },
                            modifier = Modifier.size(36.dp),
                            enabled = user.role != "admin"
                        ) {
                            Icon(Icons.Default.PersonRemove, contentDescription = "Elimina",
                                tint = if (user.role != "admin") ErrorRed else SurfaceVariant,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    HorizontalDivider(color = SurfaceVariant, modifier = Modifier.padding(top = 8.dp))

    // Activity log toggle
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = viewModel::toggleActivityLog),
        verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("LOG ATTIVITÀ")
        Spacer(Modifier.weight(1f))
        Icon(
            if (state.showActivityLog) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null, tint = OnSurfaceVariant
        )
    }
    AnimatedVisibility(visible = state.showActivityLog, enter = expandVertically(), exit = shrinkVertically()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
            if (state.isLoadingLog) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AlfredBlueLight, modifier = Modifier.size(24.dp))
                }
            }
            if (state.logError != null) {
                Text(state.logError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
            }
            state.activityLog.forEach { entry ->
                Surface(shape = RoundedCornerShape(6.dp), color = Surface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(entry.username, color = AlfredBlueLight, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(entry.action, color = OnBackground, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.weight(1f))
                            Text(entry.timestamp.take(16).replace("T", " "), color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                        if (entry.details.isNotBlank()) {
                            Text(entry.details, color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
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
