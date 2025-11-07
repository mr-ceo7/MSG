package com.galvaniytechnologies.MSG.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.galvaniytechnologies.MSG.data.model.DeliveryLog

@Dao
interface DeliveryLogDao {
    @Query("SELECT * FROM delivery_logs ORDER BY timestamp DESC")
    fun getAllLogs(): LiveData<List<DeliveryLog>>

    @Query("SELECT * FROM delivery_logs WHERE status = :status")
    fun getLogsByStatus(status: String): LiveData<List<DeliveryLog>>

    @Query("SELECT * FROM delivery_logs WHERE messageId = :messageId LIMIT 1")
    suspend fun getLogByMessageId(messageId: String): DeliveryLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DeliveryLog): Long

    @Update
    suspend fun updateLog(log: DeliveryLog)

    @Query("UPDATE delivery_logs SET status = :status, errorMessage = :errorMessage WHERE messageId = :messageId")
    suspend fun updateLogStatus(messageId: String, status: String, errorMessage: String?)
}