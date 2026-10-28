package com.nicenotes.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nicenotes.data.db.entity.NoteEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportExportManager @Inject constructor() {

    private val legacyFormat = SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.ENGLISH)
    private val exportDateFormat = SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.ENGLISH)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun exportToJson(notes: List<NoteEntity>): String {
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val exportNotes = notes.map { it.toExportNote() }
        val root = ExportRoot(version = 1, exportedAt = now, notes = exportNotes)
        return json.encodeToString(root)
    }

    fun createShareIntent(context: Context, jsonContent: String): Intent {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date())
        val fileName = "nicenotes_backup_$dateStr.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(jsonContent)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Import from the legacy bare-array JSON format (original app format)
     * or from NiceNotes ExportRoot format.
     * Returns Pair<imported, skipped>
     */
    fun importFromStream(inputStream: InputStream): Pair<List<NoteEntity>, Int> {
        val raw = inputStream.bufferedReader().readText()
        return try {
            // Try NiceNotes format first
            if (raw.trimStart().startsWith("{")) {
                importFromExportRoot(raw)
            } else {
                // Legacy bare array format
                importFromLegacyArray(raw)
            }
        } catch (e: Exception) {
            Pair(emptyList(), 0)
        }
    }

    private fun importFromExportRoot(raw: String): Pair<List<NoteEntity>, Int> {
        val root = json.decodeFromString<ExportRoot>(raw)
        val valid = mutableListOf<NoteEntity>()
        var skipped = 0
        for (note in root.notes) {
            if (note.mLocalId.isBlank()) {
                skipped++
                continue
            }
            valid.add(note.toEntity())
        }
        return Pair(valid, skipped)
    }

    private fun importFromLegacyArray(raw: String): Pair<List<NoteEntity>, Int> {
        val legacyJson = Json { ignoreUnknownKeys = true }
        val legacyNotes = legacyJson.decodeFromString<List<LegacyNote>>(raw)
        val valid = mutableListOf<NoteEntity>()
        var skipped = 0
        for (note in legacyNotes) {
            if (note.mLocalId.isBlank()) {
                skipped++
                continue
            }
            try {
                valid.add(note.toEntity(legacyFormat))
            } catch (e: Exception) {
                skipped++
            }
        }
        return Pair(valid, skipped)
    }

    private fun NoteEntity.toExportNote(): ExportNote {
        return ExportNote(
            mLocalId = mLocalId,
            mGlobalId = mGlobalId,
            mContent = mContent,
            mStatus = mStatus,
            mCreateTime = exportDateFormat.format(Date(mCreateTime)),
            mUpdateTime = exportDateFormat.format(Date(mUpdateTime)),
            mTimestamp = exportDateFormat.format(Date(mTimestamp)),
            mFinishTime = mFinishTime?.let { exportDateFormat.format(Date(it)) },
            mIsDelete = mIsDelete,
            mIsLocal = mIsLocal,
            mAlarmTime = mAlarmTime?.let { exportDateFormat.format(Date(it)) },
            mAlarmTimePre = mAlarmTimePre?.let { exportDateFormat.format(Date(it)) },
            mNextAlarmTime = mNextAlarmTime?.let { exportDateFormat.format(Date(it)) },
            mRepeatRulePre = mRepeatRulePre,
            mForceReminderPre = mForceReminderPre,
            isReminded = isReminded,
            mColorIndex = mColorIndex,
            mParentId = mParentId,
            mSysVersion = mSysVersion,
            mExtra = ExportExtra(forceReminder = mExtraForceReminder, repeatRule = mExtraRepeatRule),
            isPinned = isPinned,
            isImportant = isImportant,
            isArchived = isArchived,
            position = position,
        )
    }

    private fun ExportNote.toEntity(): NoteEntity {
        val fmt = exportDateFormat
        return NoteEntity(
            mLocalId = mLocalId,
            mGlobalId = mGlobalId,
            mContent = mContent,
            mStatus = mStatus,
            mCreateTime = fmt.parse(mCreateTime)?.time ?: System.currentTimeMillis(),
            mUpdateTime = fmt.parse(mUpdateTime)?.time ?: System.currentTimeMillis(),
            mTimestamp = fmt.parse(mTimestamp)?.time ?: System.currentTimeMillis(),
            mFinishTime = mFinishTime?.let { fmt.parse(it)?.time },
            mIsDelete = mIsDelete,
            mIsLocal = mIsLocal,
            mAlarmTime = mAlarmTime?.let { fmt.parse(it)?.time },
            mAlarmTimePre = mAlarmTimePre?.let { fmt.parse(it)?.time },
            mNextAlarmTime = mNextAlarmTime?.let { fmt.parse(it)?.time },
            mRepeatRulePre = mRepeatRulePre,
            mForceReminderPre = mForceReminderPre,
            isReminded = isReminded,
            mColorIndex = mColorIndex,
            mParentId = mParentId,
            mSysVersion = mSysVersion,
            mExtraForceReminder = mExtra?.forceReminder ?: false,
            mExtraRepeatRule = mExtra?.repeatRule ?: "",
            isPinned = isPinned,
            isImportant = isImportant,
            isArchived = isArchived || mIsDelete,
            position = if (position == 0L) System.currentTimeMillis() else position,
        )
    }
}
