package com.nicenotes.data.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportRoot(
    val version: Int = 1,
    val exportedAt: String,        // ISO-8601
    val notes: List<ExportNote>
)

@Serializable
data class ExportNote(
    val mLocalId: String,
    val mGlobalId: String? = null,
    val mContent: String,
    val mStatus: String = "NEW",
    val mCreateTime: String,       // "MMM d, yyyy h:mm:ss a"
    val mUpdateTime: String,
    val mTimestamp: String,
    val mFinishTime: String? = null,
    val mIsDelete: Boolean,
    val mIsLocal: Boolean = true,
    val mAlarmTime: String? = null,
    val mAlarmTimePre: String? = null,
    val mNextAlarmTime: String? = null,
    val mRepeatRulePre: String = "",
    val mForceReminderPre: Boolean = false,
    val isReminded: Boolean = false,
    val mColorIndex: Int = -1,
    val mParentId: String? = null,
    val mSysVersion: Int = 1,
    val mExtra: ExportExtra? = null,
    // NiceNotes extensions
    val isPinned: Boolean = false,
    val isImportant: Boolean = false,
    val isArchived: Boolean = false,
    val position: Long = 0L,
)

@Serializable
data class ExportExtra(
    val forceReminder: Boolean = false,
    val repeatRule: String = "",
)
