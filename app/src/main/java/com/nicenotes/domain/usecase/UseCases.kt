package com.nicenotes.domain.usecase

import com.nicenotes.domain.model.Note
import com.nicenotes.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveNotesUseCase @Inject constructor(private val repo: NoteRepository) {
    operator fun invoke(): Flow<List<Note>> = repo.observeActiveNotes()
}

class GetArchivedNotesUseCase @Inject constructor(private val repo: NoteRepository) {
    operator fun invoke(): Flow<List<Note>> = repo.observeArchivedNotes()
}

class SearchNotesUseCase @Inject constructor(private val repo: NoteRepository) {
    operator fun invoke(query: String): Flow<List<Note>> = repo.searchNotes(query)
}

class SaveNoteUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(note: Note) {
        if (note.content.isBlank()) return
        repo.save(note)
    }
}

class DeleteNoteUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(note: Note) = repo.delete(note)
}

class ArchiveNoteUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(note: Note) = repo.archive(note)
}

class UnarchiveNoteUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(note: Note) = repo.unarchive(note)
}

class ReorderNotesUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(updates: List<Pair<String, Long>>) =
        repo.updatePositions(updates)
}

class GetNoteByIdUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(id: String): Note? = repo.getById(id)
}
