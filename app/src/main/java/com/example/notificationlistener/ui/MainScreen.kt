package com.example.notificationlistener.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notificationlistener.data.NotificationEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(viewModel: NotificationViewModel) {
    val logs by viewModel.logs.collectAsState()
    val pending by viewModel.pendingNotifications.collectAsState(initial = emptyList())
    var showLogs by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { viewModel.syncNotifications() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sincronizar Agora (${pending.size} pendentes)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showLogs) "Logs da Aplicação" else "Notificações Pendentes",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = { showLogs = !showLogs }) {
                Text(if (showLogs) "Ver Pendentes" else "Ver Logs")
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            if (showLogs) {
                LogList(logs)
            } else {
                PendingList(pending) { id -> viewModel.deleteNotification(id) }
            }
        }
    }
}

@Composable
fun LogList(logs: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        items(logs) { log ->
            Text(text = log, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PendingList(notifications: List<NotificationEntity>, onDelete: (Long) -> Unit) {
    if (notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhuma notificação pendente")
        }
    } else {
        val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notifications, key = { it.id }) { item ->
                PendingItem(item, sdf.format(Date(item.created_at)), onDelete)
            }
        }
    }
}

@Composable
fun PendingItem(item: NotificationEntity, time: String, onDelete: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.package_name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(text = item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = item.content, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(text = time, style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = { onDelete(item.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
        }
    }
}
