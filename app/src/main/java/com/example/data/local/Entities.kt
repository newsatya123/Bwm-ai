package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_messages")
data class ConversationMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null,
    val actionDetail: String? = null,
    val isActionSuccess: Boolean? = null
)

@Entity(tableName = "notes")
data class NoteItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "action_logs")
data class ActionLogItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolName: String,
    val parameters: String,
    val result: String,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_commands")
data class CustomCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerPhrase: String,
    val alternativePhrases: String = "", // comma-separated alternatives
    val actionType: String, // "whatsapp_send", "launch_app", "youtube_search", "device_action", "routine"
    val actionParameter: String = "", // recipient or query or app name
    val messagePayload: String = "", // message body
    val requiresConfirmation: Boolean = false,
    val isEnabled: Boolean = true,
    val lastExecutionResult: String? = null,
    val lastExecutedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
