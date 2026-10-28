package com.nicenotes.notification

import android.content.Context
import com.nicenotes.data.repository.NoteRepository

object ReminderDelivery {

    suspend fun deliver(
        context: Context,
        repository: NoteRepository,
        noteId: String
    ): NoteNotificationManager.DeliveryStatus {
        val note = repository.getById(noteId)
            ?: return NoteNotificationManager.DeliveryStatus.NOTE_NOT_FOUND

        val status = NoteNotificationManager.showReminder(context, note)
        if (status == NoteNotificationManager.DeliveryStatus.SHOWN) {
            repository.markReminded(note)
        }

        ReminderScheduler.nextOccurrence(note)?.let { nextNote ->
            repository.updateReminderSchedule(nextNote)
            ReminderScheduler.schedule(context, nextNote)
        }
        return status
    }
}
