package com.nicenotes.ui.screen.trash

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
import com.nicenotes.domain.model.Note
import com.nicenotes.ui.common.UiEvent
import com.nicenotes.ui.screen.main.NoteListItem
import com.nicenotes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteAllConfirmation by remember { mutableStateOf(false) }
    var swipeStateResetVersion by remember { mutableIntStateOf(0) }

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
                        event.actionType == UiEvent.SnackbarActionType.RESTORE_TO_TRASH &&
                        event.noteId != null
                    ) {
                        viewModel.handleSnackbarAction(event.actionType, event.noteId)
                        swipeStateResetVersion++
                    } else if (
                        event.actionType == UiEvent.SnackbarActionType.RESTORE_TO_TRASH &&
                        event.noteId != null
                    ) {
                        viewModel.discardPermanentlyDeletedNote(event.noteId)
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Black,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceVariant,
                    contentColor = TextPrimary,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trash",
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
                        onClick = { showDeleteAllConfirmation = true },
                        enabled = notes.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = if (notes.isNotEmpty()) ImportantRed else TextSecondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Delete all",
                            color = if (notes.isNotEmpty()) ImportantRed else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Black)
            )
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Trash is empty", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black)
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(notes, key = Note::id) { note ->
                    val dismissState = key(swipeStateResetVersion) {
                        rememberSwipeToDismissBoxState()
                    }
                    val currentNote by rememberUpdatedState(note)
                    LaunchedEffect(note.id, dismissState) {
                        snapshotFlow { dismissState.settledValue }.collect { settledValue ->
                            when (settledValue) {
                                SwipeToDismissBoxValue.StartToEnd ->
                                    viewModel.permanentlyDeleteNote(currentNote)
                                SwipeToDismissBoxValue.EndToStart ->
                                    viewModel.restoreNote(currentNote)
                                SwipeToDismissBoxValue.Settled -> Unit
                            }
                        }
                    }

                    val isPermanentDelete =
                        dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .background(
                                        if (isPermanentDelete) ImportantRed else ReminderBlue,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = if (isPermanentDelete) {
                                    Alignment.CenterStart
                                } else {
                                    Alignment.CenterEnd
                                }
                            ) {
                                Text(
                                    if (isPermanentDelete) "Delete permanently" else "Restore",
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
                            onClick = {},
                            onLongClick = {},
                        )
                    }
                }
            }
        }
    }

    if (showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmation = false },
            title = { Text("Delete all notes in Trash?") },
            text = { Text("This permanently deletes ${notes.size} notes. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllConfirmation = false
                        viewModel.deleteAll()
                    }
                ) {
                    Text("Delete all", color = ImportantRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmation = false }) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceVariant,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
        )
    }
}
