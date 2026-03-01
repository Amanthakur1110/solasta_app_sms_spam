package com.example.spamscan.ml

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.example.spamscan.data.SmsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsInboxScanner(private val context: Context) {

    suspend fun scanInbox(useCustomModel: Boolean? = null) = withContext(Dispatchers.IO) {
        // 1. Check Permissions First
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.READ_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("SmsInboxScanner", "Missing READ_SMS permission. Cannot scan inbox.")
            return@withContext
        }

        try {
            // Ensure detector is initialized safely
            SpamDetector.initialize(context)

            val cursor = context.contentResolver.query(
                Uri.parse("content://sms"),
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null,
                null,
                Telephony.Sms.DATE + " DESC LIMIT 200"
            )

            val preferences = com.example.spamscan.data.AppPreferences(context)
            val currentThreshold = preferences.spamThreshold.value
            val actualUseCustom = useCustomModel ?: preferences.useCustomModel.value
            val db = com.example.spamscan.data.local.AppDatabase.getDatabase(context)
            val smsDao = db.smsDao()

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addrIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val sender = it.getString(addrIndex) ?: "Unknown"
                    val body = it.getString(bodyIndex) ?: ""
                    val timestamp = it.getLong(dateIndex)

                    val cachedSms = smsDao.getCachedSmsById(id)

                    if (cachedSms == null) {
                        val result = SpamDetector.classify(context, body, currentThreshold, actualUseCustom)
                        smsDao.insertSms(
                            com.example.spamscan.data.local.CachedSms(
                                id = id,
                                sender = sender,
                                body = body,
                                timestamp = timestamp,
                                spamProbability = result.probability,
                                isSpam = result.isSpam
                            )
                        )
                    }
                }
            } ?: Log.e("SmsInboxScanner", "Cursor is null, could not read SMS")

        } catch (e: SecurityException) {
            Log.e("SmsInboxScanner", "SecurityException reading SMS: ${e.message}")
        } catch (e: Exception) {
            Log.e("SmsInboxScanner", "Error analyzing SMS inbox: ${e.message}")
        }
    }
}
