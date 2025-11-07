package com.galvaniytechnologies.messages.data.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.galvaniytechnologies.messages.data.local.AppDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

import com.galvaniytechnologies.messages.data.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SmsContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsContentProviderEntryPoint {
        fun appDatabase(): AppDatabase
    }

    private lateinit var appDatabase: AppDatabase
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext ?: throw IllegalStateException("Application context is null")
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            SmsContentProviderEntryPoint::class.java
        )
        appDatabase = hiltEntryPoint.appDatabase()
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        // Not implementing query for now
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return when (uri.pathSegments.firstOrNull()) {
            "inbox" -> {
                val messageBody = values?.getAsString("body") ?: return null
                val sender = values.getAsString("sender") ?: return null
                val timestamp = values.getAsLong("timestamp") ?: System.currentTimeMillis()
                val conversationId = values.getAsLong("conversationId") ?: 0L // TODO: Handle conversation ID properly

                val message = Message(
                    conversationId = conversationId,
                    body = messageBody,
                    sender = sender,
                    timestamp = timestamp,
                    isRead = false,
                    isSent = false,
                    isMms = false
                )
                val messageId = runBlocking { appDatabase.messageDao().insertMessage(message) }
                context?.contentResolver?.notifyChange(uri, null)
                Uri.withAppendedPath(uri, messageId.toString())
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        // Not implementing delete for now
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        // Not implementing update for now
        return 0
    }
}
