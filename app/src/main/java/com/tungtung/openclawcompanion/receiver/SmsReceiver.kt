package com.tungtung.openclawcompanion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.tungtung.openclawcompanion.data.FilterConfigStore
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
        val normalizedSender = sender.replace(" ", "")
        val smsAllowed = config.smsWhitelistAll || config.smsWhitelist.any {
            normalizedSender.contains(it.replace(" ", ""), ignoreCase = true)
        }
        if (!smsAllowed) return
        if (config.blockedKeywords.any { body.contains(it, ignoreCase = true) }) return

        GatewayService.sendEvent(
            context,
            type = "SMS",
            source = sender,
            text = body
        )
    }
}
