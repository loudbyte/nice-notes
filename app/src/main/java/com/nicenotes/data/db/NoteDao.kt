package com.nicenotes.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nicenotes.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // Active notes ordered: pinned first, then by position desc
    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 0
        ORDER BY isPinned DESC, position DESC
    """)
    fun observeActiveNotes(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 0
        ORDER BY isPinned DESC, position DESC
    """)
    suspend fun getActiveNotes(): List<NoteEntity>

    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 0
          AND mContent LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, position DESC
    """)
    fun searchActiveNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY mUpdateTime DESC")
    fun observeArchivedNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE mLocalId = :id")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT MAX(position) FROM notes WHERE isArchived = 0")
    suspend fun maxPosition(): Long?

    @Query("SELECT * FROM notes WHERE mAlarmTime IS NOT NULL AND isArchived = 0")
    suspend fun getAllWithReminders(): List<NoteEntity>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("UPDATE notes SET position = :position WHERE mLocalId = :id")
    suspend fun updatePosition(id: String, position: Long)

    @Transaction
    suspend fun updatePositions(updates: List<Pair<String, Long>>) {
        updates.forEach { (id, position) ->
            updatePosition(id, position)
        }
    }

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int
}
