package com.galvaniytechnologies.MSG.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.galvaniytechnologies.MSG.data.dao.DeliveryLogDao
import com.galvaniytechnologies.MSG.data.model.DeliveryLog

@Database(entities = [DeliveryLog::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deliveryLogDao(): DeliveryLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "msg_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}