package com.nicenotes.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone

class ReminderDialogDateTest {

    @Test
    fun selectedUtcDateUpdatesLocalCalendarDateAndPreservesTime() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles")).apply {
            set(2024, Calendar.JUNE, 15, 15, 47, 28)
            set(Calendar.MILLISECOND, 0)
        }
        val selectedDateMillis = LocalDate.of(2024, 1, 2)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        setCalendarDateFromUtcMillis(calendar, selectedDateMillis)

        assertEquals(2024, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH))
        assertEquals(2, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(47, calendar.get(Calendar.MINUTE))
        assertEquals(28, calendar.get(Calendar.SECOND))
    }
}
