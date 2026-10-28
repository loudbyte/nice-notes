package com.nicenotes.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicenotes.data.export.ImportExportManager
import com.nicenotes.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

sealed class SettingsEvent {
    data class Message(val text: String) : SettingsEvent()
    data class ShareExport(val jsonContent: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val importExportManager: ImportExportManager,
) : ViewModel() {

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun exportData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allNotes = noteRepository.getAllNotes()
                val json = importExportManager.exportToJson(allNotes)
                _events.send(SettingsEvent.ShareExport(json))
            } catch (e: Exception) {
                _events.send(SettingsEvent.Message("Export failed: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importData(inputStream: InputStream) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (notes, skipped) = inputStream.use { stream ->
                    importExportManager.importFromStream(stream)
                }
                noteRepository.upsertAll(notes)
                _events.send(SettingsEvent.Message("Imported ${notes.size} notes, skipped $skipped invalid records"))
            } catch (e: Exception) {
                _events.send(SettingsEvent.Message("Import failed: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
