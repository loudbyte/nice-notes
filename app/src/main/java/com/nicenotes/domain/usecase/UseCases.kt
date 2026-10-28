package com.nicenotes.domain.usecase

import com.nicenotes.data.db.entity.NoteEntity
import com.nicenotes.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveNotesUseCase @Inject constructor(private val repo: NoteRepository) {
    operator fun invoke(): Flow<List<NoteEntity>> = repo.observeActiveNotes()
}

class GetArchivedNotesUseCase @Inject constructor(private val repo: NoteRepository) {
    operator fun invoke(): Flow<List<NoteEntity>> = repo.observeArchivedNotes()
}

class SearchNotesUseCase @Inject constructor(private val repo: NoteRepository) {
    operator fun invoke(query: String): Flow<List<NoteEntity>> = repo.searchNotes(query)
}

class SaveNoteUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(note: NoteEntity) {
        if (note.mContent.isBlank()) return   // never save empty notes
        repo.save(note)
    }
}

class DeleteNoteUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(note: NoteEntity) = repo.delete(note)
}

class ArchiveNoteUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(note: NoteEntity) = repo.archive(note)
}

class UnarchiveNoteUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(note: NoteEntity) = repo.unarchive(note)
}

class ReorderNotesUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke(updates: List<Pair<String, Long>>) =
        repo.updatePositions(updates)
}
