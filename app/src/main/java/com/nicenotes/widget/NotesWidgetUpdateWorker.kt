package com.nicenotes.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.nicenotes.data.db.NoteDao
import kotlinx.coroutines.CancellationException

class NotesWidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val noteDao = EntryPointAccessors.fromApplication(
                applicationContext,
                NotesWidgetEntryPoint::class.java,
            ).noteDao()
            val notes = noteDao.getActiveNotes()
            val manager = AppWidgetManager.getInstance(applicationContext)
            val component = ComponentName(applicationContext, NotesWidgetReceiver::class.java)
            val widgetIds = manager.getAppWidgetIds(component)

            widgetIds.forEach { widgetId ->
                manager.updateAppWidget(
                    widgetId,
                    NotesWidgetReceiver.createRemoteViews(applicationContext, widgetId, notes),
                )
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notes widgets", e)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "NotesWidgetUpdate"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotesWidgetEntryPoint {
    fun noteDao(): NoteDao
}
