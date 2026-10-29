package com.nicenotes.ui.screen.main

import com.nicenotes.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderNoteSortingTest {

    @Test
    fun withRemindersFirstMovesReminderNotesAheadAndPreservesGroupOrder() {
        val notes = listOf(
            note(id = "ordinary-first"),
            note(id = "reminder-first", alarmTime = 1_000L),
            note(id = "ordinary-second"),
            note(id = "reminder-second", alarmTime = 2_000L),
        )

        assertEquals(
            listOf("reminder-first", "reminder-second", "ordinary-first", "ordinary-second"),
            notes.withRemindersFirst().map(Note::id),
        )
    }

    private fun note(id: String, alarmTime: Long? = null) = Note(
        id = id,
        content = id,
        createdAt = 0L,
        updatedAt = 0L,
        timestamp = 0L,
        alarmTime = alarmTime,
    )
}
