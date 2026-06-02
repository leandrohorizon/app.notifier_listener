package com.example.notificationlistener.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notificationlistener.service.WatchdogWorker
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.notificationlistener.R
import com.example.notificationlistener.data.LogManager

@Composable
fun SettingsScreen(viewModel: NotificationViewModel) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(viewModel.getSyncUrl()) }
    var isListenerEnabled by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    // Update status when screen is visible
    DisposableEffect(Unit) {
        isListenerEnabled = isNotificationServiceEnabled(context)
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configurações", style = MaterialTheme.typography.headlineSmall)

        // Status do Listener
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Status do Listener: ")
            Text(
                if (isListenerEnabled) "ATIVO" else "INATIVO",
                color = if (isListenerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }) {
            Text("Configurar Acesso ao Listener")
        }

        HorizontalDivider()

        // URL do Endpoint
        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it
                viewModel.setSyncUrl(it)
            },
            label = { Text("URL do Endpoint de Sincronização") },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // Teste de Captura
        Button(onClick = {
            val watchdogRequest = OneTimeWorkRequestBuilder<WatchdogWorker>().build()
            WorkManager.getInstance(context).enqueue(watchdogRequest)
            LogManager.addLog("Watchdog manual disparado")
        }) {
            Text("Gerar Notificação de Teste (Watchdog)")
        }
        
        Button(onClick = {
            // Check battery optimization
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        }) {
            Text("Desativar Otimização de Bateria")
        }
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
    return enabledPackages.contains(context.packageName)
}
