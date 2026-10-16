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
