package com.nicenotes.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val mLocalId: String,           // UUID from export
    val mGlobalId: String? = null,
    val mContent: String,
    val mStatus: String = "NEW",
    val mCreateTime: Long,                       // stored as epoch millis
    val mUpdateTime: Long,
    val mTimestamp: Long,
    val mFinishTime: Long? = null,
    val mIsDelete: Boolean = false,              // kept for export compat
    val mIsLocal: Boolean = true,
    val mAlarmTime: Long? = null,
    val mAlarmTimePre: Long? = null,
    val mNextAlarmTime: Long? = null,
    val mRepeatRulePre: String = "",
    val mForceReminderPre: Boolean = false,
    val isReminded: Boolean = false,
    val mColorIndex: Int = -1,
    val mParentId: String? = null,
    val mSysVersion: Int = 1,
    val mExtraForceReminder: Boolean = false,    // flattened from mExtra
    val mExtraRepeatRule: String = "",

    // NiceNotes-specific fields
    val isPinned: Boolean = false,
    val isImportant: Boolean = false,
    val isArchived: Boolean = false,
    val position: Long = 0L,
)
