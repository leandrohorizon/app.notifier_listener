package com.example.notificationlistener.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notificationlistener.data.MuteRuleEntity

@Composable
fun MuteManagementScreen(viewModel: NotificationViewModel) {
    val rules by viewModel.muteRules.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Regras de Silenciamento", style = MaterialTheme.typography.headlineSmall)
        Text("Notificações que batem com estas regras são canceladas e ignoradas automaticamente.", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (rules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma regra de silenciamento")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(rules) { rule ->
                        MuteRuleItem(rule) { viewModel.removeMuteRule(rule) }
                    }
                }
            }
        }
    }
}

@Composable
fun MuteRuleItem(rule: MuteRuleEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(rule.package_name, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            if (rule.category != null) {
                Text("Categoria: ${rule.category}", style = MaterialTheme.typography.labelSmall)
            }
            if (rule.channel_id != null) {
                Text("Canal: ${rule.channel_id}", style = MaterialTheme.typography.labelSmall)
            }
            if (rule.text_keyword != null) {
                Text("Termo: ${rule.text_keyword}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
        }
    }
}
