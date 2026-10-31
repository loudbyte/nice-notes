package com.nicenotes.notification

import com.nicenotes.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class ReminderSchedulerTest {

    @Test
    fun testParseDailyRrule() {
        val rrule = "FREQ=DAILY;INTERVAL=1;WKST=SU"
        val interval = ReminderScheduler.parseRruleIntervalMs(rrule)
        assertNotNull(interval)
        assertEquals(TimeUnit.DAYS.toMillis(1), interval)
    }

    @Test
    fun testParseWeeklyRrule() {
        val rrule = "FREQ=WEEKLY;INTERVAL=1;WKST=SU"
        val interval = ReminderScheduler.parseRruleIntervalMs(rrule)
        assertNotNull(interval)
        assertEquals(TimeUnit.DAYS.toMillis(7), interval)
    }

    @Test
    fun testParseMonthlyRrule() {
        val rrule = "FREQ=MONTHLY;INTERVAL=1;WKST=SU"
        val interval = ReminderScheduler.parseRruleIntervalMs(rrule)
        assertNotNull(interval)
        assertEquals(TimeUnit.DAYS.toMillis(30), interval)
    }

    @Test
    fun testEmptyOrInvalidRrule() {
        assertNull(ReminderScheduler.parseRruleIntervalMs(""))
        assertNull(ReminderScheduler.parseRruleIntervalMs("UNKNOWN_RULE"))
    }

    @Test
    fun testAlarmTimeMustBeInFuture() {
        assertEquals(true, ReminderScheduler.isAlarmTimeInFuture(101L, now = 100L))
        assertEquals(false, ReminderScheduler.isAlarmTimeInFuture(100L, now = 100L))
        assertEquals(false, ReminderScheduler.isAlarmTimeInFuture(99L, now = 100L))
    }

    @Test
    fun testNextOccurrenceAdvancesReminderAndResetsRemindedState() {
        val note = Note(
            id = "note-id",
            content = "Reminder",
            createdAt = 0L,
            updatedAt = 0L,
            timestamp = 0L,
            alarmTime = 1_000L,
            repeatRule = "FREQ=DAILY;INTERVAL=1",
            isReminded = true
        )

        val next = ReminderScheduler.nextOccurrence(note)

        assertNotNull(next)
        assertEquals(1_000L + TimeUnit.DAYS.toMillis(1), next?.alarmTime)
        assertEquals(false, next?.isReminded)
    }

    @Test
    fun testNextOccurrenceReturnsNullForNonRepeatingReminder() {
        val note = Note(
            id = "note-id",
            content = "Reminder",
            createdAt = 0L,
            updatedAt = 0L,
            timestamp = 0L,
            alarmTime = 1_000L
        )

        assertNull(ReminderScheduler.nextOccurrence(note))
    }

    @Test
    fun testPrepareImportedReminderAdvancesPastDueRepeatingReminderToFuture() {
        val interval = TimeUnit.DAYS.toMillis(1)
        val note = Note(
            id = "note-id",
            content = "Reminder",
            createdAt = 0L,
            updatedAt = 0L,
            timestamp = 0L,
            alarmTime = 1_000L,
            repeatRule = "FREQ=DAILY;INTERVAL=1",
            isReminded = true
        )

        val next = ReminderScheduler.prepareImportedReminder(
            note,
            now = 1_000L + interval * 2 + interval / 2
        )

        assertEquals(1_000L + interval * 3, next?.alarmTime)
        assertEquals(false, next?.isReminded)
        assertEquals(note.id, next?.id)
    }

    @Test
    fun testPrepareImportedReminderReturnsNullForPastOneTimeReminder() {
        val note = Note(
            id = "note-id",
            content = "Reminder",
            createdAt = 0L,
            updatedAt = 0L,
            timestamp = 0L,
            alarmTime = 1_000L
        )

        assertNull(ReminderScheduler.prepareImportedReminder(note, now = 1_001L))
    }

    @Test
    fun testPrepareImportedReminderDoesNotChangeFutureReminder() {
        val note = Note(
            id = "note-id",
            content = "Reminder",
            createdAt = 0L,
            updatedAt = 0L,
            timestamp = 0L,
            alarmTime = 1_001L,
            repeatRule = "FREQ=WEEKLY;INTERVAL=1"
        )

        assertEquals(note, ReminderScheduler.prepareImportedReminder(note, now = 1_000L))
    }

    @Test
    fun testPrepareImportedReminderExcludesArchivedAndRemindersWithoutAlarm() {
        val note = Note(
            id = "note-id",
            content = "Reminder",
            createdAt = 0L,
            updatedAt = 0L,
            timestamp = 0L,
            alarmTime = 1_001L
        )

        assertNull(
            ReminderScheduler.prepareImportedReminder(note.copy(isArchived = true), now = 1_000L)
        )
        assertNull(
            ReminderScheduler.prepareImportedReminder(note.copy(isTrashed = true), now = 1_000L)
        )
        assertNull(
            ReminderScheduler.prepareImportedReminder(note.copy(alarmTime = null), now = 1_000L)
        )
    }
}
