package com.tungtung.openclawcompanion.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.tungtung.openclawcompanion.data.FilterConfigStore

class NotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        val config = FilterConfigStore.load()
        if (packageName !in config.appWhitelist) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        val merged = listOf(title, text)
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
        if (merged.isBlank()) return
        if (config.blockedKeywords.any { merged.contains(it, ignoreCase = true) }) return

        GatewayService.sendEvent(
            applicationContext,
            type = "NOTI",
            source = packageName,
            text = merged
        )
    }
}
