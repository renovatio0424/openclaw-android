package com.tungtung.openclawcompanion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.tungtung.openclawcompanion.data.FilterConfigStore
import com.tungtung.openclawcompanion.data.SmsHistoryEntry
import com.tungtung.openclawcompanion.service.GatewayService

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val sender = messages[0].displayOriginatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
        if (sender.isBlank() || body.isBlank()) return

        val config = FilterConfigStore.load()
        if (config.smsIncludeKeywords.isEmpty()) return

        val matchedKeyword = config.smsIncludeKeywords.firstOrNull {
            body.contains(it, ignoreCase = true)
        } ?: return

        if (config.blockedKeywords.any { body.contains(it, ignoreCase = true) }) return

        FilterConfigStore.addSmsHistory(
            SmsHistoryEntry(
                timestamp = System.currentTimeMillis(),
                sender = sender,
                body = body,
                matchedKeyword = matchedKeyword
            )
        )

        GatewayService.sendEvent(
            context,
            type = "SMS",
            source = sender,
            text = body
        )
    }
}
