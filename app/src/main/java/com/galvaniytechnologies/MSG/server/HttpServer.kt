package com.galvaniytechnologies.MSG.server

// Use fully-qualified Android types to avoid ambiguous imports with server/Ktor types
import android.util.Log
import com.galvaniytechnologies.MSG.receiver.MessageBroadcastReceiver
import com.galvaniytechnologies.MSG.util.HmacUtils
import com.google.gson.Gson
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.ContentType
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class HttpServer(private val context: android.content.Context) {
    private val gson = Gson()
    private var server: ApplicationEngine? = null
    private val TAG = "HttpServer"

        private fun isPortAvailable(port: Int): Boolean {
            return try {
                java.net.ServerSocket(port).use { socket ->
                    socket.reuseAddress = true
                    socket.localPort == port
                }
            } catch (e: Exception) {
                Log.w(TAG, "Port $port is already in use", e)
                false
            }
        }

        fun start() {
            // First check if port is available
            if (!isPortAvailable(8080)) {
                Log.w(TAG, "Port 8080 is already in use, skipping embedded HTTP server start")
                return
            }

            try {
                server = embeddedServer(CIO, port = 8080, host = "127.0.0.1") {
                    install(ContentNegotiation)

                    routing {
                        post("/broadcast") {
                            val request = call.receiveText()
                            val broadcastRequest = gson.fromJson(request, com.galvaniytechnologies.MSG.data.model.BroadcastRequest::class.java)

                            if (!HmacUtils.verifyHmac(
                                    gson.toJson(broadcastRequest.payload),
                                    broadcastRequest.hmac
                                )) {
                                val resp = mapOf("status" to "error", "message" to "Invalid HMAC")
                                call.respondText(gson.toJson(resp), ContentType.Application.Json)
                                return@post
                            }

                            val messageId = UUID.randomUUID().toString()

                            // create and send broadcast intent (no need to switch to main thread for sendBroadcast)
                            val intent = android.content.Intent(this@HttpServer.context, MessageBroadcastReceiver::class.java).apply {
                                action = MessageBroadcastReceiver.ACTION_RECEIVE_CUSTOM
                                putExtra(MessageBroadcastReceiver.EXTRA_PAYLOAD,
                                    gson.toJson(broadcastRequest.payload))
                                putExtra(MessageBroadcastReceiver.EXTRA_HMAC,
                                    broadcastRequest.hmac)
                                putExtra(MessageBroadcastReceiver.EXTRA_BROADCAST_TYPE, "http")
                            }
                            this@HttpServer.context.sendBroadcast(intent)

                            val okResp = mapOf("status" to "ok", "messageId" to messageId)
                            call.respondText(gson.toJson(okResp), ContentType.Application.Json)
                        }
                    }
                }

                server?.start(wait = false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start embedded HTTP server", e)
                server = null
            }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
}