package com.nicenotes.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.nicenotes.data.db.entity.NoteEntity
import com.nicenotes.ui.Screen
import com.nicenotes.ui.screen.editor.NoteEditorSheet
import com.nicenotes.ui.theme.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    onNavigate: (String) -> Unit,
    openNoteId: String? = null,
    openNoteRequestId: Long = 0,
    createNoteRequestId: Long? = null,
    viewModel: MainViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isManagementMode by viewModel.isManagementMode.collectAsState()
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val focusManager = LocalFocusManager.current
    var isSearchExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            searchFocusRequester.requestFocus()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showEditorSheet by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var pendingCreatedNoteId by remember { mutableStateOf<String?>(null) }
    var showDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(notes, pendingCreatedNoteId) {
        val createdNoteId = pendingCreatedNoteId
        if (createdNoteId != null && notes.any { it.mLocalId == createdNoteId }) {
            lazyListState.animateScrollToItem(0)
            pendingCreatedNoteId = null
        }
    }

    // Handle deep link from notification or widget
    var handledOpenNoteRequestId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(openNoteRequestId, openNoteId) {
        if (openNoteId != null && openNoteRequestId != 0L && openNoteRequestId != handledOpenNoteRequestId) {
            handledOpenNoteRequestId = openNoteRequestId
            val note = viewModel.getNoteById(openNoteId)
            if (note != null) {
                editingNote = note
                showEditorSheet = true
            }
        }
    }


    // Handle create note action from widget
    LaunchedEffect(createNoteRequestId) {
        if (createNoteRequestId != null) {
            editingNote = null
            showEditorSheet = true
        }
    }

    // Collect UI events (snackbar)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.action,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onAction()
                    }
                }
            }
        }
    }

    BackHandler(isManagementMode) {
        viewModel.exitManagementMode()
    }

    fun collapseSearch() {
        if (isSearchExpanded) {
            isSearchExpanded = false
            viewModel.setSearchQuery("")
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            var imeWasVisible = false
            snapshotFlow { imeInsets.getBottom(density) > 0 }.collect { isImeVisible ->
                if (isImeVisible) {
                    imeWasVisible = true
                } else if (imeWasVisible) {
                    collapseSearch()
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
        floatingActionButton = {
            if (!isManagementMode) {
                Box {
                    FloatingActionButton(
                        onClick = {
                            collapseSearch()
                            editingNote = null
                            showEditorSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New note")
                    }

                    // Scroll to top button
                    if (lazyListState.firstVisibleItemIndex > 0) {
                        FloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(0)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (-80).dp),
                            containerColor = Black,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Black)
                .padding(paddingValues)
                .clickable(
                    enabled = isSearchExpanded,
                    onClick = ::collapseSearch
                )
        ) {
            // Notes count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${notes.size} notes",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }

            // Top bar with search and menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) headerRow@{
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    this@headerRow.AnimatedVisibility(
                        visible = isSearchExpanded,
                        modifier = Modifier.fillMaxWidth(),
                        enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = {
                                Text("Search notes...", color = TextSecondary)
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
                                .clickable(enabled = false) {}
                                .focusRequester(searchFocusRequester),
                        )
                    }

                    // The field expands from this position when search opens.
                    if (!isSearchExpanded && !isManagementMode) {
                        IconButton(
                            onClick = {
                                isSearchExpanded = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                if (isSearchExpanded || !isManagementMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Three-dot menu
                Box {
                    IconButton(onClick = { showDropdown = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = TextSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        containerColor = SurfaceVariant,
                    ) {
                        DropdownMenuItem(
                            text = { Text("Archive", color = TextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Default.Archive, null, tint = TextSecondary)
                            },
                            onClick = {
                                showDropdown = false
                                onNavigate(Screen.Archive.route)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings", color = TextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, null, tint = TextSecondary)
                            },
                            onClick = {
                                showDropdown = false
                                onNavigate(Screen.Settings.route)
                            }
                        )
                    }
                }
            }

            // Management mode label
            if (isManagementMode) {
                Text(
                    text = "Management mode — drag to reorder",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Notes list
            if (!isManagementMode) {
                SwipeableNoteList(
                    notes = notes,
                    lazyListState = lazyListState,
                    isSearchExpanded = isSearchExpanded,
                    onSearchCollapse = ::collapseSearch,
                    onNoteClick = { note ->
                        collapseSearch()
                        editingNote = note
                        showEditorSheet = true
                    },
                    onNoteLongClick = { viewModel.toggleManagementMode() },
                    onNoteArchive = viewModel::archiveNote,
                )
            } else {
                ReorderableNoteList(
                    notes = notes,
                    isSearchExpanded = isSearchExpanded,
                    onSearchCollapse = ::collapseSearch,
                    onNoteClick = { note ->
                        collapseSearch()
                        editingNote = note
                        showEditorSheet = true
                    },
                    onNoteLongClick = { viewModel.exitManagementMode() },
                    onNoteDelete = viewModel::deleteNote,
                    onReorder = viewModel::reorderNotes,
                )
            }
        }
    }

    // Note editor sheet
    if (showEditorSheet) {
        NoteEditorSheet(
            note = editingNote,
            onDismiss = { showEditorSheet = false },
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
            onSaved = { savedNote ->
                if (editingNote == null) {
                    pendingCreatedNoteId = savedNote.mLocalId
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNoteList(
    notes: List<NoteEntity>,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    isSearchExpanded: Boolean,
    onSearchCollapse: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onNoteLongClick: (NoteEntity) -> Unit,
    onNoteArchive: (NoteEntity) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                enabled = isSearchExpanded,
                onClick = onSearchCollapse
            ),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(
            items = notes,
            key = { it.mLocalId }
        ) { note ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.StartToEnd) {
                        onNoteArchive(note)
                        true
                    } else false
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .background(ArchiveGreen, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "Archive",
                            color = AccentWhite,
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = false,
            ) {
                NoteListItem(
                    note = note,
                    isManagementMode = false,
                    onClick = { onNoteClick(note) },
                    onLongClick = { onNoteLongClick(note) },
                    onDelete = {},
                )
            }
        }
    }
}

@Composable
private fun ReorderableNoteList(
    notes: List<NoteEntity>,
    isSearchExpanded: Boolean,
    onSearchCollapse: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onNoteLongClick: (NoteEntity) -> Unit,
    onNoteDelete: (NoteEntity) -> Unit,
    onReorder: (List<Pair<String, Long>>) -> Unit,
) {
    var localNotes by remember(notes) { mutableStateOf(notes) }
    var pendingUpdates by remember { mutableStateOf<List<Pair<String, Long>>?>(null) }
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentPendingUpdates by rememberUpdatedState(pendingUpdates)

    LaunchedEffect(pendingUpdates) {
        val updates = pendingUpdates ?: return@LaunchedEffect
        delay(300L)
        currentOnReorder(updates)
        pendingUpdates = null
    }

    DisposableEffect(Unit) {
        onDispose {
            currentPendingUpdates?.let { currentOnReorder(it) }
        }
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localNotes = localNotes.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        pendingUpdates = localNotes.mapIndexed { index, note ->
            note.mLocalId to (localNotes.size - index).toLong() * 1000L
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                enabled = isSearchExpanded,
                onClick = onSearchCollapse
            ),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(
            items = localNotes,
            key = { it.mLocalId }
        ) { note ->
            ReorderableItem(reorderState, key = note.mLocalId) { isDragging ->
                NoteListItem(
                    note = note,
                    isManagementMode = true,
                    onClick = { onNoteClick(note) },
                    onLongClick = { onNoteLongClick(note) },
                    onDelete = { onNoteDelete(note) },
                    dragHandle = {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag",
                            tint = TextSecondary,
                            modifier = Modifier
                                .draggableHandle()
                                .size(24.dp)
                        )
                    },
                    modifier = if (isDragging) Modifier.background(SurfaceVariant) else Modifier
                )
            }
        }
    }
}
