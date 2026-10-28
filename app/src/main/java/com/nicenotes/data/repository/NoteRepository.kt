package com.nicenotes.data.repository

import android.content.Context
import com.nicenotes.data.db.NoteDao
import com.nicenotes.data.db.entity.NoteEntity
import com.nicenotes.widget.NotesWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val dao: NoteDao,
    @ApplicationContext private val context: Context
) {
    fun observeActiveNotes(): Flow<List<NoteEntity>> = dao.observeActiveNotes()

    fun observeArchivedNotes(): Flow<List<NoteEntity>> = dao.observeArchivedNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = dao.searchActiveNotes(query)

    suspend fun save(note: NoteEntity) {
        dao.upsert(note)
        triggerWidgetUpdate()
    }

    suspend fun delete(note: NoteEntity) {
        dao.delete(note)
        triggerWidgetUpdate()
    }

    suspend fun archive(note: NoteEntity) {
        dao.update(
            note.copy(
                isArchived = true,
                isPinned = false,
                mIsDelete = true,
                mFinishTime = System.currentTimeMillis()
            )
        )
        triggerWidgetUpdate()
    }

    suspend fun unarchive(note: NoteEntity) {
        dao.update(
            note.copy(
                isArchived = false,
                mIsDelete = false,
                mFinishTime = null
            )
        )
        triggerWidgetUpdate()
    }

    suspend fun updatePositions(updates: List<Pair<String, Long>>) {
        updates.forEach { (id, pos) ->
            dao.getById(id)?.let { dao.update(it.copy(position = pos)) }
        }
        triggerWidgetUpdate()
    }

    suspend fun nextPosition(): Long = (dao.maxPosition() ?: 0L) + 1000L

    suspend fun getById(id: String): NoteEntity? = dao.getById(id)

    suspend fun getAllWithReminders(): List<NoteEntity> = dao.getAllWithReminders()

    suspend fun markReminded(note: NoteEntity) {
        dao.update(note.copy(isReminded = true))
    }

    suspend fun updateReminderSchedule(note: NoteEntity) {
        dao.update(note)
    }

    suspend fun upsertAll(notes: List<NoteEntity>) {
        dao.upsertAll(notes)
        triggerWidgetUpdate()
    }

    private fun triggerWidgetUpdate() {
        NotesWidgetReceiver.updateWidget(context)
    }
}
