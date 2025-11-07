package com.galvaniytechnologies.messages.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipients: List<String>,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isBlocked: Boolean
)
