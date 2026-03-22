package com.alfredJenny.app.ui.screens.avatar

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.alfredJenny.app.permissions.PermissionUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.ui.theme.*
import kotlinx.coroutines.delay

// ── Entry point ───────────────────────────────────────────────────────────────

/**
 * Avatar manager screen.
 * @param mode "alfred" or "jenny" — determines which tab opens by default.
 */
@Composable
fun AvatarManagerScreen(
    mode: String = "alfred",
    onBack: () -> Unit,
    viewModel: AvatarManagerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(if (mode == "jenny") 1 else 0) }

    // Feedback snackbar
    state.feedback?.let { msg ->
        LaunchedEffect(msg) { delay(2500); viewModel.dismissFeedback() }
    }

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
                "Gestione Avatar",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.weight(1f)
            )
        }

        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = AlfredBlueLight,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Alfred", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Jenny", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AlfredAvatarTab(state = state, viewModel = viewModel)
                1 -> JennyAvatarTab(state = state, viewModel = viewModel)
            }
        }

        // Feedback banner
        state.feedback?.let { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = SuccessGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(12.dp),
                    color = SuccessGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Alfred tab ────────────────────────────────────────────────────────────────

@Composable
private fun AlfredAvatarTab(
    state: AvatarManagerUiState,
    viewModel: AvatarManagerViewModel,
) {
    var pendingFilename by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingFilename?.let { viewModel.importAlfredFile(it, uri) }
        pendingFilename = null
    }

    val context = LocalContext.current
    val imagePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permission result — user can retry by tapping again */ }

    fun launchImagePickerWithPermission(action: () -> Unit) {
        if (PermissionUtils.areImagesGranted(context)) {
            action()
        } else {
            imagePermLauncher.launch(PermissionUtils.IMAGE_PERMISSIONS)
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AlfredBlueLight)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Info card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AlfredBlue.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = AlfredBlueLight, modifier = Modifier.size(28.dp))
                Column {
                    Text("Sprite PNG animati", color = OnBackground,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Importa PNG 512×512 trasparente per personalizzare i frame.",
                        color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Sprite groups
        val groupOrder = listOf("IDLE", "TALKING", "THINKING", "LISTENING")
        groupOrder.forEach { group ->
            val slots = state.alfredSpriteSlots[group] ?: return@forEach
            HorizontalDivider(color = SurfaceVariant)
            Text(
                group,
                fontWeight = FontWeight.SemiBold,
                color = AlfredBlueLight,
                fontSize = 15.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEach { slot ->
                    AlfredSlotRow(
                        slot = slot,
                        onImport = {
                            pendingFilename = slot.filename
                            launchImagePickerWithPermission { launcher.launch("image/*") }
                        },
                        onRemove = { viewModel.removeAlfredFile(slot.filename) },
                    )
                }
            }
        }

        HorizontalDivider(color = SurfaceVariant)

        // Reset button
        val hasAnyCustom = state.alfredSpriteSlots.values.flatten().any { it.hasFilesDir }
        if (hasAnyCustom) {
            OutlinedButton(
                onClick = { viewModel.resetAllAlfred() },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null,
                    tint = ErrorRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ripristina sprite default", color = ErrorRed)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AlfredSlotRow(
    slot: AvatarSlot,
    onImport: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (slot.hasFilesDir) AlfredBlue.copy(alpha = 0.1f) else SurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (slot.thumbnail != null) {
                    Image(
                        bitmap = slot.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null,
                        tint = OnSurfaceVariant, modifier = Modifier.size(28.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(slot.label, color = OnBackground, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(
                    text = if (slot.hasFilesDir) "Personalizzato" else "Default (asset)",
                    color = if (slot.hasFilesDir) SuccessGreen else AlfredBlueLight,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            IconButton(onClick = onImport, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FileUpload, contentDescription = "Importa",
                    tint = AlfredBlueLight, modifier = Modifier.size(20.dp))
            }
            if (slot.hasFilesDir) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi",
                        tint = ErrorRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ── Jenny tab ─────────────────────────────────────────────────────────────────

@Composable
private fun JennyAvatarTab(
    state: AvatarManagerUiState,
    viewModel: AvatarManagerViewModel,
) {
    // Keep track of which slot is waiting for the file picker
    var pendingFilename by remember { mutableStateOf<String?>(null) }
    var pendingCustomIndex by remember { mutableStateOf<Int?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingFilename?.let { viewModel.importJennyFile(it, uri) }
        pendingCustomIndex?.let { viewModel.importCustomOutfit(it, uri) }
        pendingFilename = null
        pendingCustomIndex = null
    }

    val context = LocalContext.current
    val imagePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permission result — user can retry by tapping again */ }

    fun launchImagePickerWithPermission(action: () -> Unit) {
        if (PermissionUtils.areImagesGranted(context)) {
            action()
        } else {
            imagePermLauncher.launch(PermissionUtils.IMAGE_PERMISSIONS)
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = JennyPurpleLight)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Body (outfit) ─────────────────────────────────────────────────────
        JennySlotSection(
            title = "Body (Outfit)",
            slots = state.jennyBodySlots,
            onImport = { filename ->
                pendingFilename = filename
                pendingCustomIndex = null
                launchImagePickerWithPermission { launcher.launch("image/*") }
            },
            onRemove = viewModel::removeJennyFile,
        )

        HorizontalDivider(color = SurfaceVariant)

        // ── Eyes ──────────────────────────────────────────────────────────────
        JennySlotSection(
            title = "Occhi",
            slots = state.jennyEyeSlots,
            onImport = { filename ->
                pendingFilename = filename
                pendingCustomIndex = null
                launchImagePickerWithPermission { launcher.launch("image/*") }
            },
            onRemove = viewModel::removeJennyFile,
        )

        HorizontalDivider(color = SurfaceVariant)

        // ── Mouth ─────────────────────────────────────────────────────────────
        JennySlotSection(
            title = "Bocca",
            slots = state.jennyMouthSlots,
            onImport = { filename ->
                pendingFilename = filename
                pendingCustomIndex = null
                launchImagePickerWithPermission { launcher.launch("image/*") }
            },
            onRemove = viewModel::removeJennyFile,
        )

        HorizontalDivider(color = SurfaceVariant)

        // ── Custom outfits ────────────────────────────────────────────────────
        Text(
            "Outfit personalizzati (max 6)",
            fontWeight = FontWeight.SemiBold,
            color = JennyPurpleLight,
            fontSize = 15.sp
        )
        Text(
            "Gli outfit importati appariranno automaticamente nella barra outfit di Jenny.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )

        state.customOutfits.forEach { slot ->
            CustomOutfitRow(
                slot = slot,
                onNameChange = { viewModel.updateCustomOutfitNameLocal(slot.index, it) },
                onNameSave = { viewModel.saveCustomOutfitName(slot.index, slot.name) },
                onImport = {
                    pendingFilename = null
                    pendingCustomIndex = slot.index
                    launchImagePickerWithPermission { launcher.launch("image/*") }
                },
                onRemove = { viewModel.removeCustomOutfit(slot.index) },
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Jenny slot section ────────────────────────────────────────────────────────

@Composable
private fun JennySlotSection(
    title: String,
    slots: List<AvatarSlot>,
    onImport: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Text(title, fontWeight = FontWeight.SemiBold, color = JennyPurpleLight, fontSize = 15.sp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.forEach { slot ->
            JennySlotRow(slot = slot, onImport = { onImport(slot.filename) }, onRemove = { onRemove(slot.filename) })
        }
    }
}

@Composable
private fun JennySlotRow(
    slot: AvatarSlot,
    onImport: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail
            ThumbnailBox(bitmap = slot.thumbnail, hasContent = slot.hasFilesDir || slot.hasAssets)

            // Label + status
            Column(modifier = Modifier.weight(1f)) {
                Text(slot.label, color = OnBackground, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(
                    text = when {
                        slot.hasFilesDir -> "✅ Personalizzato"
                        slot.hasAssets   -> "📦 Default (asset)"
                        else             -> "❌ Mancante"
                    },
                    color = when {
                        slot.hasFilesDir -> SuccessGreen
                        slot.hasAssets   -> AlfredBlueLight
                        else             -> ErrorRed
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Actions
            IconButton(onClick = onImport, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FileUpload, contentDescription = "Importa",
                    tint = JennyPurpleLight, modifier = Modifier.size(20.dp))
            }
            if (slot.hasFilesDir) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi",
                        tint = ErrorRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ── Custom outfit row ─────────────────────────────────────────────────────────

@Composable
private fun CustomOutfitRow(
    slot: CustomOutfitSlot,
    onNameChange: (String) -> Unit,
    onNameSave: () -> Unit,
    onImport: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (slot.hasFilesDir) JennyPurple.copy(alpha = 0.1f) else SurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.5.dp,
                if (slot.hasFilesDir) JennyPurple else SurfaceVariant,
                RoundedCornerShape(10.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThumbnailBox(bitmap = slot.thumbnail, hasContent = slot.hasFilesDir)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Outfit ${slot.index + 1}",
                        color = if (slot.hasFilesDir) JennyPurpleLight else OnSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text(
                        if (slot.hasFilesDir) "✅ PNG importato" else "⚠️ Nessun file",
                        color = if (slot.hasFilesDir) SuccessGreen else OnSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(onClick = onImport, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Importa",
                        tint = JennyPurpleLight, modifier = Modifier.size(20.dp))
                }
                if (slot.hasFilesDir) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Rimuovi",
                            tint = ErrorRed, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (slot.hasFilesDir) {
                OutlinedTextField(
                    value = slot.name,
                    onValueChange = onNameChange,
                    label = { Text("Nome outfit") },
                    placeholder = { Text("Es. Gym, Lavoro…", color = OnSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = onNameSave) {
                            Icon(Icons.Default.Save, contentDescription = "Salva",
                                tint = JennyPurpleLight, modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JennyPurpleLight,
                        unfocusedBorderColor = OnSurfaceVariant,
                        focusedLabelColor = JennyPurpleLight,
                        cursorColor = JennyPurpleLight,
                        focusedTextColor = OnBackground,
                        unfocusedTextColor = OnBackground,
                    )
                )
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun ThumbnailBox(bitmap: ImageBitmap?, hasContent: Boolean) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else if (hasContent) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = JennyPurpleLight,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.HideImage, contentDescription = null,
                tint = OnSurfaceVariant, modifier = Modifier.size(28.dp))
        }
    }
}

