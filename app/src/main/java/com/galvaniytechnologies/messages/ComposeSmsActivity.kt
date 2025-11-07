package com.galvaniytechnologies.messages

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.galvaniytechnologies.messages.ui.compose.ComposeMessageScreen
import com.galvaniytechnologies.messages.ui.theme.MessagesTheme

class ComposeSmsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MessagesTheme {
                ComposeMessageScreen()
            }
        }
    }
}