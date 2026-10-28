package com.nicenotes.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nicenotes.data.db.entity.NoteEntity
import java.util.concurrent.TimeUnit

enum class ReminderSchedulingMode {
    EXACT,
    BEST_EFFORT,
    NONE,
}

object ReminderScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean =
        alarmManager(context).canScheduleExactAlarms()

    fun schedule(context: Context, note: NoteEntity): ReminderSchedulingMode {
        val alarmTime = note.mAlarmTime
        if (alarmTime == null || !isAlarmTimeInFuture(alarmTime)) {
            cancel(context, note.mLocalId)
            return ReminderSchedulingMode.NONE
        }

        val alarmManager = alarmManager(context)
        if (alarmManager.canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    reminderPendingIntent(context, note.mLocalId)
                )
                WorkManager.getInstance(context).cancelUniqueWork(note.mLocalId)
                return ReminderSchedulingMode.EXACT
            } catch (exception: SecurityException) {
                Log.w(TAG, "Exact alarm access was revoked; using best-effort work.", exception)
            }
        }

        enqueueBestEffortWork(context, note.mLocalId, alarmTime)
        return ReminderSchedulingMode.BEST_EFFORT
    }

    fun cancel(context: Context, noteId: String) {
        val pendingIntent = reminderPendingIntent(context, noteId)
        alarmManager(context).cancel(pendingIntent)
        pendingIntent.cancel()
        WorkManager.getInstance(context).cancelUniqueWork(noteId)
    }

    fun nextOccurrence(note: NoteEntity): NoteEntity? {
        val intervalMs = parseRruleIntervalMs(note.mRepeatRulePre) ?: return null
        val alarmTime = note.mAlarmTime ?: return null
        return note.copy(
            mAlarmTime = alarmTime + intervalMs,
            isReminded = false
        )
    }

    internal fun parseRruleIntervalMs(rrule: String): Long? {
        return when {
            rrule.contains("FREQ=DAILY") -> TimeUnit.DAYS.toMillis(1)
            rrule.contains("FREQ=WEEKLY") -> TimeUnit.DAYS.toMillis(7)
            rrule.contains("FREQ=MONTHLY") -> TimeUnit.DAYS.toMillis(30)
            else -> null
        }
    }

    internal fun isAlarmTimeInFuture(
        alarmTime: Long,
        now: Long = System.currentTimeMillis()
    ): Boolean = alarmTime > now

    private fun enqueueBestEffortWork(context: Context, noteId: String, alarmTime: Long) {
        val delay = alarmTime - System.currentTimeMillis()
        if (delay <= 0) return

        val data = Data.Builder()
            .putString(ReminderWorker.KEY_NOTE_ID, noteId)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(noteId, ExistingWorkPolicy.REPLACE, request)
    }

    private fun reminderPendingIntent(context: Context, noteId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DELIVER_REMINDER
            data = Uri.parse("nicenotes://reminder/${Uri.encode(noteId)}")
            putExtra(ReminderWorker.KEY_NOTE_ID, noteId)
        }
        return PendingIntent.getBroadcast(
            context,
            noteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private const val ACTION_DELIVER_REMINDER = "com.nicenotes.action.DELIVER_REMINDER"
    private const val TAG = "ReminderScheduler"
}
