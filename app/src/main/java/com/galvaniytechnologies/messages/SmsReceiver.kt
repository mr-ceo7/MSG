package com.galvaniytechnologies.messages

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.galvaniytechnologies.messages.data.local.AppDatabase
import com.galvaniytechnologies.messages.data.model.Message
import com.galvaniytechnologies.messages.data.model.Conversation
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsReceiverEntryPoint {
        fun appDatabase(): AppDatabase
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                SmsReceiverEntryPoint::class.java
            )
            val appDatabase = hiltEntryPoint.appDatabase()
            val messageDao = appDatabase.messageDao()
            val conversationDao = appDatabase.conversationDao()

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (smsMessage in messages) {
                val sender = smsMessage.originatingAddress ?: "Unknown"
                val body = smsMessage.messageBody ?: ""
                val timestamp = smsMessage.timestampMillis

                CoroutineScope(Dispatchers.IO).launch {
                    val conversations = conversationDao.getAllConversations().firstOrNull() ?: emptyList()
                    var conversation = conversations.firstOrNull { conv ->
                        conv.recipients.contains(sender)
                    }

                    if (conversation == null) {
                        conversation = Conversation(
                            recipients = listOf(sender),
                            lastMessage = body,
                            timestamp = timestamp,
                            unreadCount = 1,
                            isArchived = false,
                            isPinned = false,
                            isBlocked = false
                        )
                        val conversationId = conversationDao.insertConversation(conversation)
                        conversation = conversation.copy(id = conversationId)
                    } else {
                        // Update existing conversation
                        conversationDao.insertConversation(conversation.copy(
                            lastMessage = body,
                            timestamp = timestamp,
                            unreadCount = conversation.unreadCount + 1
                        ))
                    }

                    val message = Message(
                        conversationId = conversation.id,
                        body = body,
                        sender = sender,
                        timestamp = timestamp,
                        isRead = false,
                        isSent = false,
                        isMms = false
                    )
                    messageDao.insertMessage(message)

                    showSmsNotification(context, sender, body, conversation.id.toInt())
                }
            }
        } else if (intent.action == Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION) {
            // Handle MMS here
            Log.d("SmsReceiver", "MMS received - not yet fully implemented")
        }
    }

    private fun showSmsNotification(context: Context, sender: String, messageBody: String, conversationId: Int) {
        val channelId = "sms_channel"
        val notificationId = conversationId // Use conversation ID for notification ID

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SMS Messages"
            val descriptionText = "Notifications for incoming SMS messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Reply action
        val remoteInput = RemoteInput.Builder("key_text_reply").run {
            setLabel("Reply")
            build()
        }

        val replyIntent = Intent(context, SmsReplyReceiver::class.java).apply {
            putExtra("conversation_id", conversationId)
            putExtra("sender_address", sender)
        }
        val replyPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context, 
            conversationId, 
            replyIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.sym_action_chat,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(sender)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .addAction(replyAction)
            .setStyle(NotificationCompat.MessagingStyle("Me")
                .addMessage(messageBody, System.currentTimeMillis(), sender))

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, notificationBuilder.build())
        }
    }
}