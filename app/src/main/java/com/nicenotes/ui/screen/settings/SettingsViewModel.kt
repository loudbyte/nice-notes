package com.nicenotes.ui.screen.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicenotes.data.export.ImportExportManager
import com.nicenotes.data.export.ImportResult
import com.nicenotes.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

sealed class SettingsEvent {
    data class Message(val text: String) : SettingsEvent()
    data class ShareExport(val intent: Intent) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val importExportManager: ImportExportManager,
    @param:ApplicationContext private val context: Context,
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
                _events.send(
                    SettingsEvent.ShareExport(importExportManager.createShareIntent(context, json))
                )
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
                val result = inputStream.use { stream ->
                    importExportManager.importFromStream(stream)
                }

                when (result) {
                    is ImportResult.Success -> {
                        noteRepository.upsertAll(result.notes)
                        _events.send(
                            SettingsEvent.Message(
                                "Imported ${result.notes.size} notes, skipped ${result.skipped} invalid records"
                            )
                        )
                    }
                    is ImportResult.Error -> {
                        _events.send(SettingsEvent.Message(result.message))
                    }
                }
            } catch (e: Exception) {
                _events.send(SettingsEvent.Message("Import failed: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
