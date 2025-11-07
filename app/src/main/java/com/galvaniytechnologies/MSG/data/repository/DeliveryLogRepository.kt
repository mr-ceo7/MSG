package com.galvaniytechnologies.MSG.data.repository

import androidx.lifecycle.LiveData
import com.galvaniytechnologies.MSG.data.dao.DeliveryLogDao
import com.galvaniytechnologies.MSG.data.model.DeliveryLog

class DeliveryLogRepository(private val deliveryLogDao: DeliveryLogDao) {
    fun getAllLogs(): LiveData<List<DeliveryLog>> = deliveryLogDao.getAllLogs()
    
    fun getLogsByStatus(status: String): LiveData<List<DeliveryLog>> = 
        deliveryLogDao.getLogsByStatus(status)

    suspend fun insertLog(log: DeliveryLog): Long = deliveryLogDao.insertLog(log)

    suspend fun updateLog(log: DeliveryLog) = deliveryLogDao.updateLog(log)

    suspend fun updateLogStatus(messageId: String, status: String, errorMessage: String? = null) =
        deliveryLogDao.updateLogStatus(messageId, status, errorMessage)

    suspend fun getLogByMessageId(messageId: String): DeliveryLog? =
        deliveryLogDao.getLogByMessageId(messageId)
}