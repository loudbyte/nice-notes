package com.nicenotes.ui.screen.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicenotes.domain.model.Note
import com.nicenotes.domain.usecase.ArchiveNoteUseCase
import com.nicenotes.domain.usecase.DeleteNoteUseCase
import com.nicenotes.domain.usecase.GetArchivedNotesUseCase
import com.nicenotes.domain.usecase.SaveNoteUseCase
import com.nicenotes.domain.usecase.UnarchiveNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.nicenotes.ui.common.UiEvent
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    getArchivedNotesUseCase: GetArchivedNotesUseCase,
    private val unarchiveNoteUseCase: UnarchiveNoteUseCase,
    private val archiveNoteUseCase: ArchiveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
) : ViewModel() {

    val notes = getArchivedNotesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = Channel<UiEvent>()
    val events = _events.receiveAsFlow()
    private val deletedNotes = mutableMapOf<String, Note>()

    fun unarchiveNote(note: Note) {
        viewModelScope.launch {
            unarchiveNoteUseCase(note)
            _events.send(UiEvent.ShowSnackbar(
                message = "Note unarchived",
                action = "UNDO",
                actionType = UiEvent.SnackbarActionType.ARCHIVE_NOTE,
                noteId = note.id
            ))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            deletedNotes[note.id] = note
            deleteNoteUseCase(note)
            _events.send(UiEvent.ShowSnackbar(
                message = "Note deleted",
                action = "UNDO",
                actionType = UiEvent.SnackbarActionType.RESTORE_ARCHIVED_NOTE,
                noteId = note.id
            ))
        }
    }

    fun handleSnackbarAction(actionType: UiEvent.SnackbarActionType, noteId: String) {
        when (actionType) {
            UiEvent.SnackbarActionType.ARCHIVE_NOTE -> {
                val note = notes.value.firstOrNull { it.id == noteId } ?: return
                archiveBack(note)
            }
            UiEvent.SnackbarActionType.RESTORE_ARCHIVED_NOTE -> restoreDeletedNote(noteId)
            UiEvent.SnackbarActionType.UNARCHIVE_NOTE -> Unit
            UiEvent.SnackbarActionType.RESTORE_NOTE -> Unit
        }
    }

    fun discardDeletedNote(noteId: String) {
        deletedNotes.remove(noteId)
    }

    private fun archiveBack(note: Note) {
        viewModelScope.launch {
            archiveNoteUseCase(note)
        }
    }

    private fun restoreDeletedNote(noteId: String) {
        viewModelScope.launch {
            val note = deletedNotes.remove(noteId)
                ?: notes.value.firstOrNull { it.id == noteId }
                ?: return@launch
            saveNoteUseCase(note)
        }
    }
}
