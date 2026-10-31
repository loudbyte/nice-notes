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
        assertEquals(2, archivedNotes.size)
        assertTrue("Finished legacy notes should be archived", archivedNotes.any { it.id == "id-8" })

        val activeNotes = notes.filter { !it.isArchived }
        assertEquals(12, activeNotes.size)

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

    @Test
    fun testVersionedImportArchivesFinishedNotes() {
        val json = """
            {
              "version": 1,
              "exportedAt": "2024-01-01T00:00:00Z",
              "notes": [{
                "mLocalId": "finished-note",
                "mContent": "Finished note",
                "mCreateTime": "Jan 1, 2024 1:00:00 PM",
                "mUpdateTime": "Jan 1, 2024 1:30:00 PM",
                "mTimestamp": "Jan 1, 2024 1:00:00 PM",
                "mFinishTime": "Jan 1, 2024 1:30:00 PM",
                "mIsDelete": false,
                "isArchived": false
              }]
            }
        """.trimIndent()

        val result = manager.importFromStream(json.byteInputStream())

        assertTrue(result is ImportResult.Success)
        assertEquals(1, result.notes.size)
        assertTrue(result.notes.single().isArchived)
        assertNotNull(result.notes.single().finishedAt)
    }
}
