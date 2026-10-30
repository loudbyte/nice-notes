package com.nicenotes.ui.common

sealed class UiEvent {
    data class ShowSnackbar(
        val message: String,
        val action: String? = null,
        val actionType: SnackbarActionType? = null,
        val noteId: String? = null,
    ) : UiEvent()

    enum class SnackbarActionType {
        UNARCHIVE_NOTE,
        RESTORE_NOTE,
        ARCHIVE_NOTE,
        RESTORE_ARCHIVED_NOTE,
    }
}
