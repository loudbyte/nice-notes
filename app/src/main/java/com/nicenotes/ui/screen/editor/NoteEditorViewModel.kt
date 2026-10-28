package com.nicenotes.ui.screen.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicenotes.data.db.entity.NoteEntity
import com.nicenotes.data.repository.NoteRepository
import com.nicenotes.domain.usecase.SaveNoteUseCase
import com.nicenotes.notification.ReminderScheduler
import com.nicenotes.notification.ReminderSchedulingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val saveNoteUseCase: SaveNoteUseCase,
    private val noteRepository: NoteRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()

    private val _isPinned = MutableStateFlow(false)
    val isPinned = _isPinned.asStateFlow()

    private val _isImportant = MutableStateFlow(false)
    val isImportant = _isImportant.asStateFlow()

    private val _alarmTime = MutableStateFlow<Long?>(null)
    val alarmTime = _alarmTime.asStateFlow()

    private val _repeatRule = MutableStateFlow("")
    val repeatRule = _repeatRule.asStateFlow()

    private var editingNote: NoteEntity? = null

    fun loadNote(note: NoteEntity?) {
        editingNote = note
        _content.value = note?.mContent ?: ""
        _isPinned.value = note?.isPinned ?: false
        _isImportant.value = note?.isImportant ?: false
        _alarmTime.value = note?.mAlarmTime
        _repeatRule.value = note?.mRepeatRulePre ?: ""
    }

    fun setContent(text: String) { _content.value = text }
    fun togglePin() { _isPinned.value = !_isPinned.value }
    fun toggleImportant() { _isImportant.value = !_isImportant.value }
    fun setAlarm(time: Long?, rrule: String) {
        _alarmTime.value = time
        _repeatRule.value = rrule
    }

    fun save(onDone: (NoteEntity, ReminderSchedulingMode) -> Unit) {
        val content = _content.value
        if (content.isBlank()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val note = editingNote?.copy(
                mContent = content,
                mUpdateTime = now,
                isPinned = _isPinned.value,
                isImportant = _isImportant.value,
                mAlarmTime = _alarmTime.value,
                mRepeatRulePre = _repeatRule.value,
            ) ?: NoteEntity(
                mLocalId = UUID.randomUUID().toString(),
                mContent = content,
                mCreateTime = now,
                mUpdateTime = now,
                mTimestamp = now,
                isPinned = _isPinned.value,
                isImportant = _isImportant.value,
                mAlarmTime = _alarmTime.value,
                mRepeatRulePre = _repeatRule.value,
                position = noteRepository.nextPosition(),
            )
            saveNoteUseCase(note)

            // Schedule or cancel reminder
            val schedulingMode = ReminderScheduler.schedule(context, note)
            onDone(note, schedulingMode)
        }
    }
}
