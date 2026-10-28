package com.nicenotes.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nicenotes.MainActivity
import com.nicenotes.R
import com.nicenotes.data.db.entity.NoteEntity

object NoteNotificationManager {

    const val CHANNEL_ID = "nicenotes_reminders"
    const val CHANNEL_NAME = "Reminders"
    const val EXTRA_NOTE_ID = MainActivity.EXTRA_NOTE_ID

    enum class DeliveryStatus {
        SHOWN,
        NOTE_NOT_FOUND,
        APP_NOTIFICATIONS_DISABLED,
        CHANNEL_DISABLED,
    }

    data class NotificationSettings(
        val notificationsEnabled: Boolean,
        val soundEnabled: Boolean,
    )

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Note reminders from NiceNotes"
            enableVibration(true)
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
        }
        notificationManager(context).createNotificationChannel(channel)
    }

    fun notificationSettings(context: Context): NotificationSettings {
        val notificationManager = notificationManager(context)
        val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
        val appEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val channelEnabled = channel != null &&
            channel.importance != NotificationManager.IMPORTANCE_NONE
        return NotificationSettings(
            notificationsEnabled = appEnabled && channelEnabled,
            soundEnabled = appEnabled &&
                channel != null &&
                channel.importance >= NotificationManager.IMPORTANCE_DEFAULT &&
                channel.sound != null
        )
    }

    fun channelSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
        }

    fun appNotificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

    fun showReminder(context: Context, note: NoteEntity): DeliveryStatus {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return DeliveryStatus.APP_NOTIFICATIONS_DISABLED
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (channel == null || channel.importance == NotificationManager.IMPORTANCE_NONE) {
            return DeliveryStatus.CHANNEL_DISABLED
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTE_ID, note.mLocalId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            note.mLocalId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_note)
            .setContentTitle(note.mContent)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        return try {
            notificationManager.notify(note.mLocalId, 0, notification)
            DeliveryStatus.SHOWN
        } catch (_: SecurityException) {
            DeliveryStatus.APP_NOTIFICATIONS_DISABLED
        }
    }

    private fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
