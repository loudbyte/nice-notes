package com.nicenotes.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class ImportExportTest {

    private val manager = ImportExportManager()

    @Test
    fun testParseExampleOfDataDataset() {
        // Find example-of-data in root or parent
        val fileCandidates = listOf(
            File("example-of-data"),
            File("../example-of-data"),
            File("../../example-of-data")
        )
        val dataFile = fileCandidates.firstOrNull { it.exists() }
        assertNotNull("example-of-data file should exist", dataFile)

        val (notes, skipped) = FileInputStream(dataFile!!).use {
            manager.importFromStream(it)
        }

        // Expected dataset metrics from discovery
        assertEquals(2164, notes.size)
        assertEquals(0, skipped)

        val archivedNotes = notes.filter { it.isArchived }
        assertEquals(809, archivedNotes.size)

        val activeNotes = notes.filter { !it.isArchived }
        assertEquals(1355, activeNotes.size)

        // Verify notes with reminders
        val notesWithReminders = notes.filter { it.mAlarmTime != null }
        assertTrue("Should have notes with reminders", notesWithReminders.isNotEmpty())

        // Verify repeating reminders
        val repeatingNotes = notes.filter { it.mRepeatRulePre.isNotEmpty() }
        assertTrue("Should have notes with repeat rules", repeatingNotes.isNotEmpty())
    }

    @Test
    fun testExportAndReImportRoundTrip() {
        val sampleFile = listOf(
            File("example-of-data"),
            File("../example-of-data"),
            File("../../example-of-data")
        ).firstOrNull { it.exists() }
        assertNotNull(sampleFile)

        val (originalNotes, _) = FileInputStream(sampleFile!!).use {
            manager.importFromStream(it)
        }

        val subset = originalNotes.take(50)
        val exportedJson = manager.exportToJson(subset)
        assertTrue(exportedJson.startsWith("{\"version\":1"))

        val (reimportedNotes, reimportSkipped) = manager.importFromStream(
            exportedJson.byteInputStream()
        )

        assertEquals(50, reimportedNotes.size)
        assertEquals(0, reimportSkipped)
        assertEquals(subset[0].mLocalId, reimportedNotes[0].mLocalId)
        assertEquals(subset[0].mContent, reimportedNotes[0].mContent)
        assertEquals(subset[0].isArchived, reimportedNotes[0].isArchived)
    }
}
