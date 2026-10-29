package com.nicenotes.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileInputStream

class ImportExportTest {

    private val manager = ImportExportManager()

    @Test
    fun testParseExampleOfDataDataset() {
        val stream = javaClass.getResourceAsStream("/test-data.json")
        assertNotNull("test-data file should exist", stream)

        val (notes, skipped) = stream!!.use {
            manager.importFromStream(it)
        }

        assertEquals(14, notes.size)
        assertEquals(1, skipped)

        val archivedNotes = notes.filter { it.isArchived }
        assertEquals(1, archivedNotes.size)

        val activeNotes = notes.filter { !it.isArchived }
        assertEquals(13, activeNotes.size)

        val notesWithReminders = notes.filter { it.alarmTime != null }
        assertTrue("Should have notes with reminders", notesWithReminders.isNotEmpty())

        val repeatingNotes = notes.filter { it.repeatRule.isNotEmpty() }
        assertTrue("Should have notes with repeat rules", repeatingNotes.isNotEmpty())
    }

    @Test
    fun testExportAndReImportRoundTrip() {
        val stream = javaClass.getResourceAsStream("/test-data.json")
        assertNotNull(stream)

        val (originalNotes, _) = stream!!.use {
            manager.importFromStream(it)
        }

        val subset = originalNotes.take(5)
        val exportedJson = manager.exportToJson(subset)
        assertTrue(exportedJson.startsWith("{\"version\":1"))

        val (reimportedNotes, reimportSkipped) = manager.importFromStream(
            exportedJson.byteInputStream()
        )

        assertEquals(5, reimportedNotes.size)
        assertEquals(0, reimportSkipped)
        assertEquals(subset[0].id, reimportedNotes[0].id)
        assertEquals(subset[0].content, reimportedNotes[0].content)
        assertEquals(subset[0].isArchived, reimportedNotes[0].isArchived)
    }
}
