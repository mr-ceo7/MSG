package com.galvaniytechnologies.messages.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.galvaniytechnologies.messages.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<Message>>

    @Insert
    suspend fun insertMessage(message: Message)
}
