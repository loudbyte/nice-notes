package com.nicenotes.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nicenotes.data.repository.NoteRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var noteRepository: NoteRepository

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(ReminderWorker.KEY_NOTE_ID)
        if (noteId == null) {
            Log.e(TAG, "Reminder alarm received without a note ID.")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val status = ReminderDelivery.deliver(context, noteRepository, noteId)
                if (status != NoteNotificationManager.DeliveryStatus.SHOWN) {
                    Log.w(TAG, "Reminder notification was not shown: $status")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ReminderReceiver"
    }
}
