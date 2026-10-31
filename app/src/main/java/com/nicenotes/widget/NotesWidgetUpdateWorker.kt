package com.nicenotes.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotesWidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        NotesWidgetReceiver.notifyWidgetDataChanged(applicationContext)
        return Result.success()
    }

}
