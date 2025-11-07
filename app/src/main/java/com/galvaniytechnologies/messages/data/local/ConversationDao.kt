package com.galvaniytechnologies.messages.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.galvaniytechnologies.messages.data.model.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Insert
    suspend fun insertConversation(conversation: Conversation): Long
}
