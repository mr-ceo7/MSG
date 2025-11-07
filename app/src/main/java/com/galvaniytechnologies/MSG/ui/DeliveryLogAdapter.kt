package com.galvaniytechnologies.MSG.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.galvaniytechnologies.MSG.R
import com.galvaniytechnologies.MSG.data.model.DeliveryLog
import java.text.SimpleDateFormat
import java.util.*

class DeliveryLogAdapter : ListAdapter<DeliveryLog, DeliveryLogAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DeliveryLog>() {
            override fun areItemsTheSame(oldItem: DeliveryLog, newItem: DeliveryLog): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DeliveryLog, newItem: DeliveryLog): Boolean {
                return oldItem == newItem
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageIdText: TextView = view.findViewById(R.id.messageIdText)
        private val recipientsText: TextView = view.findViewById(R.id.recipientsText)
        private val messageText: TextView = view.findViewById(R.id.messageText)
        private val timestampText: TextView = view.findViewById(R.id.timestampText)
        private val statusText: TextView = view.findViewById(R.id.statusText)
        private val errorText: TextView = view.findViewById(R.id.errorText)

        fun bind(log: DeliveryLog) {
            messageIdText.text = "Message ID: ${log.messageId}"
            recipientsText.text = "Recipients: ${log.recipients}"
            messageText.text = "Message: ${log.message}"
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            timestampText.text = "Time: ${dateFormat.format(Date(log.timestamp))}"
            
            statusText.text = "Status: ${log.status}"
            errorText.apply {
                visibility = if (log.errorMessage != null) View.VISIBLE else View.GONE
                text = "Error: ${log.errorMessage}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}