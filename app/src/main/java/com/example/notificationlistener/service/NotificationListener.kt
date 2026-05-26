package com.example.notificationlistener.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.notificationlistener.data.AppDatabase
import com.example.notificationlistener.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val extras = it.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            // Ignore notifications from own package, EXCEPT for the test notification
            if (packageName == applicationContext.packageName && title != "Teste de Captura") {
                return
            }

            // Filter: Ignore notifications with no content
            if (title.isEmpty() && text.isEmpty()) return

            // Filter: Ignore persistent media players (standard practice check)
            if (it.isOngoing && (it.notification.flags and android.app.Notification.FLAG_FOREGROUND_SERVICE) != 0) {
                 return // Skip ongoing foreground notifications (likely media or system)
            }

            val entity = NotificationEntity(
                package_name = packageName,
                title = title,
                content = text,
                created_at = System.currentTimeMillis()
            )

            scope.launch {
                AppDatabase.getDatabase(applicationContext).notificationDao().insert(entity)
                com.example.notificationlistener.data.LogManager.addLog("Capturada notificação de [$packageName]")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
