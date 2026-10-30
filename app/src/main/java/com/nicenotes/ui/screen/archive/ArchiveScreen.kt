package com.nicenotes.ui.screen.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nicenotes.ui.screen.editor.NoteEditorSheet
import com.nicenotes.ui.screen.main.NoteListItem
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
    val notes by viewModel.notes.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }

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
                        "Archive",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary)
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
                Text("No archived notes", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black)
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(
                    items = notes,
                    key = { it.id }
                ) { note ->
                    // Swipe right to unarchive, swipe left to delete
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    viewModel.unarchiveNote(note)
                                    true
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    viewModel.deleteNote(note)
                                    true
                                }
                                else -> false
                            }
                        }
                    )

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
                                    if (isDelete) "Delete" else "Unarchive",
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
            }
        )
    }
}
