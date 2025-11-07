package com.galvaniytechnologies.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import androidx.core.app.RemoteInput
import com.galvaniytechnologies.messages.data.local.AppDatabase
import com.galvaniytechnologies.messages.data.model.Message
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SmsReplyReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsReplyReceiverEntryPoint {
        fun appDatabase(): AppDatabase
    }

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            val replyText = remoteInput.getCharSequence("key_text_reply")?.toString()
            val conversationId = intent.getIntExtra("conversation_id", -1)
            val senderAddress = intent.getStringExtra("sender_address")

            if (replyText != null && conversationId != -1 && senderAddress != null) {
                val hiltEntryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    SmsReplyReceiverEntryPoint::class.java
                )
                val appDatabase = hiltEntryPoint.appDatabase()
                val messageDao = appDatabase.messageDao()

                CoroutineScope(Dispatchers.IO).launch {
                    val message = Message(
                        conversationId = conversationId.toLong(),
                        body = replyText,
                        sender = "Me", // Assuming "Me" for the current user
                        timestamp = System.currentTimeMillis(),
                        isRead = true,
                        isSent = true,
                        isMms = false
                    )
                    messageDao.insertMessage(message)

                    // Send the SMS message
                    val smsManager = context.getSystemService(SmsManager::class.java)
                    smsManager.sendTextMessage(senderAddress, null, replyText, null, null)
                }
            }
        }
    }
}
