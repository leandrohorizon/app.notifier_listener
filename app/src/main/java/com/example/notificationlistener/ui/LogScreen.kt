package com.example.notificationlistener.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun LogScreen(viewModel: NotificationViewModel) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll para o final quando novos logs chegam
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Logs da Aplicação",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Nenhum log registrado", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(logs) { log ->
                        val logColor = getLogColor(log)
                        Text(
                            text = log,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                color = logColor,
                                fontSize = MaterialTheme.typography.bodySmall.fontSize
                            ),
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getLogColor(log: String): Color {
    return when {
        log.contains("Capturada", ignoreCase = true) -> Color(0xFF00BCD4) // Cyan
        log.contains("Sincronizados", ignoreCase = true) -> Color(0xFF4CAF50) // Green
        log.contains("Falha", ignoreCase = true) || log.contains("Erro", ignoreCase = true) -> Color(0xFFF44336) // Red
        log.contains("Definido", ignoreCase = true) || log.contains("Deletada", ignoreCase = true) || log.contains("Gerada", ignoreCase = true) -> Color(0xFFFFEB3B) // Yellow
        else -> Color.LightGray
    }
}
