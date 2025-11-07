package com.galvaniytechnologies.messages.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val body: String,
    val sender: String,
    val timestamp: Long,
    val isRead: Boolean,
    val isSent: Boolean,
    val isMms: Boolean,
    val mmsSubject: String? = null,
    val mmsParts: List<MmsPart> = emptyList()
)

data class MmsPart(
    val type: String, // e.g., "image/jpeg", "text/plain"
    val uri: String, // a content URI for the part
    val text: String? = null
)
