package com.nicenotes.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.nicenotes.MainActivity
import com.nicenotes.R
import com.nicenotes.data.db.NoteDao
import com.nicenotes.data.db.entity.NoteEntity
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.security.MessageDigest

class NotesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        NotesRemoteViewsFactory(applicationContext)
}

private class NotesRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private val noteDao = EntryPointAccessors.fromApplication(
        context,
        NotesWidgetEntryPoint::class.java
    ).noteDao()
    private var notes: List<NoteEntity> = emptyList()
    private var itemIds: List<Long> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        notes = runBlocking(Dispatchers.IO) {
            noteDao.getActiveNotes()
        }
        itemIds = notes.map { stableWidgetItemId(it.mLocalId) }
    }

    override fun onDestroy() {
        notes = emptyList()
        itemIds = emptyList()
    }

    override fun getCount(): Int = notes.size

    override fun getViewAt(position: Int): RemoteViews? {
        val note = notes.getOrNull(position) ?: return null
        return RemoteViews(context.packageName, R.layout.widget_note_item).apply {
            setTextViewText(
                R.id.widget_note_content,
                note.mContent.replace('\n', ' ').replace('\r', ' ')
            )
            setOnClickFillInIntent(
                R.id.widget_note_content,
                Intent().putExtra(MainActivity.EXTRA_NOTE_ID, note.mLocalId)
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        itemIds.getOrNull(position) ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}

internal fun stableWidgetItemId(noteId: String): Long {
    val digest = MessageDigest.getInstance("SHA-256").digest(noteId.toByteArray(Charsets.UTF_8))
    return ByteBuffer.wrap(digest, 0, Long.SIZE_BYTES).long
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotesWidgetEntryPoint {
    fun noteDao(): NoteDao
}
