package com.example.notificationlistener

import android.app.Application
import androidx.work.*
import com.example.notificationlistener.service.WatchdogWorker
import java.util.concurrent.TimeUnit

class NotificationApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupWatchdog()
    }

    private fun setupWatchdog() {
        val watchdogRequest = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WatchdogService",
            ExistingPeriodicWorkPolicy.UPDATE,
            watchdogRequest
        )
    }
}
