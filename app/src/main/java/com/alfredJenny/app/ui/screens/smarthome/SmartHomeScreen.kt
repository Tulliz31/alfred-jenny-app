package com.alfredJenny.app.ui.screens.smarthome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

// ── Color additions for Smart Home ────────────────────────────────────────────
private val SmartHomeAmber = Color(0xFFFFA726)
private val SmartHomeAmberDim = Color(0xFF3D2A00)
private val SmartHomeGreen = Color(0xFF4BCF7E)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun SmartHomeScreen(viewModel: SmartHomeViewModel = hiltViewModel()) {
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = SmartHomeAmber,
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
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = SmartHomeAmber,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = viewModel::load) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aggiorna", tint = OnSurfaceVariant)
                }
            }
        }

        HorizontalDivider(color = SurfaceVariant)

        // Error banner
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
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(state.error ?: "", color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = viewModel::dismissError, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Empty state
        if (!state.isLoading && state.devices.isEmpty() && state.error == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Nessun dispositivo trovato", color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Configura le credenziali Tuya nelle Impostazioni admin.",
                        color = OnSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            return@Column
        }

        // Device grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
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
                )
            }
        }
    }
}

// ── Device card ───────────────────────────────────────────────────────────────

@Composable
private fun DeviceCard(
    device: SmartHomeDevice,
    isPending: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
) {
    val cardColor = if (device.isOn) SmartHomeAmberDim else SurfaceVariant
    val iconTint = when {
        !device.online -> OnSurfaceVariant
        device.isOn    -> SmartHomeAmber
        else           -> OnSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = device.online) { onExpand() },
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Icon row
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
                        imageVector = deviceIcon(device.category),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!device.online) {
                    Text("offline", color = OnSurfaceVariant, fontSize = 10.sp)
                } else if (isPending) {
                    CircularProgressIndicator(
                        color = SmartHomeAmber,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Switch(
                        checked = device.isOn,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier
                            .height(24.dp)
                            .padding(0.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SmartHomeAmber,
                            checkedTrackColor = SmartHomeAmber.copy(alpha = 0.4f),
                            uncheckedThumbColor = OnSurfaceVariant,
                            uncheckedTrackColor = SurfaceVariant,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Device name
            Text(
                device.name,
                color = if (device.isOn) OnBackground else OnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
            )

            // State label or temperature
            if (device.temperature != null) {
                Text(
                    "%.1f°C".format(device.temperature),
                    color = SmartHomeGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Text(
                    if (!device.online) "Non raggiungibile"
                    else if (device.isOn) "Acceso" else "Spento",
                    color = if (device.isOn) SmartHomeAmber else OnSurfaceVariant,
                    fontSize = 12.sp,
                )
            }

            // Brightness slider (expanded)
            AnimatedVisibility(
                visible = isExpanded && device.brightness != null && device.online,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text("Luminosità", color = OnSurfaceVariant, fontSize = 11.sp)
                    var sliderValue by remember(device.brightness) {
                        mutableFloatStateOf((device.brightness ?: 500).toFloat())
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { onBrightnessChange(sliderValue.toInt()) },
                        valueRange = 10f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = SmartHomeAmber,
                            activeTrackColor = SmartHomeAmber,
                            inactiveTrackColor = SurfaceVariant,
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Min", color = OnSurfaceVariant, fontSize = 10.sp)
                        Text("${(sliderValue / 10).toInt()}%", color = SmartHomeAmber, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text("Max", color = OnSurfaceVariant, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ── Icon mapping ──────────────────────────────────────────────────────────────

private fun deviceIcon(category: String): ImageVector = when (category) {
    "dj", "dd", "fwd", "dc", "xdd" -> Icons.Default.Lightbulb   // lights
    "kg", "pc", "cz"                -> Icons.Default.Power         // switches / outlets
    "wk", "wsdcg", "moes"          -> Icons.Default.Thermostat    // thermostats / sensors
    "fs"                            -> Icons.Default.Air           // fans
    "kt"                            -> Icons.Default.AcUnit        // AC
    "cl", "msp"                     -> Icons.Default.Blinds        // curtains
    "kfj"                           -> Icons.Default.LocalCafe     // coffee maker
    else                            -> Icons.Default.Devices        // generic
}
