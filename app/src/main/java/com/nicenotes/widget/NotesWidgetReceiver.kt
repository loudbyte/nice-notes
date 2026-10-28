package com.nicenotes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.widget.RemoteViews
import com.nicenotes.MainActivity
import com.nicenotes.R

class NotesWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            refreshWidgetViews(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(
                appWidgetId,
                createRemoteViews(context, appWidgetId)
            )
            appWidgetManager.notifyAppWidgetViewDataChanged(
                appWidgetId,
                R.id.widget_notes_list
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        appWidgetManager.updateAppWidget(
            appWidgetId,
            createRemoteViews(context, appWidgetId)
        )
        appWidgetManager.notifyAppWidgetViewDataChanged(
            appWidgetId,
            R.id.widget_notes_list
        )
    }

    companion object {
        fun updateWidget(context: Context) {
            refreshWidgetViews(context)
        }

        private fun refreshWidgetViews(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, NotesWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(
                    appWidgetId,
                    createRemoteViews(context, appWidgetId)
                )
                appWidgetManager.notifyAppWidgetViewDataChanged(
                    appWidgetId,
                    R.id.widget_notes_list
                )
            }
        }

        private fun createRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_notes)
            val serviceIntent = Intent(context, NotesWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
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

            views.setRemoteAdapter(appWidgetId, R.id.widget_notes_list, serviceIntent)
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
