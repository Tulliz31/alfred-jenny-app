package com.alfredJenny.app.ui.screens.smarthome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.model.SmartHomeDevice
import com.alfredJenny.app.ui.theme.*
import kotlinx.coroutines.delay

// ── Colors ────────────────────────────────────────────────────────────────────

private val Amber       = Color(0xFFFFA726)
private val AmberDim    = Color(0xFF3D2A00)
private val AmberOff    = Color(0xFF1E1E1E)
private val TealOnline  = Color(0xFF4BCF7E)
private val TuyaBadge   = Color(0xFF1A6B8A)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHomeScreen(
    onOpenSettings: () -> Unit = {},
    isAdmin: Boolean = false,
    viewModel: SmartHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show command feedback as snackbar
    LaunchedEffect(state.commandFeedback) {
        state.commandFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.dismissCommandFeedback()
        }
    }

    // Rename dialog
    if (state.renamingDeviceId != null) {
        RenameDialog(
            currentName = state.renameInput,
            onNameChange = viewModel::onRenameInputChange,
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::cancelRename,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background)
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Smart Home",
                    color = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                )
                if (state.isSyncing || state.isLoading) {
                    CircularProgressIndicator(
                        color = Amber,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = viewModel::syncDevices) {
                        Icon(Icons.Default.Sync, contentDescription = "Sincronizza", tint = OnSurfaceVariant)
                    }
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aggiorna", tint = OnSurfaceVariant)
                    }
                }
            }

            HorizontalDivider(color = SurfaceVariant)

            // ── Error banner ──────────────────────────────────────────────────
            if (state.error != null) {
                Surface(
                    color = ErrorRed.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            state.error ?: "",
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = viewModel::dismissError,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── Tuya not configured ───────────────────────────────────────────
            if (!state.tuyaConfigured) {
                TuyaSetupScreen(onOpenSettings = onOpenSettings, isAdmin = isAdmin)
                return@Column
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (!state.isLoading && state.devices.isEmpty() && state.error == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Home,
                            null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Nessun dispositivo trovato",
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Premi Sincronizza per cercare i dispositivi Tuya.",
                            color = OnSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                return@Column
            }

            // ── Device grid ───────────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        isPending = state.pendingDeviceId == device.id,
                        isExpanded = state.expandedDeviceId == device.id,
                        onToggle = { viewModel.toggleDevice(device) },
                        onExpand = {
                            viewModel.expandDevice(
                                if (state.expandedDeviceId == device.id) null else device.id
                            )
                        },
                        onBrightnessChange = { level -> viewModel.setBrightness(device, level) },
                        onLongPress = {
                            viewModel.startRename(device.id, device.displayName)
                        },
                    )
                }
            }

            // ── Bottom bar ────────────────────────────────────────────────────
            if (state.lastUpdated.isNotBlank()) {
                HorizontalDivider(color = SurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Aggiornato: ${state.lastUpdated}",
                        color = OnSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${state.devices.count { it.online }} online",
                        color = TealOnline,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

// ── Tuya not configured ───────────────────────────────────────────────────────

@Composable
private fun TuyaSetupScreen(onOpenSettings: () -> Unit, isAdmin: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = Amber.copy(alpha = 0.15f),
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Home,
                        null,
                        tint = Amber,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            if (isAdmin) {
                Text(
                    "Connetti il tuo account Tuya",
                    color = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    "Configura le credenziali Tuya nelle impostazioni per controllare i tuoi dispositivi smart home con Alfred e Jenny.",
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Amber),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Configura", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(
                    "Smart Home non ancora configurato",
                    color = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    "Chiedi all'amministratore di configurare l'integrazione Tuya nelle impostazioni.",
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

// ── Device card ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceCard(
    device: SmartHomeDevice,
    isPending: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onLongPress: () -> Unit,
) {
    val cardColor = when {
        !device.online -> AmberOff
        device.isOn    -> AmberDim
        else           -> SurfaceVariant
    }
    val iconTint = when {
        !device.online -> OnSurfaceVariant
        device.isOn    -> Amber
        else           -> OnSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = true,
                onClick = { if (device.online) onExpand() },
                onLongClick = onLongPress,
            ),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Icon + badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = deviceIcon(device.type),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!device.online) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ErrorRed.copy(alpha = 0.15f),
                    ) {
                        Text(
                            "offline",
                            color = ErrorRed,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else if (isPending) {
                    CircularProgressIndicator(
                        color = Amber,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Switch(
                        checked = device.isOn,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Amber,
                            checkedTrackColor = Amber.copy(alpha = 0.4f),
                            uncheckedThumbColor = OnSurfaceVariant,
                            uncheckedTrackColor = SurfaceVariant,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Device name
            Text(
                device.displayName,
                color = if (device.online && device.isOn) OnBackground else OnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
            )

            // State text or temperature
            when {
                device.temperature != null ->
                    Text(
                        "%.1f°C".format(device.temperature),
                        color = TealOnline,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                device.online && device.isOn && device.brightness != null ->
                    Text(
                        "Accesa ${device.brightness}%",
                        color = Amber,
                        fontSize = 11.sp,
                    )
                else ->
                    Text(
                        if (!device.online) "Non raggiungibile"
                        else if (device.isOn) "Acceso" else "Spento",
                        color = if (device.online && device.isOn) Amber else OnSurfaceVariant,
                        fontSize = 11.sp,
                    )
            }

            // Source badge
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = TuyaBadge.copy(alpha = 0.25f),
            ) {
                Text(
                    device.source.uppercase(),
                    color = TuyaBadge,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }

            // Brightness slider (expanded)
            AnimatedVisibility(
                visible = isExpanded && device.canDim && device.online,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text("Luminosità", color = OnSurfaceVariant, fontSize = 11.sp)
                    var sliderValue by remember(device.brightness) {
                        mutableFloatStateOf((device.brightness ?: 50).toFloat())
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { onBrightnessChange(sliderValue.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Amber,
                            activeTrackColor = Amber,
                            inactiveTrackColor = SurfaceVariant,
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Min", color = OnSurfaceVariant, fontSize = 10.sp)
                        Text("${sliderValue.toInt()}%", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text("Max", color = OnSurfaceVariant, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ── Rename dialog ─────────────────────────────────────────────────────────────

@Composable
private fun RenameDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rinomina dispositivo", color = OnBackground) },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Amber,
                    unfocusedBorderColor = OnSurfaceVariant,
                    focusedLabelColor = Amber,
                    cursorColor = Amber,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Salva", color = Amber, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = OnSurfaceVariant)
            }
        },
        containerColor = Surface,
    )
}

// ── Icon mapping ──────────────────────────────────────────────────────────────

private fun deviceIcon(type: String): ImageVector = when (type) {
    "light"     -> Icons.Default.Lightbulb
    "switch"    -> Icons.Default.Power
    "thermostat"-> Icons.Default.Thermostat
    "fan"       -> Icons.Default.Air
    "ac"        -> Icons.Default.AcUnit
    "curtain"   -> Icons.Default.Blinds
    "camera"    -> Icons.Default.Videocam
    "appliance" -> Icons.Default.LocalCafe
    else        -> Icons.Default.Devices
}
