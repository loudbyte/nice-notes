package com.nicenotes.ui.screen.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nicenotes.ui.screen.editor.NoteEditorSheet
import com.nicenotes.ui.screen.main.NoteListItem
import com.nicenotes.ui.common.AppSnackbarHost
import com.nicenotes.ui.common.UiEvent
import com.nicenotes.ui.theme.*
import com.nicenotes.domain.model.Note
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val archivedCount by viewModel.archivedCount.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var swipeStateResetVersion by remember { mutableIntStateOf(0) }
    var showMoveAllConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.action,
                        duration = SnackbarDuration.Short
                    )
                    if (
                        result == SnackbarResult.ActionPerformed &&
                        event.actionType != null &&
                        event.noteId != null
                    ) {
                        viewModel.handleSnackbarAction(event.actionType, event.noteId)
                        if (
                            event.actionType == UiEvent.SnackbarActionType.ARCHIVE_NOTE ||
                            event.actionType == UiEvent.SnackbarActionType.RESTORE_ARCHIVED_NOTE
                        ) {
                            swipeStateResetVersion++
                        }
                    } else if (
                        event.actionType == UiEvent.SnackbarActionType.RESTORE_ARCHIVED_NOTE &&
                        event.noteId != null
                    ) {
                        viewModel.discardDeletedNote(event.noteId)
                    } else if (
                        event.actionType == UiEvent.SnackbarActionType.ARCHIVE_NOTE &&
                        event.noteId != null
                    ) {
                        viewModel.discardUnarchivedNote(event.noteId)
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Black,
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Archive",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showMoveAllConfirmation = true },
                        enabled = archivedCount > 0
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = if (archivedCount > 0) ImportantRed else TextSecondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Move all to Trash",
                            color = if (archivedCount > 0) ImportantRed else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Black)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = {
                    Text("Search archived notes...", color = TextSecondary)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentWhite.copy(alpha = 0.3f),
                    unfocusedBorderColor = Divider,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentWhite,
                    focusedContainerColor = SurfaceVariant,
                    unfocusedContainerColor = SurfaceVariant,
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isBlank()) "No archived notes" else "No matching notes",
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Black),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(
                        items = notes,
                        key = { it.id }
                    ) { note ->
                        // Swipe right to unarchive, swipe left to move to Trash.
                        val dismissState = key(swipeStateResetVersion) {
                            rememberSwipeToDismissBoxState()
                        }
                        val currentNote by rememberUpdatedState(note)
                        LaunchedEffect(note.id, dismissState) {
                            snapshotFlow { dismissState.settledValue }.collect { settledValue ->
                                when (settledValue) {
                                    SwipeToDismissBoxValue.StartToEnd ->
                                        viewModel.unarchiveNote(currentNote)
                                    SwipeToDismissBoxValue.EndToStart ->
                                        viewModel.deleteNote(currentNote)
                                    SwipeToDismissBoxValue.Settled -> Unit
                                }
                            }
                        }

                        val isDelete = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .background(
                                            if (isDelete) ImportantRed else ReminderBlue,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Text(
                                        if (isDelete) "Move to Trash" else "Unarchive",
                                        color = AccentWhite,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true,
                        ) {
                            NoteListItem(
                                note = note,
                                isManagementMode = false,
                                onClick = {
                                    editingNote = note
                                    showEditor = true
                                },
                                onLongClick = {},
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor && editingNote != null) {
        NoteEditorSheet(
            note = editingNote,
            onDismiss = { showEditor = false },
            onMessage = { message, action ->
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = action?.let { "SETTINGS" },
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        action?.invoke()
                    }
                }
            },
            onDelete = viewModel::deleteNote,
        )
    }

    if (showMoveAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showMoveAllConfirmation = false },
            title = { Text("Move all notes to Trash?") },
            text = { Text("This moves $archivedCount notes to Trash.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMoveAllConfirmation = false
                        viewModel.moveAllToTrash()
                    }
                ) {
                    Text("Move all", color = ImportantRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveAllConfirmation = false }) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceVariant,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
        )
    }
}
