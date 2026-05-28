package com.example.notificationlistener.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.notificationlistener.R
import com.example.notificationlistener.data.AppDatabase
import com.example.notificationlistener.data.LogManager
import com.example.notificationlistener.data.NotificationEntity
import com.example.notificationlistener.receiver.FilterActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val it = sbn ?: return
        val packageName = it.packageName

        val notification = it.notification
        val extras = notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val category = notification.category
        val channelId = notification.channelId

        // Ignore notifications from own package, EXCEPT for the test notification
        if (packageName == applicationContext.packageName && title != "Teste de Captura") {
            return
        }

        // Filter: Ignore notifications with no content
        if (title.isEmpty() && text.isEmpty()) return

        // // Filter: Ignore persistent media players
        // if (it.isOngoing && (notification.flags and android.app.Notification.FLAG_FOREGROUND_SERVICE) != 0) {
        //      return
        // }

        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)

            // // 1. Keyword Filter (High Priority - Skip saving if hits globally)
            // val keywords = db.keywordDao().getAllKeywords().map { it.word.lowercase() }
             val fullContent = (title + " " + text).lowercase()
            // if (keywords.any { fullContent.contains(it) }) {
            //     LogManager.addLog("Ignorada por palavra-chave global: [$packageName]")
            //     return@launch
            // }

            // // 2. Allowlist / Deniedlist Logic
            // val filters = db.appFilterDao().getAllFilters()
            // val allowlist = filters.filter { it.is_allowed }.map { it.package_name }
            // val deniedlist = filters.filter { !it.is_allowed }.map { it.package_name }

            // val shouldCapture = if (allowlist.isNotEmpty()) {
            //     allowlist.contains(packageName)
            // } else {
            //     !deniedlist.contains(packageName)
            // }

            // if (!shouldCapture) {
            //     if (!allowlist.contains(packageName) && !deniedlist.contains(packageName)) {
            //         // Onboarding
            //         withContext(Dispatchers.Main) {
            //             showOnboardingNotification(packageName)
            //         }
            //     }
            //     return@launch
            // }

            // 3. Mute Rules Logic - SALVA PRIMEIRO
            val muteRules = db.muteRuleDao().getAllRules()
            val isMuted = muteRules.any { rule ->
                rule.package_name == packageName &&
                (rule.category == null || rule.category == category) &&
                (rule.channel_id == null || rule.channel_id == channelId) &&
                (rule.text_keyword == null || fullContent.contains(rule.text_keyword.lowercase()))
            }

            val metadataBuilder = StringBuilder()
            metadataBuilder.append("KEY: ${it.key}\n")
            metadataBuilder.append("CHANNEL_ID: ${it.notification.channelId}\n")
            metadataBuilder.append("CATEGORY: ${it.notification.category}\n")
            metadataBuilder.append("POST_TIME: ${it.postTime}\n\n--- EXTRAS ---\n")
            val extrasSet = it.notification.extras.keySet()
            for (key in extrasSet) {
                val value = it.notification.extras.get(key)
                metadataBuilder.append("$key: $value\n")
            }
            val metadataString = metadataBuilder.toString()

            val entity = NotificationEntity(
                package_name = packageName,
                title = title,
                content = text,
                category = category,
                channel_id = channelId,
                is_muted = isMuted,
                created_at = System.currentTimeMillis(),
                raw_metadata = metadataString
            )

            db.notificationDao().insert(entity)

            if (isMuted) {
                cancelNotification(it.key)
                LogManager.addLog("Silenciada e Salva: [$packageName] Channel: $channelId")
            } else {
                LogManager.addLog("Capturada: [$packageName]")
            }
        }
    }

    // private fun showOnboardingNotification(targetPackage: String) {
    //     val channelId = "onboarding_channel"
    //     val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    //         val channel = NotificationChannel(channelId, "Onboarding", NotificationManager.IMPORTANCE_HIGH)
    //         notificationManager.createNotificationChannel(channel)
    //     }

    //     val appName = try {
    //         val ai = packageManager.getApplicationInfo(targetPackage, 0)
    //         packageManager.getApplicationLabel(ai).toString()
    //     } catch (e: Exception) {
    //         targetPackage
    //     }

    //     val notificationId = targetPackage.hashCode()

    //     val allowIntent = Intent(this, FilterActionReceiver::class.java).apply {
    //         putExtra("package_name", targetPackage)
    //         putExtra("is_allowed", true)
    //         putExtra("notification_id", notificationId)
    //     }
    //     val allowPending = PendingIntent.getBroadcast(this, notificationId * 2, allowIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    //     val denyIntent = Intent(this, FilterActionReceiver::class.java).apply {
    //         putExtra("package_name", targetPackage)
    //         putExtra("is_allowed", false)
    //         putExtra("notification_id", notificationId)
    //     }
    //     val denyPending = PendingIntent.getBroadcast(this, notificationId * 2 + 1, denyIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    //     val builder = NotificationCompat.Builder(this, channelId)
    //         .setSmallIcon(R.mipmap.ic_launcher)
    //         .setContentTitle("Novo aplicativo detectado")
    //         .setContentText("Deseja capturar notificações do app $appName?")
    //         .setPriority(NotificationCompat.PRIORITY_HIGH)
    //         .setAutoCancel(true)
    //         .addAction(0, "Permitir", allowPending)
    //         .addAction(0, "Bloquear", denyPending)

    //     notificationManager.notify(notificationId, builder.build())
    // }
}
