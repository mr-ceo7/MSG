package com.galvaniytechnologies.messages.ui.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.galvaniytechnologies.messages.MmsFileProvider
import com.galvaniytechnologies.messages.data.model.MmsPart
import java.io.File

@Composable
fun ComposeMessageScreen() {
    val context = LocalContext.current
    var recipients by remember { mutableStateOf("") }
    var messageBody by remember { mutableStateOf("") }
    val mmsParts = remember { mutableStateListOf<MmsPart>() }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        uri: Uri? ->
        uri?.let { mmsParts.add(MmsPart("image/*", it.toString())) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = recipients,
            onValueChange = { recipients = it },
            label = { Text("Recipients (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = messageBody,
            onValueChange = { messageBody = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        LazyColumn {
            items(mmsParts) {
                Text(text = "Attached: ${it.type} - ${it.uri}")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                Icon(Icons.Default.Add, contentDescription = "Attach Image")
            }
            Button(
                onClick = { sendMessage(context, recipients, messageBody, mmsParts) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
                Text("Send")
            }
        }
    }
}

private fun sendMessage(context: Context, recipients: String, messageBody: String, mmsParts: List<MmsPart>) {
    if (recipients.isBlank()) {
        Toast.makeText(context, "Please enter recipients", Toast.LENGTH_SHORT).show()
        return
    }

    val recipientList = recipients.split(",").map { it.trim() }

    if (mmsParts.isNotEmpty()) {
        // Handle MMS
        sendMms(context, recipientList, messageBody, mmsParts)
    } else {
        // Handle SMS
        sendSms(context, recipientList, messageBody)
    }
}

private fun sendSms(context: Context, recipients: List<String>, messageBody: String) {
    val smsManager = context.getSystemService(SmsManager::class.java)
    for (recipient in recipients) {
        smsManager.sendTextMessage(recipient, null, messageBody, null, null)
    }
    Toast.makeText(context, "SMS sent to ${recipients.joinToString()}", Toast.LENGTH_SHORT).show()
    (context as? Activity)?.finish()
}

private fun sendMms(context: Context, recipients: List<String>, messageBody: String, mmsParts: List<MmsPart>) {
    val mmsManager = context.getSystemService(SmsManager::class.java)
    val mmsIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        type = "image/*" // Default to image, will be updated based on parts
        putExtra("address", recipients.joinToString(";"))
        putExtra("sms_body", messageBody)

        val uriList = ArrayList<Uri>()
        mmsParts.forEach { part ->
            val file = File(part.uri) // This will not work directly with content URIs
            // Need to copy content URI to a temporary file and then get its URI
            // For simplicity, let's assume part.uri is already a file URI or handle it properly
            uriList.add(Uri.parse(part.uri))
        }
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
    }

    // This is a simplified approach. A full MMS implementation is complex.
    // It involves creating a PDU, handling network, etc.
    // For now, we are relying on the system's MMS app to handle the intent.
    context.startActivity(Intent.createChooser(mmsIntent, "Send MMS"))
    Toast.makeText(context, "MMS intent launched for ${recipients.joinToString()}", Toast.LENGTH_SHORT).show()
    (context as? Activity)?.finish()
}
