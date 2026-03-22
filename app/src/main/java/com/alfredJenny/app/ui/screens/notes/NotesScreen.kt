package com.alfredJenny.app.ui.screens.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.local.MemoEntity
import com.alfredJenny.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    viewModel: NotesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        if (state.memos.isEmpty()) {
            EmptyNotesPlaceholder(modifier = Modifier.align(Alignment.Center))
        } else {
            NotesGrid(
                memos = state.memos,
                onLongPress = { viewModel.startEdit(it) },
                onPin = { viewModel.togglePin(it) },
                onDelete = { viewModel.requestDelete(it) },
                modifier = Modifier.fillMaxSize()
            )
        }

        FloatingActionButton(
            onClick = { viewModel.startCreate() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = AlfredBlue,
            contentColor = Color.White,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuova nota")
        }
    }

    // Edit/Create dialog
    if (state.editingMemo != null) {
        MemoEditDialog(
            title = state.editTitle,
            content = state.editContent,
            onTitleChange = viewModel::onEditTitle,
            onContentChange = viewModel::onEditContent,
            onConfirm = viewModel::saveEdit,
            onDismiss = viewModel::cancelEdit,
        )
    }

    // Delete confirmation
    state.showDeleteConfirm?.let { memo ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Elimina nota") },
            text = { Text("Vuoi eliminare \"${memo.title}\"?") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Annulla") }
            },
            containerColor = Surface,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesGrid(
    memos: List<MemoEntity>,
    onLongPress: (MemoEntity) -> Unit,
    onPin: (MemoEntity) -> Unit,
    onDelete: (MemoEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(memos, key = { it.id }) { memo ->
            MemoCard(
                memo = memo,
                onLongPress = { onLongPress(memo) },
                onPin = { onPin(memo) },
                onDelete = { onDelete(memo) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoCard(
    memo: MemoEntity,
    onLongPress: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    val cardColor = if (memo.companion == "jenny") JennyPurple.copy(alpha = 0.15f)
                    else AlfredBlue.copy(alpha = 0.12f)
    val badgeColor = if (memo.companion == "jenny") JennyPurpleLight else AlfredBlueLight
    val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (memo.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Fissato",
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = memo.companion.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = dateFmt.format(Date(memo.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = memo.title,
                style = MaterialTheme.typography.titleSmall,
                color = OnBackground,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (memo.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = memo.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Modifica") },
                onClick = { showMenu = false; onLongPress() },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(if (memo.isPinned) "Rimuovi pin" else "Fissa") },
                onClick = { showMenu = false; onPin() },
                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Elimina", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

@Composable
private fun MemoEditDialog(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (title.isBlank() && content.isBlank()) "Nuova nota" else "Modifica nota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Titolo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("Contenuto") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = title.isNotBlank()) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
        containerColor = Surface,
    )
}

@Composable
private fun EmptyNotesPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("📝", style = MaterialTheme.typography.displayMedium)
        Text(
            "Nessuna nota",
            style = MaterialTheme.typography.titleMedium,
            color = OnSurfaceVariant,
        )
        Text(
            "Chiedi ad Alfred o Jenny di creare una nota,\noppure usa il + in basso.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
        )
    }
}
