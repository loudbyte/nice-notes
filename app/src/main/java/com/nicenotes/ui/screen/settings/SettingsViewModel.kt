package com.nicenotes.ui.screen.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicenotes.data.export.ImportExportManager
import com.nicenotes.data.export.ImportResult
import com.nicenotes.data.repository.NoteRepository
import com.nicenotes.domain.model.Note
import com.nicenotes.notification.ReminderScheduler
import com.nicenotes.notification.ReminderSchedulingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

sealed class SettingsEvent {
    data class Message(val text: String) : SettingsEvent()
    data class ShareExport(val intent: Intent) : SettingsEvent()
    object RequestNotificationPermission : SettingsEvent()
    object RequestExactAlarmAccess : SettingsEvent()
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

    private val pendingExactAlarmNoteIds = linkedSetOf<String>()

    fun exportData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val shareIntent = withContext(Dispatchers.IO) {
                    val allNotes = noteRepository.getAllNotes()
                    val json = importExportManager.exportToJson(allNotes)
                    importExportManager.createShareIntent(context, json)
                }
                _events.send(
                    SettingsEvent.ShareExport(shareIntent)
                )
            } catch (e: Exception) {
                _events.send(SettingsEvent.Message("Export failed: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IOException("Unable to open the selected import file.")
                    stream.use {
                        importExportManager.importFromStream(it)
                    }
                }

                when (result) {
                    is ImportResult.Success -> {
                        noteRepository.upsertAll(result.notes)
                        val importedNotesById = result.notes.associateBy(Note::id).values
                        val reminderSummary = scheduleImportedReminders(importedNotesById)

                        val scheduled = reminderSummary.exact + reminderSummary.bestEffort
                        if (scheduled > 0) {
                            requestReminderPermissions(reminderSummary.bestEffort > 0)
                        }

                        _events.send(
                            SettingsEvent.Message(
                                buildString {
                                    append("Imported ${result.notes.size} notes, skipped ${result.skipped} invalid records")
                                    if (scheduled > 0) {
                                        append("; scheduled $scheduled reminders")
                                        if (reminderSummary.bestEffort > 0) {
                                            append(" (${reminderSummary.bestEffort} best-effort)")
                                        }
                                    }
                                    if (reminderSummary.pastOneTime > 0) {
                                        append("; left ${reminderSummary.pastOneTime} past one-time reminders unscheduled")
                                    }
                                    if (reminderSummary.failed > 0) {
                                        append("; failed to schedule ${reminderSummary.failed} reminders")
                                    }
                                }
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

    fun onNotificationPermissionResult() {
        viewModelScope.launch {
            requestExactAlarmPermissionIfNeeded()
        }
    }

    fun onExactAlarmPermissionResult(hasAccess: Boolean) {
        val noteIds = pendingExactAlarmNoteIds.toList()
        pendingExactAlarmNoteIds.clear()
        if (noteIds.isEmpty()) return

        if (!hasAccess) {
            viewModelScope.launch {
                _events.send(
                    SettingsEvent.Message(
                        "Exact-alarm access was not granted; imported reminders will use best-effort delivery."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            var summary = ImportReminderSummary()
            val now = System.currentTimeMillis()

            noteIds.forEach { noteId ->
                try {
                    val note = noteRepository.getById(noteId)
                    if (note != null) {
                        summary += scheduleImportedReminder(note, now)
                    }
                } catch (e: Exception) {
                    summary += ImportReminderSummary(failed = 1)
                    Log.e(TAG, "Failed to reschedule imported reminder for note $noteId", e)
                }
            }

            val message = buildString {
                append("Exact-alarm access enabled; scheduled ${summary.exact} imported reminders.")
                if (summary.pastOneTime > 0) {
                    append(" ${summary.pastOneTime} past one-time reminders remain unscheduled.")
                }
                if (summary.failed > 0) {
                    append(" ${summary.failed} reminders could not be scheduled.")
                }
            }
            _events.send(SettingsEvent.Message(message))
        }
    }

    private suspend fun scheduleImportedReminders(
        notes: Collection<Note>,
    ): ImportReminderSummary {
        var exact = 0
        var bestEffort = 0
        var pastOneTime = 0
        var failed = 0
        val pendingExactIds = mutableSetOf<String>()
        val now = System.currentTimeMillis()

        notes.forEach { importedNote ->
            val result = scheduleImportedReminder(importedNote, now)
            exact += result.exact
            bestEffort += result.bestEffort
            pastOneTime += result.pastOneTime
            failed += result.failed
            if (result.bestEffort > 0) {
                pendingExactIds += importedNote.id
            }
        }
        pendingExactAlarmNoteIds += pendingExactIds
        return ImportReminderSummary(exact, bestEffort, pastOneTime, failed)
    }

    private suspend fun scheduleImportedReminder(
        note: Note,
        now: Long,
    ): ImportReminderSummary {
        try {
            val noteToSchedule = ReminderScheduler.prepareImportedReminder(note, now)
            if (noteToSchedule == null) {
                ReminderScheduler.cancel(context, note.id)
                val isPastDue = !note.isArchived && !note.isTrashed &&
                    note.alarmTime != null && note.alarmTime <= now
                return when {
                    !isPastDue -> ImportReminderSummary()
                    note.repeatRule.isBlank() -> ImportReminderSummary(pastOneTime = 1)
                    else -> {
                        Log.w(TAG, "Cannot advance past-due reminder for note ${note.id}")
                        ImportReminderSummary(failed = 1)
                    }
                }
            }

            if (noteToSchedule.alarmTime != note.alarmTime) {
                noteRepository.updateReminderSchedule(noteToSchedule)
            }

            return when (ReminderScheduler.schedule(context, noteToSchedule)) {
                ReminderSchedulingMode.EXACT -> ImportReminderSummary(exact = 1)
                ReminderSchedulingMode.BEST_EFFORT -> ImportReminderSummary(bestEffort = 1)
                ReminderSchedulingMode.NONE -> ImportReminderSummary(failed = 1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule imported reminder for note ${note.id}", e)
            return ImportReminderSummary(failed = 1)
        }
    }

    private suspend fun requestReminderPermissions(needsExactAlarmAccess: Boolean) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _events.send(SettingsEvent.RequestNotificationPermission)
        } else if (needsExactAlarmAccess) {
            requestExactAlarmPermissionIfNeeded()
        }
    }

    private suspend fun requestExactAlarmPermissionIfNeeded() {
        if (pendingExactAlarmNoteIds.isNotEmpty()) {
            _events.send(SettingsEvent.RequestExactAlarmAccess)
        }
    }

    private data class ImportReminderSummary(
        val exact: Int = 0,
        val bestEffort: Int = 0,
        val pastOneTime: Int = 0,
        val failed: Int = 0,
    ) {
        operator fun plus(other: ImportReminderSummary) = ImportReminderSummary(
            exact = exact + other.exact,
            bestEffort = bestEffort + other.bestEffort,
            pastOneTime = pastOneTime + other.pastOneTime,
            failed = failed + other.failed,
        )
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
