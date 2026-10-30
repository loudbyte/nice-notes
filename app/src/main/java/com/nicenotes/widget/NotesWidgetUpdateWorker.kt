package com.nicenotes.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nicenotes.R

class NotesWidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val component = ComponentName(applicationContext, NotesWidgetReceiver::class.java)
        val widgetIds = manager.getAppWidgetIds(component)
        if (widgetIds.isNotEmpty()) {
            manager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_notes_list)
        }
        return Result.success()
    }

}
