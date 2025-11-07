package com.galvaniytechnologies.MSG.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "delivery_logs")
data class DeliveryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: String,
    val recipients: String, // CSV of recipient numbers
    val message: String,
    val timestamp: Long,
    val deliveryMethod: String, // "intent" or "http"
    val status: String, // "pending", "sent", "failed"
    val errorMessage: String?
)