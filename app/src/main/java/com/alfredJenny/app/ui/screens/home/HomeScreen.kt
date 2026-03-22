package com.alfredJenny.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.CompanionDto
import com.alfredJenny.app.data.model.VoiceMode
import com.alfredJenny.app.ui.components.AlfredAvatarView
import com.alfredJenny.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
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

    // Auto-scroll: on new messages or while streaming
    val totalItems = state.messages.size +
            (if (state.isLoading && state.streamingContent.isBlank()) 1 else 0) +
            (if (state.streamingContent.isNotBlank()) 1 else 0)
    LaunchedEffect(state.messages.size, state.streamingContent.isNotBlank()) {
        if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
    }

    // Auto-dismiss fallback notice
    LaunchedEffect(state.fallbackNotice) {
        if (state.fallbackNotice != null) {
            delay(3000)
            viewModel.dismissFallbackNotice()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().background(Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { avatarExpanded = !avatarExpanded }) {
                Box(contentAlignment = Alignment.Center) {
                    CompanionAvatar(
                        companion = state.companions.firstOrNull { it.id == state.selectedCompanionId },
                        isThinking = state.isLoading
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        state.companions.firstOrNull { it.id == state.selectedCompanionId }?.name ?: "Alfred",
                        fontWeight = FontWeight.Bold, color = OnBackground, fontSize = 18.sp
                    )
                    // Provider badge
                    if (state.activeProvider.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(5.dp), color = AlfredBlue.copy(alpha = 0.22f)) {
                            Text(
                                state.activeProvider.uppercase(),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                color = AlfredBlueLight, fontSize = 9.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    when {
                        state.isSpeaking             -> "parla..."
                        state.streamingContent.isNotBlank() -> "scrive..."
                        state.isLoading              -> "pensa..."
                        state.isListening            -> "ti ascolto..."
                        else                         -> "online"
                    },
                    color = when {
                        state.isSpeaking             -> AccentGlow
                        state.streamingContent.isNotBlank() -> AlfredBlueLight
                        state.isLoading              -> AccentGlow
                        state.isListening            -> SuccessGreen
                        else                         -> SuccessGreen
                    },
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = viewModel::toggleVoice) {
                Icon(
                    if (state.voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Voce",
                    tint = if (state.voiceEnabled) AccentGlow else OnSurfaceVariant
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = OnSurfaceVariant)
            }
        }

        // ── Fallback notice ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.fallbackNotice != null,
            enter = expandVertically(), exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(AccentGlow.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null,
                    tint = AccentGlow, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(state.fallbackNotice ?: "", color = AccentGlow,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::dismissFallbackNotice, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = AccentGlow, modifier = Modifier.size(14.dp))
                }
            }
        }

        // ── Avatar panel (collapsible) ────────────────────────────────────────
        AnimatedVisibility(visible = avatarExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            Box(
                modifier = Modifier.fillMaxWidth().background(Background),
                contentAlignment = Alignment.Center
            ) {
                if (state.isSpeaking) SpeakingWave()
                AlfredAvatarView(state = state.avatarState)
            }
        }

        // ── Voice mode selector ───────────────────────────────────────────────
        VoiceModeSelector(
            currentMode = state.voiceMode,
            onToggle = {
                if (!hasAudioPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                else viewModel.toggleVoiceMode()
            }
        )

        // ── Companion tabs (admin only) ───────────────────────────────────────
        if (state.companions.size > 1) {
            CompanionSelectorRow(
                companions = state.companions,
                selectedId = state.selectedCompanionId,
                onSelect = viewModel::onCompanionSelected
            )
        }

        // ── Messages ──────────────────────────────────────────────────────────
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            state = rememberPullToRefreshState(),
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(state.messages, key = { it.id }) { msg -> MessageBubble(msg) }
                // Dots: only while waiting for first chunk
                if (state.isLoading && state.streamingContent.isBlank()) {
                    item(key = "typing") { TypingIndicator() }
                }
                // Streaming bubble: in-progress text arriving word by word
                if (state.streamingContent.isNotBlank()) {
                    item(key = "streaming") { StreamingBubble(state.streamingContent) }
                }
            }
        }

        // ── Error bar ─────────────────────────────────────────────────────────
        val displayError = state.error ?: state.voiceError
        if (displayError != null) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(ErrorRed.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(displayError, color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::dismissError) { Text("OK", color = ErrorRed) }
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().background(Surface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.partialSpeechText.ifBlank { state.inputText },
                onValueChange = { if (state.partialSpeechText.isBlank()) viewModel.onInputChange(it) },
                placeholder = {
                    Text(
                        if (state.voiceMode == VoiceMode.CASA) "Dì \"Alfred\"..."
                        else "Scrivi o tieni premuto il microfono...",
                        color = OnSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AlfredBlueLight,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = if (state.partialSpeechText.isNotBlank()) AccentGlow else OnBackground,
                    cursorColor = AlfredBlueLight
                ),
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            MicButton(
                mode = state.voiceMode,
                isListening = state.isListening,
                hasPermission = hasAudioPermission,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                onPressStart = viewModel::startOutdoorListening,
                onPressEnd = viewModel::stopOutdoorListening
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = viewModel::sendMessage,
                enabled = state.inputText.isNotBlank() && !state.isLoading,
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(if (state.inputText.isNotBlank()) AlfredBlue else SurfaceVariant)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Invia", tint = OnBackground)
            }
        }
    }
}

// ── Streaming bubble ──────────────────────────────────────────────────────────

@Composable
private fun StreamingBubble(content: String) {
    // Blinking cursor
    val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        0f, 1f, label = "cur",
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                .background(BubbleAssistant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = "$content▌".dropLast(if (cursorAlpha < 0.5f) 1 else 0),
                color = OnBackground.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── Voice mode selector ───────────────────────────────────────────────────────

@Composable
private fun VoiceModeSelector(currentMode: VoiceMode, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Home, contentDescription = null,
            tint = if (currentMode == VoiceMode.CASA) SuccessGreen else OnSurfaceVariant,
            modifier = Modifier.size(18.dp))
        Text("Casa", color = if (currentMode == VoiceMode.CASA) SuccessGreen else OnSurfaceVariant,
            fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Switch(
            checked = currentMode == VoiceMode.OUTDOOR,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = AlfredBlueLight, checkedTrackColor = AlfredBlue,
                uncheckedThumbColor = SuccessGreen, uncheckedTrackColor = SuccessGreen.copy(alpha = 0.4f)
            ),
            modifier = Modifier.scale(0.8f)
        )
        Icon(Icons.Default.DirectionsWalk, contentDescription = null,
            tint = if (currentMode == VoiceMode.OUTDOOR) AlfredBlueLight else OnSurfaceVariant,
            modifier = Modifier.size(18.dp))
        Text("Outdoor", color = if (currentMode == VoiceMode.OUTDOOR) AlfredBlueLight else OnSurfaceVariant,
            fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(if (currentMode == VoiceMode.CASA) "Ascolto continuo" else "Tieni premuto",
            fontSize = 11.sp, color = OnSurfaceVariant)
    }
}

// ── Mic button ────────────────────────────────────────────────────────────────

@Composable
private fun MicButton(
    mode: VoiceMode,
    isListening: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "mic")
    val scale by pulse.animateFloat(
        1f, if (isListening) 1.25f else 1f, label = "micScale",
        animationSpec = infiniteRepeatable(tween(500, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier.size(48.dp).scale(scale).clip(CircleShape)
            .background(when {
                isListening             -> ErrorRed
                mode == VoiceMode.CASA  -> SuccessGreen.copy(alpha = 0.3f)
                else                    -> SurfaceVariant
            })
            .then(
                if (mode == VoiceMode.OUTDOOR) {
                    Modifier.pointerInput(hasPermission) {
                        detectTapGestures(onPress = {
                            if (!hasPermission) { onRequestPermission(); return@detectTapGestures }
                            onPressStart()
                            tryAwaitRelease()
                            onPressEnd()
                        })
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = "Microfono",
            tint = if (isListening) OnBackground else OnSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ── Speaking wave ─────────────────────────────────────────────────────────────

@Composable
private fun SpeakingWave() {
    val transition = rememberInfiniteTransition(label = "wave")
    repeat(4) { i ->
        val progress by transition.animateFloat(
            0f, 1f, label = "wave$i",
            animationSpec = infiniteRepeatable(tween(1400, delayMillis = i * 280, easing = LinearEasing), RepeatMode.Restart)
        )
        Box(
            modifier = Modifier.size(44.dp).graphicsLayer {
                scaleX = 1f + progress * 2.2f; scaleY = 1f + progress * 2.2f; alpha = (1f - progress) * 0.5f
            }.clip(CircleShape).background(AlfredBlue)
        )
    }
}

// ── Companion avatar ──────────────────────────────────────────────────────────

@Composable
private fun CompanionAvatar(companion: CompanionDto?, isThinking: Boolean) {
    val colors = if (companion?.id == "jenny") listOf(JennyPurpleLight, JennyPurpleDark)
                 else listOf(AlfredBlueLight, AlfredBlueDark)
    val pulse = rememberInfiniteTransition(label = "avatar")
    val scale by pulse.animateFloat(
        1f, if (isThinking) 1.15f else 1.04f, label = "avatarScale",
        animationSpec = infiniteRepeatable(tween(if (isThinking) 600 else 2000, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier.size(44.dp).scale(scale).clip(CircleShape)
            .background(Brush.radialGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(companion?.name?.first()?.toString() ?: "A", color = OnBackground,
            fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

// ── Companion selector tabs ───────────────────────────────────────────────────

@Composable
private fun CompanionSelectorRow(companions: List<CompanionDto>, selectedId: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceVariant).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        companions.forEach { c ->
            val isSelected = c.id == selectedId
            val accent = if (c.id == "jenny") JennyPurple else AlfredBlue
            val accentLight = if (c.id == "jenny") JennyPurpleLight else AlfredBlueLight
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) accent else accent.copy(alpha = 0.2f),
                modifier = Modifier.clickable { onSelect(c.id) }
            ) {
                Text(c.name, color = if (isSelected) OnBackground else accentLight,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            }
        }
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: ConversationEntity) {
    val isUser = message.role == "user"
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Box(
            modifier = Modifier.widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ))
                .background(if (isUser) BubbleUser else BubbleAssistant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(message.content, color = OnBackground, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Typing indicator ──────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val t = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(BubbleAssistant)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        repeat(3) { i ->
            val alpha by t.animateFloat(0.3f, 1f, label = "dot$i",
                animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 150), RepeatMode.Reverse))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(OnSurfaceVariant.copy(alpha = alpha)))
        }
    }
}
