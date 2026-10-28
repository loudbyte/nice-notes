package com.nicenotes.ui.screen.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicenotes.data.db.entity.NoteEntity
import com.nicenotes.domain.usecase.DeleteNoteUseCase
import com.nicenotes.domain.usecase.GetArchivedNotesUseCase
import com.nicenotes.domain.usecase.UnarchiveNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.nicenotes.ui.screen.main.UiEvent
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    getArchivedNotesUseCase: GetArchivedNotesUseCase,
    private val unarchiveNoteUseCase: UnarchiveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
) : ViewModel() {

    val notes = getArchivedNotesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = Channel<UiEvent>()
    val events = _events.receiveAsFlow()

    fun unarchiveNote(note: NoteEntity) {
        viewModelScope.launch {
            unarchiveNoteUseCase(note)
            _events.send(UiEvent.ShowSnackbar(
                message = "Note unarchived",
                action = "UNDO",
                onAction = { archiveBack(note) }
            ))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            deleteNoteUseCase(note)
            _events.send(UiEvent.ShowSnackbar(
                message = "Note deleted",
                action = null,
                onAction = {}
            ))
        }
    }

    private fun archiveBack(note: NoteEntity) {
        viewModelScope.launch {
            // Re-archive by updating isArchived flag
            deleteNoteUseCase(note.copy(isArchived = false))
        }
    }
}
