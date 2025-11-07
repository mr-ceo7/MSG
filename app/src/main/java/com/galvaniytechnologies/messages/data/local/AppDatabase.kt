package com.galvaniytechnologies.messages.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.galvaniytechnologies.messages.data.model.Conversation
import com.galvaniytechnologies.messages.data.model.Message
import com.galvaniytechnologies.messages.data.model.MmsPart
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Database(entities = [Message::class, Conversation::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
}

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromMmsPartList(value: List<MmsPart>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toMmsPartList(value: String): List<MmsPart> {
        val listType = object : TypeToken<List<MmsPart>>() {}.type
        return Gson().fromJson(value, listType)
    }
}