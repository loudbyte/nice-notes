package com.nicenotes.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotesWidgetServiceTest {

    @Test
    fun stableWidgetItemIdIsDeterministicAndIndependentOfPosition() {
        val firstNoteId = "note-123"
        val secondNoteId = "note-456"

        val originalOrder = listOf(firstNoteId, secondNoteId).associateWith(::stableWidgetItemId)
        val reordered = listOf(secondNoteId, firstNoteId).associateWith(::stableWidgetItemId)

        assertEquals(originalOrder[firstNoteId], reordered[firstNoteId])
        assertEquals(originalOrder[secondNoteId], reordered[secondNoteId])
        assertNotEquals(originalOrder[firstNoteId], originalOrder[secondNoteId])
    }
}
