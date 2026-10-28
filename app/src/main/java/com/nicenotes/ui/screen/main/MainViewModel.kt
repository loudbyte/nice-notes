package com.nicenotes.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicenotes.data.db.entity.NoteEntity
import com.nicenotes.domain.usecase.ArchiveNoteUseCase
import com.nicenotes.domain.usecase.DeleteNoteUseCase
import com.nicenotes.domain.usecase.GetActiveNotesUseCase
import com.nicenotes.domain.usecase.GetNoteByIdUseCase
import com.nicenotes.domain.usecase.ReorderNotesUseCase
import com.nicenotes.domain.usecase.SaveNoteUseCase
import com.nicenotes.domain.usecase.SearchNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiEvent {
    data class ShowSnackbar(
        val message: String,
        val action: String?,
        val onAction: () -> Unit
    ) : UiEvent()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getActiveNotesUseCase: GetActiveNotesUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val archiveNoteUseCase: ArchiveNoteUseCase,
    private val reorderNotesUseCase: ReorderNotesUseCase,
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) getActiveNotesUseCase()
            else searchNotesUseCase(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isManagementMode = MutableStateFlow(false)
    val isManagementMode = _isManagementMode.asStateFlow()

    private val _events = Channel<UiEvent>()
    val events = _events.receiveAsFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleManagementMode() {
        _isManagementMode.value = !_isManagementMode.value
    }

    fun exitManagementMode() {
        _isManagementMode.value = false
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch {
            archiveNoteUseCase(note)
            _events.send(UiEvent.ShowSnackbar(
                message = "Note archived",
                action = "UNDO",
                onAction = { unarchiveNote(note) }
            ))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            deleteNoteUseCase(note)
            _events.send(UiEvent.ShowSnackbar(
                message = "Note deleted",
                action = "UNDO",
                onAction = { restoreNote(note) }
            ))
        }
    }

    private fun unarchiveNote(note: NoteEntity) {
        viewModelScope.launch {
            saveNoteUseCase(note.copy(isArchived = false, mIsDelete = false))
        }
    }

    private fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            saveNoteUseCase(note)
        }
    }

    fun reorderNotes(updates: List<Pair<String, Long>>) {
        viewModelScope.launch {
            reorderNotesUseCase(updates)
        }
    }

    suspend fun getNoteById(id: String): NoteEntity? = getNoteByIdUseCase(id)
}
