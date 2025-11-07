package com.galvaniytechnologies.MSG.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.galvaniytechnologies.MSG.data.db.AppDatabase
import com.galvaniytechnologies.MSG.data.model.DeliveryLog
import com.galvaniytechnologies.MSG.data.repository.DeliveryLogRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DeliveryLogRepository
    val allLogs: LiveData<List<DeliveryLog>>

    init {
        val dao = AppDatabase.getInstance(application).deliveryLogDao()
        repository = DeliveryLogRepository(dao)
        allLogs = repository.getAllLogs()
    }

    fun getLogsByStatus(status: String): LiveData<List<DeliveryLog>> =
        repository.getLogsByStatus(status)
}