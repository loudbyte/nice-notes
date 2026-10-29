package com.nicenotes.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

class NoteDateFormatsTest {

    @Test
    fun legacyTimestampFormatRoundTripsUsingSystemZone() {
        val previousTimeZone = TimeZone.getDefault()
        val previousLocale = Locale.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            Locale.setDefault(Locale.US)
            val epochMillis = LocalDateTime.of(2024, 1, 2, 15, 4, 5)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()

            val formatted = NoteDateFormats.formatLegacyTimestamp(epochMillis)

            assertEquals("Jan 2, 2024 3:04:05 PM", formatted)
            assertEquals(epochMillis, NoteDateFormats.parseLegacyTimestamp(formatted))
        } finally {
            TimeZone.setDefault(previousTimeZone)
            Locale.setDefault(previousLocale)
        }
    }
}
