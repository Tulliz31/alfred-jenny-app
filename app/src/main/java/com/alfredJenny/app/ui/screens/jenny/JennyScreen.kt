package com.alfredJenny.app.ui.screens.jenny

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.model.VoiceMode
import com.alfredJenny.app.ui.components.AlfredAvatarView
import com.alfredJenny.app.ui.theme.*

@Composable
fun JennyScreen(
    viewModel: JennyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var avatarExpanded by remember { mutableStateOf(true) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAudioPermission = granted }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { avatarExpanded = !avatarExpanded }) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(
                        androidx.compose.ui.graphics.Brush.radialGradient(listOf(JennyPurpleLight, JennyPurpleDark))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("J", color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text("Jenny", fontWeight = FontWeight.Bold, color = OnBackground, fontSize = 18.sp)
                Text(
                    when {
                        state.isSpeaking  -> "parla..."
                        state.isLoading   -> "sta scrivendo..."
                        state.isListening -> "ti ascolto..."
                        else              -> "online"
                    },
                    color = when {
                        state.isSpeaking  -> JennyPurpleLight
                        state.isLoading   -> AccentGlow
                        state.isListening -> SuccessGreen
                        else              -> JennyPurpleLight
                    },
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = viewModel::toggleVoice) {
                Icon(
                    if (state.voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Voce",
                    tint = if (state.voiceEnabled) JennyPurpleLight else OnSurfaceVariant
                )
            }
        }

        // Avatar panel (collapsible)
        AnimatedVisibility(visible = avatarExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            Box(
                modifier = Modifier.fillMaxWidth().background(Background),
                contentAlignment = Alignment.Center
            ) {
                if (state.isSpeaking) JennySpeakingWave()
                AlfredAvatarView(state = state.avatarState)
            }
        }

        // Voice mode selector
        JennyVoiceModeSelector(
            currentMode = state.voiceMode,
            onToggle = {
                if (!hasAudioPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                else viewModel.toggleVoiceMode()
            }
        )

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                JennyMessageBubble(msg.role, msg.content)
            }
            if (state.isLoading) item {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(BubbleAssistant).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    val t = rememberInfiniteTransition(label = "typing")
                    repeat(3) { i ->
                        val alpha by t.animateFloat(0.3f, 1f, label = "dot$i", animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 150), RepeatMode.Reverse))
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(OnSurfaceVariant.copy(alpha = alpha)))
                    }
                }
            }
        }

        // Error bar
        val displayError = state.error ?: state.voiceError
        if (displayError != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(ErrorRed.copy(alpha = 0.15f)).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(displayError, color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::dismissError) { Text("OK", color = ErrorRed) }
            }
        }

        // Input bar
        Row(
            modifier = Modifier.fillMaxWidth().background(Surface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.partialSpeechText.ifBlank { state.inputText },
                onValueChange = { if (state.partialSpeechText.isBlank()) viewModel.onInputChange(it) },
                placeholder = {
                    Text(
                        if (state.voiceMode == VoiceMode.CASA) "Dì \"Alfred\"..." else "Scrivi o tieni premuto il microfono...",
                        color = OnSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JennyPurpleLight,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = if (state.partialSpeechText.isNotBlank()) AccentGlow else OnBackground,
                    cursorColor = JennyPurpleLight
                ),
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            // Mic button
            val pulse = rememberInfiniteTransition(label = "mic")
            val micScale by pulse.animateFloat(
                1f, if (state.isListening) 1.25f else 1f, label = "micScale",
                animationSpec = infiniteRepeatable(tween(500, easing = EaseInOutSine), RepeatMode.Reverse)
            )
            Box(
                modifier = Modifier.size(48.dp).scale(micScale).clip(CircleShape)
                    .background(when {
                        state.isListening             -> ErrorRed
                        state.voiceMode == VoiceMode.CASA -> SuccessGreen.copy(alpha = 0.3f)
                        else                          -> SurfaceVariant
                    })
                    .then(
                        if (state.voiceMode == VoiceMode.OUTDOOR) {
                            Modifier.pointerInput(hasAudioPermission) {
                                detectTapGestures(onPress = {
                                    if (!hasAudioPermission) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return@detectTapGestures }
                                    viewModel.startOutdoorListening()
                                    tryAwaitRelease()
                                    viewModel.stopOutdoorListening()
                                })
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Microfono",
                    tint = if (state.isListening) OnBackground else OnSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = viewModel::sendMessage,
                enabled = state.inputText.isNotBlank() && !state.isLoading,
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(if (state.inputText.isNotBlank()) JennyPurple else SurfaceVariant)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Invia", tint = OnBackground)
            }
        }
    }
}

@Composable
private fun JennyVoiceModeSelector(currentMode: VoiceMode, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceVariant).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Home, contentDescription = null, tint = if (currentMode == VoiceMode.CASA) SuccessGreen else OnSurfaceVariant, modifier = Modifier.size(18.dp))
        Text("Casa", color = if (currentMode == VoiceMode.CASA) SuccessGreen else OnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Switch(
            checked = currentMode == VoiceMode.OUTDOOR,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = JennyPurpleLight, checkedTrackColor = JennyPurple, uncheckedThumbColor = SuccessGreen, uncheckedTrackColor = SuccessGreen.copy(alpha = 0.4f)),
            modifier = Modifier.scale(0.8f)
        )
        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = if (currentMode == VoiceMode.OUTDOOR) JennyPurpleLight else OnSurfaceVariant, modifier = Modifier.size(18.dp))
        Text("Outdoor", color = if (currentMode == VoiceMode.OUTDOOR) JennyPurpleLight else OnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(if (currentMode == VoiceMode.CASA) "Ascolto continuo" else "Tieni premuto", fontSize = 11.sp, color = OnSurfaceVariant)
    }
}

@Composable
private fun JennyMessageBubble(role: String, content: String) {
    val isUser = role == "user"
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Box(
            modifier = Modifier.widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp))
                .background(if (isUser) BubbleUser else JennyPurple.copy(alpha = 0.3f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(content, color = OnBackground, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun JennySpeakingWave() {
    val transition = rememberInfiniteTransition(label = "wave")
    repeat(4) { i ->
        val progress by transition.animateFloat(
            initialValue = 0f, targetValue = 1f, label = "wave$i",
            animationSpec = infiniteRepeatable(tween(1400, delayMillis = i * 280, easing = LinearEasing), RepeatMode.Restart)
        )
        Box(
            modifier = Modifier.size(44.dp).graphicsLayer {
                scaleX = 1f + progress * 2.2f; scaleY = 1f + progress * 2.2f; alpha = (1f - progress) * 0.5f
            }.clip(CircleShape).background(JennyPurple)
        )
    }
}
