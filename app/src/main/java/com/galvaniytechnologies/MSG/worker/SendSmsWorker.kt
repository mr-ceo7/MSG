package com.galvaniytechnologies.MSG.worker

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.galvaniytechnologies.MSG.service.MessageSenderService
import java.util.concurrent.TimeUnit

class SendSmsWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MILLIS = 10000L // 10 seconds
    }

    override suspend fun doWork(): Result {
        val messageId = inputData.getString("messageId") ?: return Result.failure()
        val payload = inputData.getString("payload") ?: return Result.failure()
        val hmac = inputData.getString("hmac") ?: return Result.failure()
        val broadcastType = inputData.getString("broadcastType") ?: "intent"

        return try {
            val intent = Intent(applicationContext, MessageSenderService::class.java).apply {
                putExtra("extra_payload", payload)
                putExtra("extra_hmac", hmac)
                putExtra("broadcast_type", broadcastType)
            }

            MessageSenderService.enqueueWork(applicationContext, intent)
            Result.success()
        } catch (e: Exception) {
            val runAttemptCount = runAttemptCount
            if (runAttemptCount >= MAX_RETRIES) {
                Result.failure()
            } else {
                // Exponential backoff
                val backoffDelay = INITIAL_BACKOFF_MILLIS * (1 shl runAttemptCount)
                val backoffConstraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val retryWork = OneTimeWorkRequestBuilder<SendSmsWorker>()
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        backoffDelay,
                        TimeUnit.MILLISECONDS
                    )
                    .setConstraints(backoffConstraints)
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(applicationContext)
                    .enqueue(retryWork)

                Result.retry()
            }
        }
    }
}