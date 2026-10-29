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
}
