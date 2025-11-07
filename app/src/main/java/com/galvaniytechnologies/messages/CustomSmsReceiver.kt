package com.galvaniytechnologies.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.galvaniytechnologies.messages.data.local.AppDatabase
import com.galvaniytechnologies.messages.data.model.Conversation
import com.galvaniytechnologies.messages.data.model.Message
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class CustomSmsReceiver : BroadcastReceiver() {

    private val SECRET_KEY = "your-experimental-secret".toByteArray()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CustomSmsReceiverEntryPoint {
        fun appDatabase(): AppDatabase
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.yourapp.sms.RECEIVE_CUSTOM") {
            val messageBody = intent.getStringExtra("message")
            val signature = intent.getStringExtra("signature")
            val sender = intent.getStringExtra("sender") ?: "Custom Sender"
            val timestamp = System.currentTimeMillis()

            if (messageBody == null || signature == null) {
                Log.e("CustomSmsReceiver", "Received custom message with missing body or signature")
                return
            }

            if (!verifyHmacSha256(messageBody, signature, SECRET_KEY)) {
                Log.e("CustomSmsReceiver", "HMAC-SHA256 signature verification failed for custom message")
                return
            }

            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                CustomSmsReceiverEntryPoint::class.java
            )
            val appDatabase = hiltEntryPoint.appDatabase()
            val messageDao = appDatabase.messageDao()
            val conversationDao = appDatabase.conversationDao()

            CoroutineScope(Dispatchers.IO).launch {
                val conversations = conversationDao.getAllConversations().firstOrNull() ?: emptyList()
                var conversation = conversations.firstOrNull { conv ->
                    conv.recipients.contains(sender)
                }

                if (conversation == null) {
                    conversation = Conversation(
                        recipients = listOf(sender),
                        lastMessage = messageBody,
                        timestamp = timestamp,
                        unreadCount = 1,
                        isArchived = false,
                        isPinned = false,
                        isBlocked = false
                    )
                    val conversationId = conversationDao.insertConversation(conversation)
                    conversation = conversation.copy(id = conversationId)
                } else {
                    conversationDao.insertConversation(conversation.copy(
                        lastMessage = messageBody,
                        timestamp = timestamp,
                        unreadCount = conversation.unreadCount + 1
                    ))
                }

                val message = Message(
                    conversationId = conversation.id,
                    body = messageBody,
                    sender = sender,
                    timestamp = timestamp,
                    isRead = false,
                    isSent = false,
                    isMms = false
                )
                messageDao.insertMessage(message)
                Log.d("CustomSmsReceiver", "Custom message inserted into DB: $messageBody")
            }
        }
    }

    private fun verifyHmacSha256(data: String, signature: String, key: ByteArray): Boolean {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(key, "HmacSHA256")
            mac.init(secretKeySpec)
            val hmacBytes = mac.doFinal(data.toByteArray())
            val expectedSignature = hmacBytes.joinToString("") { "%02x".format(it) }
            expectedSignature == signature
        } catch (e: Exception) {
            Log.e("CustomSmsReceiver", "Error verifying HMAC-SHA256 signature", e)
            false
        }
    }
}