package com.nicenotes.data.export

import kotlinx.serialization.Serializable
import com.nicenotes.domain.model.Note
import com.nicenotes.util.NoteDateFormats

/**
 * Data class for deserializing the legacy bare-array JSON format
 * produced by the original notes app.
 */
@Serializable
data class LegacyNote(
    val mLocalId: String = "",
    val mGlobalId: String? = null,
    val mContent: String = "",
    val mStatus: String = "NEW",
    val mCreateTime: String = "",
    val mUpdateTime: String = "",
    val mTimestamp: String = "",
    val mFinishTime: String? = null,
    val mIsDelete: Boolean = false,
    val mIsLocal: Boolean = false,
    val mAlarmTime: String? = null,
    val mAlarmTimePre: String? = null,
    val mNextAlarmTime: String? = null,
    val mRepeatRulePre: String = "",
    val mForceReminderPre: Boolean = false,
    val isReminded: Boolean = false,
    val mColorIndex: Int = -1,
    val mParentId: String? = null,
    val mSysVersion: Int = 0,
    val mExtra: LegacyExtra? = null,
)

@Serializable
data class LegacyExtra(
    val forceReminder: Boolean = false,
    val repeatRule: String = "",
)

fun LegacyNote.toDomainNote(): Note {
    fun parseTime(s: String?): Long? = s?.let {
        NoteDateFormats.parseLegacyTimestamp(it)
    }

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
        isArchived = mIsDelete || finishedAt != null,
        position = parseTime(mUpdateTime) ?: now,
    )
}
