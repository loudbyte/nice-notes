package com.nicenotes.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicenotes.data.preferences.NotePreferences
import com.nicenotes.domain.model.Note
import com.nicenotes.domain.usecase.ArchiveNoteUseCase
import com.nicenotes.domain.usecase.DeleteNoteUseCase
import com.nicenotes.domain.usecase.GetActiveNotesUseCase
import com.nicenotes.domain.usecase.GetNoteByIdUseCase
import com.nicenotes.domain.usecase.ReorderNotesUseCase
import com.nicenotes.domain.usecase.SaveNoteUseCase
import com.nicenotes.domain.usecase.SearchNotesUseCase
import com.nicenotes.ui.common.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getActiveNotesUseCase: GetActiveNotesUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val archiveNoteUseCase: ArchiveNoteUseCase,
    private val reorderNotesUseCase: ReorderNotesUseCase,
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val notePreferences: NotePreferences,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _remindersFirst = MutableStateFlow(notePreferences.remindersFirst)
    val remindersFirst = _remindersFirst.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val matchingNotes = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) getActiveNotesUseCase()
            else searchNotesUseCase(query)
        }

    val notes = combine(matchingNotes, _remindersFirst) { currentNotes, showRemindersFirst ->
        if (showRemindersFirst) currentNotes.withRemindersFirst() else currentNotes
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isManagementMode = MutableStateFlow(false)
    val isManagementMode = _isManagementMode.asStateFlow()

    private val _events = Channel<UiEvent>()
    val events = _events.receiveAsFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRemindersFirst(enabled: Boolean) {
        if (_remindersFirst.value == enabled) return
        notePreferences.setRemindersFirst(enabled)
        _remindersFirst.value = enabled
    }

    fun toggleManagementMode() {
        _isManagementMode.value = !_isManagementMode.value
    }

    fun exitManagementMode() {
        _isManagementMode.value = false
    }

    fun archiveNote(note: Note) {
        viewModelScope.launch {
            archiveNoteUseCase(note)
            _events.send(UiEvent.ShowSnackbar(
                message = "Note archived",
                action = "UNDO",
                actionType = UiEvent.SnackbarActionType.UNARCHIVE_NOTE,
                noteId = note.id
            ))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            deleteNoteUseCase(note)
            _events.send(UiEvent.ShowSnackbar(
                message = "Note deleted",
                action = "UNDO",
                actionType = UiEvent.SnackbarActionType.RESTORE_NOTE,
                noteId = note.id
            ))
        }
    }

    fun handleSnackbarAction(actionType: UiEvent.SnackbarActionType, noteId: String) {
        when (actionType) {
            UiEvent.SnackbarActionType.UNARCHIVE_NOTE -> unarchiveNote(noteId)
            UiEvent.SnackbarActionType.RESTORE_NOTE -> restoreNote(noteId)
            UiEvent.SnackbarActionType.ARCHIVE_NOTE -> Unit
            UiEvent.SnackbarActionType.RESTORE_ARCHIVED_NOTE -> Unit
        }
    }

    private fun unarchiveNote(noteId: String) {
        viewModelScope.launch {
            val note = getNoteByIdUseCase(noteId) ?: return@launch
            saveNoteUseCase(note.copy(isArchived = false, isDeleted = false))
        }
    }

    private fun restoreNote(noteId: String) {
        viewModelScope.launch {
            val note = getNoteByIdUseCase(noteId) ?: return@launch
            saveNoteUseCase(note)
        }
    }

    fun reorderNotes(updates: List<Pair<String, Long>>) {
        viewModelScope.launch {
            reorderNotesUseCase(updates)
        }
    }

    suspend fun getNoteById(id: String): Note? = getNoteByIdUseCase(id)
}

internal fun List<Note>.withRemindersFirst(): List<Note> {
    val (notesWithReminders, notesWithoutReminders) = partition { it.alarmTime != null }
    return notesWithReminders + notesWithoutReminders
}
