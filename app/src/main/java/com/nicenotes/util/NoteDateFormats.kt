package com.nicenotes.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object NoteDateFormats {
    private val legacyTimestampFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy h:mm:ss a", Locale.ENGLISH)
    private val noteDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    private val noteDateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH)
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val backupFileFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH)

    fun formatLegacyTimestamp(epochMillis: Long): String =
        format(epochMillis, legacyTimestampFormatter)

    fun parseLegacyTimestamp(value: String): Long? = try {
        LocalDateTime.parse(value, legacyTimestampFormatter)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }

    fun formatNoteDate(epochMillis: Long): String = format(epochMillis, noteDateFormatter)

    fun formatNoteDateTime(epochMillis: Long): String = format(epochMillis, noteDateTimeFormatter)

    fun formatDate(epochMillis: Long): String = format(epochMillis, dateFormatter)

    fun formatTime(epochMillis: Long): String = format(epochMillis, timeFormatter)

    fun formatBackupFileDate(epochMillis: Long): String = format(epochMillis, backupFileFormatter)

    private fun format(epochMillis: Long, formatter: DateTimeFormatter): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
}
