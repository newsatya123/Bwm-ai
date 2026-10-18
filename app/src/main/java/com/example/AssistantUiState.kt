package com.example

enum class AssistantUiState {
    IDLE,
    LISTENING_FOR_WAKE_WORD,
    CAPTURING_COMMAND,
    LISTENING,
    PROCESSING,
    EXECUTING,
    SPEAKING,
    ERROR;

    val isListening: Boolean
        get() = this == LISTENING_FOR_WAKE_WORD || this == CAPTURING_COMMAND || this == LISTENING
}

data class ConfirmationDialogData(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)
