package com.nicenotes.data.export

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import com.nicenotes.data.db.entity.NoteEntity

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

fun LegacyNote.toEntity(fmt: SimpleDateFormat): NoteEntity {
    fun parseTime(s: String?): Long? = s?.let {
        try { fmt.parse(it)?.time } catch (e: Exception) { null }
    }

    val now = System.currentTimeMillis()
    return NoteEntity(
        mLocalId = mLocalId,
        mGlobalId = mGlobalId,
        mContent = mContent,
        mStatus = mStatus,
        mCreateTime = parseTime(mCreateTime) ?: now,
        mUpdateTime = parseTime(mUpdateTime) ?: now,
        mTimestamp = parseTime(mTimestamp) ?: now,
        mFinishTime = parseTime(mFinishTime),
        mIsDelete = mIsDelete,
        mIsLocal = mIsLocal,
        mAlarmTime = parseTime(mAlarmTime),
        mAlarmTimePre = parseTime(mAlarmTimePre),
        mNextAlarmTime = parseTime(mNextAlarmTime),
        mRepeatRulePre = mRepeatRulePre,
        mForceReminderPre = mForceReminderPre,
        isReminded = isReminded,
        mColorIndex = mColorIndex,
        mParentId = mParentId,
        mSysVersion = mSysVersion,
        mExtraForceReminder = mExtra?.forceReminder ?: false,
        mExtraRepeatRule = mExtra?.repeatRule ?: "",
        // Map mIsDelete → isArchived per plan
        isArchived = mIsDelete,
        position = parseTime(mUpdateTime) ?: now,
    )
}
