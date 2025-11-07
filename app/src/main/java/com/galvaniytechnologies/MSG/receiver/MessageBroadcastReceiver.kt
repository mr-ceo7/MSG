package com.galvaniytechnologies.MSG.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.galvaniytechnologies.MSG.data.model.DeliveryMethod
import com.galvaniytechnologies.MSG.service.MessageSenderService
import com.galvaniytechnologies.MSG.util.HmacUtils
import com.google.gson.Gson

class MessageBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MessageBroadcastReceiver"
        const val ACTION_RECEIVE_CUSTOM = "com.yourapp.sms.RECEIVE_CUSTOM"
        const val EXTRA_PAYLOAD = "extra_payload"
        const val EXTRA_HMAC = "extra_hmac"
        const val EXTRA_BROADCAST_TYPE = "broadcast_type"
    }

    private val gson = Gson()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RECEIVE_CUSTOM) {
            Log.d(TAG, "Ignoring action: ${intent.action}")
            return
        }

        val payload = intent.getStringExtra(EXTRA_PAYLOAD) ?: run {
            Log.e(TAG, "Missing required EXTRA_PAYLOAD")
            return
        }
        val hmac = intent.getStringExtra(EXTRA_HMAC) ?: run {
            Log.e(TAG, "Missing required EXTRA_HMAC") 
            return
        }
        val broadcastType = intent.getStringExtra(EXTRA_BROADCAST_TYPE) ?: "intent"
        
        Log.i(TAG, "Received broadcast type=$broadcastType payload=${payload.take(50)}...")

        if (!HmacUtils.verifyHmac(payload, hmac)) {
            Log.e(TAG, "HMAC verification failed")
            return
        }

        // Start the MessageSenderService
        val serviceIntent = Intent(context, MessageSenderService::class.java).apply {
            putExtra(EXTRA_PAYLOAD, payload)
            putExtra(EXTRA_HMAC, hmac)
            putExtra(EXTRA_BROADCAST_TYPE, broadcastType)
        }
        
        MessageSenderService.enqueueWork(context, serviceIntent)
    }
}