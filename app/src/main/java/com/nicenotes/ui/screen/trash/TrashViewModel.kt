package com.nicenotes.ui.screen.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicenotes.domain.model.Note
import com.nicenotes.domain.usecase.DeleteAllTrashedNotesUseCase
import com.nicenotes.domain.usecase.DeleteNoteUseCase
import com.nicenotes.domain.usecase.GetTrashedNotesUseCase
import com.nicenotes.domain.usecase.RestoreFromTrashUseCase
import com.nicenotes.domain.usecase.RestoreToTrashUseCase
import com.nicenotes.ui.common.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    getTrashedNotesUseCase: GetTrashedNotesUseCase,
    private val restoreFromTrashUseCase: RestoreFromTrashUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val restoreToTrashUseCase: RestoreToTrashUseCase,
    private val deleteAllTrashedNotesUseCase: DeleteAllTrashedNotesUseCase,
) : ViewModel() {

    val notes = getTrashedNotesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = Channel<UiEvent>()
    val events = _events.receiveAsFlow()
    private val permanentlyDeletedNotes = mutableMapOf<String, Note>()

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            restoreFromTrashUseCase(note)
            _events.send(UiEvent.ShowSnackbar(message = "Note restored"))
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            permanentlyDeletedNotes[note.id] = note
            deleteNoteUseCase(note)
            _events.send(
                UiEvent.ShowSnackbar(
                    message = "Note permanently deleted",
                    action = "UNDO",
                    actionType = UiEvent.SnackbarActionType.RESTORE_TO_TRASH,
                    noteId = note.id
                )
            )
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            deleteAllTrashedNotesUseCase()
            permanentlyDeletedNotes.clear()
            _events.send(UiEvent.ShowSnackbar(message = "Trash emptied"))
        }
    }

    fun handleSnackbarAction(actionType: UiEvent.SnackbarActionType, noteId: String) {
        if (actionType == UiEvent.SnackbarActionType.RESTORE_TO_TRASH) {
            viewModelScope.launch {
                permanentlyDeletedNotes.remove(noteId)?.let { note ->
                    restoreToTrashUseCase(note)
                }
            }
        }
    }

    fun discardPermanentlyDeletedNote(noteId: String) {
        permanentlyDeletedNotes.remove(noteId)
    }
}
