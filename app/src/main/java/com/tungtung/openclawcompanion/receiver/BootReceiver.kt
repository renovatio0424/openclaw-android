package com.tungtung.openclawcompanion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tungtung.openclawcompanion.service.GatewayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            GatewayService.start(context)
        }
    }
}
