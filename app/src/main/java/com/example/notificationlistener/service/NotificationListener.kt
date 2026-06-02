package com.example.notificationlistener.service

import android.app.Notification
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
import com.example.notificationlistener.util.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class NotificationListener : NotificationListenerService() {

    companion object {
        @Volatile
        var lastCaptureTimestamp: Long = 0L
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val notificationCache = ConcurrentHashMap<String, Long>()
    private val CACHE_TTL_MS = 5 * 60 * 1000L
    private val ERROR_NOTIFICATION_ID = 9999

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            super.onNotificationPosted(sbn)
            val it = sbn ?: return
            val packageName = it.packageName

            val notification = it.notification
            val extras = notification.extras
            val title = extras.getCharSequence("android.title")?.toString() ?: ""

            // Watchdog Check
            if (title == "health_check") {
                lastCaptureTimestamp = System.currentTimeMillis()
                return
            }

            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val subText = extras.getCharSequence("android.subText")?.toString()

            // 1. Filtro Universal de Progresso (Ignora uploads, downloads e escaneamentos)
            val progress = extras.getInt("android.progress")
            if (progress > 0 && progress < 100) {
                return
            }

            // 2. Filtro Universal de Conteúdo Estático (Desduplicação)
            val cacheKey = "$packageName|${it.id}|$title|$text"
            val now = System.currentTimeMillis()
            val expiration = notificationCache[cacheKey]

            if (expiration != null && now < expiration) {
                notificationCache[cacheKey] = now + CACHE_TTL_MS
                return
            }
            notificationCache[cacheKey] = now + CACHE_TTL_MS

            // Limpeza ocasional do cache
            if (notificationCache.size > 500) {
                notificationCache.entries.removeIf { it.value < now }
            }

            val category = notification.category
            val channelId = notification.channelId

            // Ignore notifications from own package (health_check is already handled above)
            if (packageName == applicationContext.packageName) {
                return
            }

            // Filter: Ignore notifications with no content
            if (title.isEmpty() && text.isEmpty()) return

            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)

                    // 3. Mute Rules Logic - SALVA PRIMEIRO
                    val muteRules = db.muteRuleDao().getRulesForPackage(packageName)

                    fun String.normalizeText(): String {
                        val nfd = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
                        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                    }

                    val normFullContent = (title + " " + text).normalizeText().lowercase()

                    val isMuted = muteRules.any { rule ->
                        // 1. Se (package_name preenchido AND sbn.packageName != package_name) -> NÃO muta (Ignora a regra)
                        if (!rule.package_name.isNullOrBlank() && rule.package_name != packageName) return@any false

                        // 2. Se (keywords_to_mute não estiver vazia AND texto da notificação NÃO der match com nenhuma tag de mute via REGEXP) -> NÃO muta
                        val muteTags = rule.keywords_to_mute ?: emptyList()
                        if (muteTags.isNotEmpty()) {
                            val hasMuteMatch = muteTags.any { tag ->
                                val regex = Regex(".*${Regex.escape(tag.normalizeText())}.*", RegexOption.IGNORE_CASE)
                                regex.containsMatchIn(normFullContent)
                            }
                            if (!hasMuteMatch) return@any false
                        }

                        // 3. Se (keywords_to_bypass não estiver vazia AND texto da notificação DER match com alguma tag de bypass) -> NÃO muta (A exceção anula o bloqueio)
                        val bypassTags = rule.keywords_to_bypass ?: emptyList()
                        if (bypassTags.isNotEmpty()) {
                            val hasBypassMatch = bypassTags.any { tag ->
                                val regex = Regex(".*${Regex.escape(tag.normalizeText())}.*", RegexOption.IGNORE_CASE)
                                regex.containsMatchIn(normFullContent)
                            }
                            if (hasBypassMatch) return@any false
                        }

                        // 4. Caso contrário -> MUTA imediatamente
                        true
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
                        sub_text = subText,
                        category = category,
                        channel_id = channelId,
                        is_muted = isMuted,
                        created_at = System.currentTimeMillis(),
                        raw_metadata = metadataString
                    )

                    try {
                        db.notificationDao().insert(entity)

                        if (isMuted) {
                            cancelNotification(it.key)
                            LogManager.addLog("Silenciada e Salva: [$packageName] Channel: $channelId")
                        } else {
                            LogManager.addLog("Capturada: [$packageName]")
                        }
                    } catch (e: Exception) {
                        FileLogger.writeError(
                            context = applicationContext,
                            className = "NotificationListener",
                            methodName = "persistNotificationEntity",
                            operation = "Gravação de notificação capturada no banco de dados SQLite local",
                            contextMap = mapOf(
                                "EntityPackageName" to entity.package_name,
                                "EntityTitle" to entity.title,
                                "DatabaseVersion" to 11 // Conforme AppDatabase.kt
                            ),
                            exception = e
                        )
                        showErrorNotification(e)
                    }
                } catch (e: Exception) {
                    handleGlobalError(e, it)
                }
            }
        } catch (e: Exception) {
            handleGlobalError(e, sbn)
        }
    }

    private fun handleGlobalError(e: Exception, sbn: StatusBarNotification?) {
        val contextMap = mutableMapOf<String, Any?>()
        sbn?.let {
            contextMap["PackageName"] = it.packageName
            contextMap["NotificationId"] = it.id
            contextMap["ChannelId"] = it.notification.channelId
            contextMap["TitleLength"] = it.notification.extras.getString(Notification.EXTRA_TITLE)?.length ?: 0
            contextMap["TextLength"] = it.notification.extras.getString(Notification.EXTRA_TEXT)?.length ?: 0
        }

        FileLogger.writeError(
            context = applicationContext,
            className = "NotificationListener",
            methodName = "onNotificationPosted",
            operation = "Processamento e filtragem de notificação recebida",
            contextMap = contextMap,
            exception = e
        )
        showErrorNotification(e)
    }

    private fun showErrorNotification(e: Exception) {
        val channelId = "error_logs"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Falhas do Sistema", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Erro de Processamento")
            .setContentText("Uma falha foi interceptada e salva no log.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Erro: ${e.message}\nConsulte o arquivo app_errors.txt para mais detalhes."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(ERROR_NOTIFICATION_ID, builder.build())
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
