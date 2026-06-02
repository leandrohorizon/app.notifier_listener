package com.example.notificationlistener.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.notificationlistener.R
import com.example.notificationlistener.data.LogManager
import com.example.notificationlistener.util.FileLogger
import kotlinx.coroutines.delay
import kotlin.random.Random

class WatchdogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "watchdog_channel"
        val errorChannelId = "error_logs"
        val errorNotificationId = 9999

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Watchdog Check", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
            
            val errorChannel = NotificationChannel(errorChannelId, "Falhas de Monitoramento", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(errorChannel)
        }

        val testNotificationId = Random.nextInt(10000, 20000)
        val startTime = System.currentTimeMillis()

        // 1. Disparar notificação de teste
        val testNotification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("health_check")
            .setContentText("Verificando integridade do serviço...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(testNotificationId, testNotification)

        // 2. Aguardar 5 segundos
        delay(5000)

        // 3. Validar se o NotificationListener capturou
        val lastCapture = NotificationListener.lastCaptureTimestamp
        
        // Remove a notificação de teste
        notificationManager.cancel(testNotificationId)

        if (lastCapture < startTime) {
            val currentTime = System.currentTimeMillis()
            val delaySeconds = if (lastCapture > 0) (currentTime - lastCapture) / 1000 else -1

            // Falha detectada - Registro Estruturado
            FileLogger.writeError(
                context = applicationContext,
                className = "NotificationWatchdog",
                methodName = "verifyListenerHealth",
                operation = "Verificação periódica de atividade do Listener",
                contextMap = mapOf(
                    "CheckIntervalMinutes" to 15,
                    "LastCaptureTimestamp" to lastCapture,
                    "CurrentTimestamp" to currentTime,
                    "DelayDetectedSeconds" to delaySeconds
                ),
                exception = IllegalStateException("NotificationListener encontra-se inativo ou desvinculado pelo sistema operacional")
            )

            val errorNotification = NotificationCompat.Builder(applicationContext, errorChannelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Falha de Monitoramento")
                .setContentText("O serviço de captura parou de responder.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("O Watchdog detectou que o NotificationListener não está processando eventos. Verifique as permissões."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(errorNotificationId, errorNotification)
        } else {
            LogManager.addLog("Watchdog: Monitoramento executado com sucesso")
        }

        return Result.success()
    }
}
