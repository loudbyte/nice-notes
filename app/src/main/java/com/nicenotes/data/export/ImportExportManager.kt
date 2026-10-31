package com.nicenotes.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nicenotes.domain.model.Note
import com.nicenotes.util.NoteDateFormats
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

sealed class ImportResult {
    abstract val notes: List<Note>
    abstract val skipped: Int

    operator fun component1(): List<Note> = notes
    operator fun component2(): Int = skipped

    class Success(
        override val notes: List<Note>,
        override val skipped: Int,
    ) : ImportResult()

    class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : ImportResult() {
        override val notes: List<Note> = emptyList()
        override val skipped: Int = 0
    }
}

@Singleton
class ImportExportManager @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun exportToJson(notes: List<Note>): String {
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val exportNotes = notes.map { it.toExportNote() }
        val root = ExportRoot(version = 1, exportedAt = now, notes = exportNotes)
        return json.encodeToString(root)
    }

    fun createShareIntent(context: Context, jsonContent: String): Intent {
        val dateStr = NoteDateFormats.formatBackupFileDate(System.currentTimeMillis())
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
    fun importFromStream(inputStream: InputStream): ImportResult {
        return try {
            val raw = inputStream.bufferedReader().readText()
            val notes = if (raw.trimStart().startsWith("{")) {
                importFromExportRoot(raw)
            } else {
                importFromLegacyArray(raw)
            }
            ImportResult.Success(notes.first, notes.second)
        } catch (e: Exception) {
            ImportResult.Error(
                message = "Unable to parse the import file: ${e.message ?: "unknown error"}",
                cause = e
            )
        }
    }

    private fun importFromExportRoot(raw: String): Pair<List<Note>, Int> {
        val root = json.decodeFromString<ExportRoot>(raw)
        val valid = mutableListOf<Note>()
        var skipped = 0
        for (note in root.notes) {
            if (note.mLocalId.isBlank()) {
                skipped++
                continue
            }
            valid.add(note.toDomainNote())
        }
        return Pair(valid, skipped)
    }

    private fun importFromLegacyArray(raw: String): Pair<List<Note>, Int> {
        val legacyNotes = json.decodeFromString<List<LegacyNote>>(raw)
        val valid = mutableListOf<Note>()
        var skipped = 0
        for (note in legacyNotes) {
            if (note.mLocalId.isBlank()) {
                skipped++
                continue
            }
            try {
                valid.add(note.toDomainNote())
            } catch (e: Exception) {
                skipped++
            }
        }
        return Pair(valid, skipped)
    }

    private fun Note.toExportNote(): ExportNote {
        return ExportNote(
            mLocalId = id,
            mGlobalId = globalId,
            mContent = content,
            mStatus = status,
            mCreateTime = NoteDateFormats.formatLegacyTimestamp(createdAt),
            mUpdateTime = NoteDateFormats.formatLegacyTimestamp(updatedAt),
            mTimestamp = NoteDateFormats.formatLegacyTimestamp(timestamp),
            mFinishTime = finishedAt?.let(NoteDateFormats::formatLegacyTimestamp),
            mIsDelete = isDeleted,
            mIsLocal = isLocal,
            mAlarmTime = alarmTime?.let(NoteDateFormats::formatLegacyTimestamp),
            mAlarmTimePre = previousAlarmTime?.let(NoteDateFormats::formatLegacyTimestamp),
            mNextAlarmTime = nextAlarmTime?.let(NoteDateFormats::formatLegacyTimestamp),
            mRepeatRulePre = repeatRule,
            mForceReminderPre = forceReminder,
            isReminded = isReminded,
            mColorIndex = colorIndex,
            mParentId = parentId,
            mSysVersion = systemVersion,
            mExtra = ExportExtra(forceReminder = extraForceReminder, repeatRule = extraRepeatRule),
            isPinned = isPinned,
            isImportant = isImportant,
            isArchived = isArchived,
            isTrashed = isTrashed,
            position = position,
        )
    }

    private fun ExportNote.toDomainNote(): Note {
        fun parseTime(value: String?): Long? =
            value?.let(NoteDateFormats::parseLegacyTimestamp)
        val now = System.currentTimeMillis()
        val finishedAt = parseTime(mFinishTime)
        return Note(
            id = mLocalId,
            globalId = mGlobalId,
            content = mContent,
            status = mStatus,
            createdAt = parseTime(mCreateTime) ?: now,
            updatedAt = parseTime(mUpdateTime) ?: now,
            timestamp = parseTime(mTimestamp) ?: now,
            finishedAt = finishedAt,
            isDeleted = mIsDelete,
            isLocal = mIsLocal,
            alarmTime = parseTime(mAlarmTime),
            previousAlarmTime = parseTime(mAlarmTimePre),
            nextAlarmTime = parseTime(mNextAlarmTime),
            repeatRule = mRepeatRulePre,
            forceReminder = mForceReminderPre,
            isReminded = isReminded,
            colorIndex = mColorIndex,
            parentId = mParentId,
            systemVersion = mSysVersion,
            extraForceReminder = mExtra?.forceReminder ?: false,
            extraRepeatRule = mExtra?.repeatRule ?: "",
            isPinned = isPinned,
            isImportant = isImportant,
            isArchived = isArchived || mIsDelete || finishedAt != null,
            isTrashed = isTrashed,
            position = if (position == 0L) now else position,
        )
    }
}
