package com.nicenotes.data.db

import com.nicenotes.data.db.entity.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteMappersTest {

    @Test
    fun entityDomainRoundTripPreservesAllStoredFields() {
        val entity = NoteEntity(
            mLocalId = "note-id",
            mGlobalId = "global-id",
            mContent = "A note",
            mStatus = "UPDATED",
            mCreateTime = 1L,
            mUpdateTime = 2L,
            mTimestamp = 3L,
            mFinishTime = 4L,
            mIsDelete = true,
            mIsLocal = false,
            mAlarmTime = 5L,
            mAlarmTimePre = 6L,
            mNextAlarmTime = 7L,
            mRepeatRulePre = "FREQ=DAILY",
            mForceReminderPre = true,
            isReminded = true,
            mColorIndex = 8,
            mParentId = "parent-id",
            mSysVersion = 9,
            mExtraForceReminder = true,
            mExtraRepeatRule = "FREQ=WEEKLY",
            isPinned = true,
            isImportant = true,
            isArchived = true,
            isTrashed = true,
            position = 10L,
        )

        assertEquals(entity, entity.toDomain().toEntity())
    }
}
