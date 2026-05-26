package com.example.notificationlistener.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notificationlistener.service.ForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ForegroundService.startService(context)
        }
    }
}
