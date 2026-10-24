package com.example

import com.example.engine.actions.ContactInfo

enum class AssistantUiState {
    IDLE,
    DISABLED,
    STANDBY_WAKE_WORD,
    COMMAND_CAPTURE,
    LISTENING_FOR_WAKE_WORD,
    CAPTURING_COMMAND,
    LISTENING,
    PROCESSING,
    EXECUTING,
    WAITING_FOR_CONFIRMATION,
    SPEAKING,
    MEDIA_PAUSED_LISTENING,
    ERROR,
    ERROR_RECOVERY;

    val isListening: Boolean
        get() = this == LISTENING_FOR_WAKE_WORD || this == CAPTURING_COMMAND || this == LISTENING || this == COMMAND_CAPTURE || this == STANDBY_WAKE_WORD
}

data class ConfirmationDialogData(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

data class ContactDisambiguationData(
    val title: String,
    val prompt: String,
    val contacts: List<ContactInfo>,
    val onContactChosen: (ContactInfo) -> Unit,
    val onDismiss: () -> Unit
)
