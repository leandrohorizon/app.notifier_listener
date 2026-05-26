package com.example.notificationlistener.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notificationlistener.data.NotificationEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: NotificationViewModel) {
    val pending by viewModel.pendingNotifications.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterPackage by viewModel.filterPackage.collectAsState()
    val distinctPackages by viewModel.distinctPackages.collectAsState(initial = emptyList())
    val selectedIds by viewModel.selectedIds.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSyncConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Barra de Busca e Filtro
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Pesquisar notificações...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        Icons.Default.FilterList, 
                        contentDescription = "Filtrar por App",
                        tint = if (filterPackage != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Todos") },
                        onClick = { viewModel.setFilterPackage(null); showFilterMenu = false }
                    )
                    distinctPackages.forEach { pkg ->
                        DropdownMenuItem(
                            text = { Text(pkg) },
                            onClick = { viewModel.setFilterPackage(pkg); showFilterMenu = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ações em Lote / Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = pending.isNotEmpty() && selectedIds.size == pending.size,
                    onCheckedChange = { checked ->
                        if (checked) viewModel.selectAll(pending.map { it.id })
                        else viewModel.clearSelection()
                    }
                )
                Text("Selecionar Tudo", style = MaterialTheme.typography.bodyMedium)
            }
            
            if (selectedIds.isNotEmpty()) {
                Row {
                    IconButton(onClick = { showSyncConfirm = true }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sincronizar Selecionados")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Apagar Selecionados", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Botão Principal (Sincronizar Tudo ou Selecionados)
        Button(
            onClick = { if (selectedIds.isEmpty()) viewModel.syncNotifications() else showSyncConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = pending.isNotEmpty()
        ) {
            val count = if (selectedIds.isEmpty()) pending.size else selectedIds.size
            val label = if (selectedIds.isEmpty()) "Sincronizar Tudo" else "Sincronizar Selecionados"
            Text("$label ($count)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            PendingList(
                notifications = pending,
                selectedIds = selectedIds,
                onToggleSelect = { viewModel.toggleSelection(it) }
            )
        }
    }

    // Diálogos de Confirmação
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Tem certeza que deseja apagar as ${selectedIds.size} notificações selecionadas? Esta ação é irreversível.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNotifications(selectedIds.toList())
                    showDeleteConfirm = false
                }) {
                    Text("Apagar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (showSyncConfirm) {
        AlertDialog(
            onDismissRequest = { showSyncConfirm = false },
            title = { Text("Iniciar Sincronização") },
            text = { Text("Deseja enviar as ${selectedIds.size} notificações selecionadas para o servidor agora?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.syncNotifications(selectedIds.toList())
                    showSyncConfirm = false
                }) {
                    Text("Sincronizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun PendingList(
    notifications: List<NotificationEntity>, 
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit
) {
    if (notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhuma notificação encontrada")
        }
    } else {
        val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notifications, key = { it.id }) { item ->
                PendingItem(
                    item = item, 
                    time = sdf.format(Date(item.created_at)),
                    isSelected = selectedIds.contains(item.id),
                    onToggleSelect = { onToggleSelect(item.id) }
                )
            }
        }
    }
}

@Composable
fun PendingItem(
    item: NotificationEntity, 
    time: String, 
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
        
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(text = item.package_name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(text = item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = item.content, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(text = time, style = MaterialTheme.typography.labelSmall)
        }
    }
}
