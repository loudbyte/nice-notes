package com.nicenotes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nicenotes.MainActivity
import com.nicenotes.R
import com.nicenotes.data.db.entity.NoteEntity

class NotesWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            enqueueWidgetUpdate(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        enqueueWidgetUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        enqueueWidgetUpdate(context)
    }

    companion object {
        private const val UPDATE_WORK_NAME = "notes_widget_update"

        fun updateWidget(context: Context) {
            enqueueWidgetUpdate(context)
        }

        internal fun enqueueWidgetUpdate(context: Context) {
            val request = OneTimeWorkRequestBuilder<NotesWidgetUpdateWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UPDATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        internal fun createRemoteViews(
            context: Context,
            appWidgetId: Int,
            notes: List<NoteEntity>,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_notes)
            val collectionPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + APP_OPEN_REQUEST_CODE_OFFSET,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val createNotePendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + CREATE_NOTE_REQUEST_CODE_OFFSET,
                Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_CREATE_NOTE
                    putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_CREATE)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val items = RemoteViews.RemoteCollectionItems.Builder()
                .setHasStableIds(true)
                .setViewTypeCount(1)
                .apply {
                    notes.forEach { note ->
                        val item = RemoteViews(context.packageName, R.layout.widget_note_item).apply {
                            setTextViewText(
                                R.id.widget_note_content,
                                note.mContent.replace('\n', ' ').replace('\r', ' ')
                            )
                            setOnClickFillInIntent(
                                R.id.widget_note_content,
                                Intent().putExtra(MainActivity.EXTRA_NOTE_ID, note.mLocalId)
                            )
                        }
                        addItem(stableWidgetItemId(note.mLocalId), item)
                    }
                }
                .build()

            views.setRemoteAdapter(R.id.widget_notes_list, items)
            views.setEmptyView(R.id.widget_notes_list, R.id.widget_empty_view)
            views.setPendingIntentTemplate(
                R.id.widget_notes_list,
                collectionPendingIntent
            )
            views.setOnClickPendingIntent(R.id.widget_header, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_add_note, createNotePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_view, openAppPendingIntent)
            return views
        }

        private const val APP_OPEN_REQUEST_CODE_OFFSET = 100_000
        private const val CREATE_NOTE_REQUEST_CODE_OFFSET = 200_000
    }
}

internal fun stableWidgetItemId(noteId: String): Long {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(noteId.toByteArray(Charsets.UTF_8))
    return java.nio.ByteBuffer.wrap(digest, 0, Long.SIZE_BYTES).long
}
