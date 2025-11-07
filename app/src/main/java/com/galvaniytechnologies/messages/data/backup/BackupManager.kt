package com.galvaniytechnologies.messages.data.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.galvaniytechnologies.messages.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val context: Context,
    private val appDatabase: AppDatabase
) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        "secret_shared_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun getEncryptionKey(): String {
        var key = sharedPreferences.getString("db_encryption_key", null)
        if (key == null) {
            key = generateRandomKey()
            sharedPreferences.edit().putString("db_encryption_key", key).apply()
        }
        return key
    }

    private fun generateRandomKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32) // 256 bits
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun backupDatabase(backupFile: File) = withContext(Dispatchers.IO) {
        val databasePath = context.getDatabasePath("messages.db").absolutePath
        val databaseFile = File(databasePath)

        if (databaseFile.exists()) {
            appDatabase.close()
            databaseFile.copyTo(backupFile, overwrite = true)
        } else {
            throw FileNotFoundException("Database file not found")
        }
    }

    suspend fun restoreDatabase(backupFile: File) = withContext(Dispatchers.IO) {
        if (!backupFile.exists()) {
            throw IllegalArgumentException("Backup file does not exist")
        }

        val databasePath = context.getDatabasePath("messages.db").absolutePath
        val databaseFile = File(databasePath)

        appDatabase.close()
        context.deleteDatabase("messages.db")
        backupFile.copyTo(databaseFile, overwrite = true)
    }
}
