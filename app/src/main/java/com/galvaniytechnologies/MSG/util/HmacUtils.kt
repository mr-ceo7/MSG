package com.galvaniytechnologies.MSG.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacUtils {
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val SECRET_KEY = "your-experimental-secret" // Development key as per instructions

    fun verifyHmac(payloadJson: String, incomingHmacBase64: String): Boolean {
        return try {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(), HMAC_ALGORITHM)
            mac.init(keySpec)
            
            val computed = mac.doFinal(payloadJson.toByteArray())
            val computedBase64 = Base64.encodeToString(computed, Base64.NO_WRAP)
            
            MessageDigest.isEqual(
                computedBase64.toByteArray(),
                incomingHmacBase64.toByteArray()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}