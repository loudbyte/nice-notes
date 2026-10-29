package com.nicenotes.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nicenotes.data.db.entity.NoteEntity

@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = true
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "nicenotes.db"
    }
}
