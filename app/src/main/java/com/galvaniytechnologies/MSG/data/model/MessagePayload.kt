package com.galvaniytechnologies.MSG.data.model

data class MessagePayload(
    val recipients: List<String>,
    val message: String,
    val timestamp: Long
)

data class BroadcastRequest(
    val payload: MessagePayload,
    val hmac: String
)

enum class DeliveryMethod {
    INTENT,
    HTTP
}

enum class DeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    SIMULATED
}