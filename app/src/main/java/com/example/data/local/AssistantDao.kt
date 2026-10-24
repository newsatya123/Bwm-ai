package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
    // Conversation Messages
    @Query("SELECT * FROM conversation_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ConversationMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationMessage): Long

    @Query("DELETE FROM conversation_messages")
    suspend fun clearAllMessages()

    // Notes
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteItem): Long

    @Delete
    suspend fun deleteNote(note: NoteItem)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun searchNotes(query: String): List<NoteItem>

    // Action Logs
    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentActionLogs(): Flow<List<ActionLogItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLog(log: ActionLogItem): Long

    @Query("DELETE FROM action_logs")
    suspend fun clearActionLogs()

    // Custom Commands
    @Query("SELECT * FROM custom_commands ORDER BY createdAt DESC")
    fun getAllCustomCommands(): Flow<List<CustomCommandEntity>>

    @Query("SELECT * FROM custom_commands WHERE isEnabled = 1")
    suspend fun getEnabledCustomCommands(): List<CustomCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCommand(command: CustomCommandEntity): Long

    @Update
    suspend fun updateCustomCommand(command: CustomCommandEntity)

    @Delete
    suspend fun deleteCustomCommand(command: CustomCommandEntity)

    @Query("DELETE FROM custom_commands WHERE id = :id")
    suspend fun deleteCustomCommandById(id: Long)

    @Query("SELECT * FROM custom_commands WHERE triggerPhrase = :phrase OR alternativePhrases LIKE '%' || :phrase || '%' LIMIT 1")
    suspend fun findCustomCommandByTrigger(phrase: String): CustomCommandEntity?
}
