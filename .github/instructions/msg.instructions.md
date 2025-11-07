---
applyTo: '**'
---
``instructions
---
Title: Companion SMS App for Secure Message Delivery
---
Build a companion SMS App that receives payloads from the Broadcaster app
(this repo) and actually performs the SMS delivery. The companion app must
be fully offline, trusted, and run on the same device or local hotspot.

Purpose
- Provide a secure receiver and sender for messages produced by the
  Broadcaster app. Verify HMACs, accept Intent or local HTTP requests,
  enqueue and send SMS locally, expose delivery logs, and support retries.

Exact contract (must match Broadcaster)
- Intent action: `com.yourapp.sms.RECEIVE_CUSTOM`
- Intent extras:
  - `extra_payload` (String): JSON string of the payload
  - `extra_hmac` (String): base64 HMAC-SHA256 of the payload JSON
  - `broadcast_type` (String, optional): `intent` or `http`
- HTTP fallback: POST `http://127.0.0.1:8080/broadcast` with JSON body
  { "payload": { recipients: [...], message: "...", timestamp: 0 }, "hmac": "..." }
- HMAC secret (development): `your-experimental-secret` (base64 output,
  no newlines). Use exact JSON string as received to compute/verify HMAC.

Required components
1) Incoming BroadcastReceiver
   - Validates `extra_hmac` against `extra_payload`.
   - If valid, forwards payload to the foreground `MessageSenderService`
     (or enqueues WorkManager work). If invalid, log and ignore.

2) Embedded HTTP server (Ktor) bound to 127.0.0.1:8080
   - POST `/broadcast`: verify HMAC, enqueue to same pipeline.
   - Respond 200 {"status":"ok","messageId":"<id>"} on success.

3) MessageSenderService (foreground)
   - Accepts payload JSON + hmac + broadcast_type.
   - Calls `SmsManager` (or appropriate carrier APIs) to send SMS.
   - Handles multipart messages and bulk recipients (batching/chunking).
   - On failure, schedule retry via WorkManager and log status.

4) WorkManager worker(s)
   - `SendSmsWorker` for retries with exponential backoff.

5) Room DB and UI
   - `DeliveryLog` entity (messageId, recipients CSV, message, timestamp,
     deliveryMethod, status, errorMessage).
   - Debug UI to view logs and statuses.

Security & manifest notes
- Permissions: `FOREGROUND_SERVICE`, `SEND_SMS` (if sending SMS directly),
  `INTERNET` for local server. Request runtime SMS permission when required.
- Receiver security: prefer `exported=false` and require the Broadcaster to
  call `Intent.setPackage(...)`. If the receiver is exported, always verify
  HMAC before taking action and consider adding a custom permission.
- Bind Ktor to `127.0.0.1` only, never 0.0.0.0.

Edge cases & implementation hints
- Do NOT re-serialize payload before HMAC verification. Use the exact
  received payload string for HMAC calculation.
- For large lists, implement chunking (e.g., 100 recipients per batch) and
  throttle sends to avoid carrier/device limitations.
- Use a BroadcastReceiver -> startForegroundService flow for alarms
  (this Broadcaster uses that approach). The Receiver should call
  `startForegroundService(...)` when handling scheduled alarms on Android O+.

Acceptance tests (manual)
1. Install Broadcaster and SMS App on same device/emulator.
2. Immediate Intent test: from Broadcaster UI click "Broadcast Now".
   - SMS App must verify HMAC and enqueue/send the message. Room log must
     show status `sent`.
3. HTTP test: Broadcaster posts to localhost:8080/broadcast.
   - SMS App responds 200 and sends message.
4. Scheduled test: schedule message 1 minute ahead in Broadcaster.
   - AlarmReceiver must start MessageSenderService and send message.

Dev notes for implementer
- Use Ktor CIO engine, Gson or kotlinx.serialization per project style.
- Use Room + ViewModel + WorkManager (AndroidX).
- Keep the HMAC secret configurable (dev-only) in app settings.

Deliverables
- Full Android Studio project in Kotlin targeting Android 10+.
- README with build/run/test steps and HMAC secret instructions.
- Minimal instrumentation test(s) for HMAC verification + an emulator
  script to install both apps and run log capture.

Example HMAC verification snippet (Kotlin)
```kotlin
import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun verifyHmac(payloadJson: String, incomingHmacBase64: String, secret: String): Boolean {
  val mac = Mac.getInstance("HmacSHA256")
  val keySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
  mac.init(keySpec)
  val computed = mac.doFinal(payloadJson.toByteArray())
  val computedBase64 = Base64.encodeToString(computed, Base64.NO_WRAP)
  return MessageDigest.isEqual(computedBase64.toByteArray(), incomingHmacBase64.toByteArray())
}
```

End of instructions.