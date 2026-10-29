package com.nicenotes.data.repository

import android.content.Context
import com.nicenotes.data.db.NoteDao
import com.nicenotes.data.db.toDomain
import com.nicenotes.data.db.toEntity
import com.nicenotes.data.db.entity.NoteEntity
import com.nicenotes.domain.model.Note
import com.nicenotes.widget.NotesWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val dao: NoteDao,
    @param:ApplicationContext private val context: Context
) {
    fun observeActiveNotes(): Flow<List<Note>> = dao.observeActiveNotes().map { notes ->
        notes.map(NoteEntity::toDomain)
    }

    fun observeArchivedNotes(): Flow<List<Note>> = dao.observeArchivedNotes().map { notes ->
        notes.map(NoteEntity::toDomain)
    }

    fun searchNotes(query: String): Flow<List<Note>> = dao.searchActiveNotes(query).map { notes ->
        notes.map(NoteEntity::toDomain)
    }

    suspend fun save(note: Note) {
        dao.upsert(note.toEntity())
        triggerWidgetUpdate()
    }

    suspend fun delete(note: Note) {
        dao.delete(note.toEntity())
        triggerWidgetUpdate()
    }

    suspend fun archive(note: Note) {
        dao.update(
            note.copy(
                isArchived = true,
                isPinned = false,
                isDeleted = true,
                finishedAt = System.currentTimeMillis()
            ).toEntity()
        )
        triggerWidgetUpdate()
    }

    suspend fun unarchive(note: Note) {
        dao.update(
            note.copy(
                isArchived = false,
                isDeleted = false,
                finishedAt = null
            ).toEntity()
        )
        triggerWidgetUpdate()
    }

    suspend fun updatePositions(updates: List<Pair<String, Long>>) {
        dao.updatePositions(updates)
        triggerWidgetUpdate()
    }

    suspend fun getAllNotes(): List<Note> = dao.getAllNotes().map(NoteEntity::toDomain)

    suspend fun nextPosition(): Long = (dao.maxPosition() ?: 0L) + 1000L

    suspend fun getById(id: String): Note? = dao.getById(id)?.toDomain()

    suspend fun getAllWithReminders(): List<Note> =
        dao.getAllWithReminders().map(NoteEntity::toDomain)

    suspend fun markReminded(note: Note) {
        dao.update(note.copy(isReminded = true).toEntity())
    }

    suspend fun updateReminderSchedule(note: Note) {
        dao.update(note.toEntity())
    }

    suspend fun upsertAll(notes: List<Note>) {
        dao.upsertAll(notes.map(Note::toEntity))
        triggerWidgetUpdate()
    }

    private fun triggerWidgetUpdate() {
        NotesWidgetReceiver.updateWidget(context)
    }
}
