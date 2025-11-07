package com.galvaniytechnologies.MSG.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.galvaniytechnologies.MSG.R
import com.galvaniytechnologies.MSG.data.db.AppDatabase
import com.galvaniytechnologies.MSG.data.model.DeliveryLog
import com.galvaniytechnologies.MSG.data.model.DeliveryMethod
import com.galvaniytechnologies.MSG.data.model.DeliveryStatus
import com.galvaniytechnologies.MSG.data.model.MessagePayload
import com.galvaniytechnologies.MSG.receiver.MessageBroadcastReceiver.Companion.EXTRA_BROADCAST_TYPE
import com.galvaniytechnologies.MSG.receiver.MessageBroadcastReceiver.Companion.EXTRA_HMAC
import com.galvaniytechnologies.MSG.receiver.MessageBroadcastReceiver.Companion.EXTRA_PAYLOAD
import com.galvaniytechnologies.MSG.worker.SendSmsWorker
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MessageSenderService : Service() {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: AppDatabase
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "msg_service_channel"
        private const val NOTIFICATION_ID = 1
        private const val BATCH_SIZE = 100 // Maximum recipients per batch
        
        fun enqueueWork(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_NOT_STICKY
    }

    private fun handleIntent(intent: Intent) {
        val payload = intent.getStringExtra(EXTRA_PAYLOAD) ?: run {
            android.util.Log.e("MessageSenderService", "Missing EXTRA_PAYLOAD")
            return
        }
        val hmac = intent.getStringExtra(EXTRA_HMAC) ?: run {
            android.util.Log.e("MessageSenderService", "Missing EXTRA_HMAC")
            return
        }
        val broadcastType = intent.getStringExtra(EXTRA_BROADCAST_TYPE) ?: "intent"
        android.util.Log.i("MessageSenderService", "Processing broadcast type=$broadcastType")

        scope.launch {
            var messageId: String = ""
            try {
                val messagePayload = gson.fromJson(payload, MessagePayload::class.java)
                messageId = UUID.randomUUID().toString()

                // Create delivery log
                val deliveryLog = DeliveryLog(
                    messageId = messageId,
                    recipients = messagePayload.recipients.joinToString(","),
                    message = messagePayload.message,
                    timestamp = messagePayload.timestamp,
                    deliveryMethod = broadcastType,
                    status = DeliveryStatus.PENDING.name,
                    errorMessage = null
                )
                database.deliveryLogDao().insertLog(deliveryLog)

                // If simulation is enabled, don't call SmsManager — just mark as simulated
                val simulate = com.galvaniytechnologies.MSG.util.DebugConfig.isSimulationEnabled(this@MessageSenderService)

                if (simulate) {
                    database.deliveryLogDao().updateLogStatus(
                        messageId,
                        DeliveryStatus.SIMULATED.name,
                        "Simulated - no SMS sent"
                    )
                } else {
                    // Split recipients into batches and send
                    messagePayload.recipients
                        .chunked(BATCH_SIZE)
                        .forEach { batch ->
                            sendSmsMessages(batch, messagePayload.message, messageId)
                        }
                }
            } catch (e: Exception) {
                // Log error and schedule retry via WorkManager
                val retryData = Data.Builder()
                    .putString("messageId", messageId)
                    .putString("payload", payload)
                    .putString("hmac", hmac)
                    .putString("broadcastType", broadcastType)
                    .build()

                val retryRequest = OneTimeWorkRequestBuilder<SendSmsWorker>()
                    .setInputData(retryData)
                    .build()

                WorkManager.getInstance(applicationContext)
                    .enqueue(retryRequest)
            }
        }
    }

    private fun sendSmsMessages(recipients: List<String>, message: String, messageId: String) {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        recipients.forEach { recipient ->
            try {
                if (message.length > 160) {
                    smsManager.sendMultipartTextMessage(
                        recipient,
                        null,
                        smsManager.divideMessage(message),
                        null,
                        null
                    )
                } else {
                    smsManager.sendTextMessage(
                        recipient,
                        null,
                        message,
                        null,
                        null
                    )
                }
            } catch (e: Exception) {
                scope.launch {
                    database.deliveryLogDao().updateLogStatus(
                        messageId,
                        DeliveryStatus.FAILED.name,
                        e.message
                    )
                }
            }
        }

        // Update status to sent
        scope.launch {
            database.deliveryLogDao().updateLogStatus(
                messageId,
                DeliveryStatus.SENT.name,
                null
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MSG Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for MSG Service notifications"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MSG Service")
            .setContentText("Processing messages...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?) = null
}