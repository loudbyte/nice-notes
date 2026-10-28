package com.nicenotes.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nicenotes.data.repository.NoteRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: NoteRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_NOTE_ID = "noteId"
    }

    override suspend fun doWork(): Result {
        val noteId = inputData.getString(KEY_NOTE_ID) ?: return Result.failure()
        return when (ReminderDelivery.deliver(applicationContext, repo, noteId)) {
            NoteNotificationManager.DeliveryStatus.SHOWN -> Result.success()
            else -> Result.failure()
        }
    }
}
