package com.nicenotes.ui.common

sealed class UiEvent {
    data class ShowSnackbar(
        val message: String,
        val action: String?,
        val onAction: () -> Unit
    ) : UiEvent()
}
