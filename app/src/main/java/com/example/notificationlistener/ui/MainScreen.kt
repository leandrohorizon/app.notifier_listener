package com.example.notificationlistener.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.notificationlistener.data.NotificationEntity
import com.example.notificationlistener.data.SavedFilterEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: NotificationViewModel) {
    val pending by viewModel.pendingNotifications.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterPackage by viewModel.filterPackage.collectAsState()
    val distinctPackages by viewModel.distinctPackages.collectAsState(initial = emptyList())
    val selectedIds by viewModel.selectedIds.collectAsState()
    val savedFilters by viewModel.savedFilters.collectAsState(initial = emptyList())
    val activePreset by viewModel.activePreset.collectAsState()
    val showMutedOnly by viewModel.showMutedOnly.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSyncConfirm by remember { mutableStateOf(false) }
    var muteCandidate by remember { mutableStateOf<NotificationEntity?>(null) }
    
    var filterToEdit by remember { mutableStateOf<SavedFilterEntity?>(null) }
    var showFilterEditor by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<SavedFilterEntity?>(null) }
    var filtersExpanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 1. Barra de Busca e Filtro de App
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Pesquisar...") },
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

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Linha de Controles (Toggle Presets, Silenciados e Lote)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Esquerda: Toggles de Visibilidade
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                    Icon(
                        if (filtersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Tune,
                        contentDescription = "Presets",
                        tint = if (activePreset != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                
                FilterChip(
                    selected = showMutedOnly,
                    onClick = { viewModel.toggleMutedOnly() },
                    label = { Text("Silenciados", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            if (showMutedOnly) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            // Direita: Controles de Lote
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedIds.isNotEmpty()) {
                    IconButton(onClick = { showSyncConfirm = true }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sincronizar", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
                
                Checkbox(
                    checked = pending.isNotEmpty() && selectedIds.size == pending.size,
                    onCheckedChange = { checked ->
                        if (checked) viewModel.selectAll(pending.map { it.id })
                        else viewModel.clearSelection()
                    }
                )
                Text("Tudo", style = MaterialTheme.typography.labelSmall)
            }
        }

        // 3. Linha Animada de Filtros Salvos (Presets)
        AnimatedVisibility(visible = filtersExpanded) {
            Column {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = activePreset == null,
                            onClick = { viewModel.setActivePreset(null) },
                            label = { Text("Início") }
                        )
                    }

                    items(savedFilters) { filter ->
                        FilterChip(
                            selected = activePreset?.id == filter.id,
                            onClick = { viewModel.setActivePreset(filter) },
                            label = { Text(filter.name) },
                            modifier = Modifier.combinedClickable(
                                onClick = { viewModel.setActivePreset(filter) },
                                onLongClick = { presetToDelete = filter }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    filterToEdit = filter
                                    showFilterEditor = true
                                }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }

                    item {
                        IconButton(
                            onClick = { 
                                filterToEdit = null
                                showFilterEditor = true 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Novo Filtro", modifier = Modifier.size(24.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // 4. Botão de Sincronização Principal
        Button(
            onClick = { if (selectedIds.isEmpty()) viewModel.syncNotifications() else showSyncConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = pending.isNotEmpty()
        ) {
            val count = if (selectedIds.isEmpty()) pending.size else selectedIds.size
            val label = if (selectedIds.isEmpty()) "Sincronizar Todos" else "Sincronizar Selecionados"
            Text("$label ($count)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            PendingList(
                notifications = pending,
                selectedIds = selectedIds,
                onToggleSelect = { viewModel.toggleSelection(it) },
                onMuteClick = { muteCandidate = it }
            )
        }
    }

    // Editor de Filtros (Criação e Edição)
    if (showFilterEditor) {
        FilterEditorDialog(
            filter = filterToEdit,
            installedApps = installedApps,
            onDismiss = { showFilterEditor = false },
            onSave = { name, pkgs, keyword ->
                if (filterToEdit == null) {
                    viewModel.saveCurrentFilter(name, pkgs, keyword)
                } else {
                    viewModel.updateSavedFilter(filterToEdit!!.copy(
                        name = name,
                        package_names = pkgs.joinToString(","),
                        keyword_query = keyword
                    ))
                }
                showFilterEditor = false
            },
            onDelete = {
                filterToEdit?.let { presetToDelete = it }
                showFilterEditor = false
            }
        )
    }

    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text("Deletar Filtro") },
            text = { Text("Deseja remover o preset '${preset.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePreset(preset)
                    presetToDelete = null
                }) { Text("Deletar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    // Confirmações existentes
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Tem certeza que deseja apagar as ${selectedIds.size} notificações selecionadas?") },
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
            text = { Text("Deseja enviar as ${selectedIds.size} notificações selecionadas?") },
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

    muteCandidate?.let { candidate ->
        var muteScope by remember { mutableIntStateOf(0) }
        var keyword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { muteCandidate = null },
            title = { Text("Silenciar Notificações") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Canal: ${candidate.channel_id}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = muteScope == 0, onClick = { muteScope = 0 })
                        Text("Silenciar canal", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = muteScope == 1, onClick = { muteScope = 1 })
                        Text("Com termo", modifier = Modifier.padding(start = 8.dp))
                    }
                    if (muteScope == 1) {
                        OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("Palavra-chave") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addMuteRule(candidate.package_name, candidate.category, candidate.channel_id, if (muteScope == 1) keyword else null)
                    muteCandidate = null
                }) { Text("Silenciar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { muteCandidate = null }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterEditorDialog(
    filter: SavedFilterEntity?,
    installedApps: List<AppInfo>,
    onDismiss: () -> Unit,
    onSave: (String, List<String>, String?) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(filter?.name ?: "") }
    var selectedPkgs by remember { mutableStateOf(filter?.package_names?.split(",")?.filter { it.isNotBlank() } ?: emptyList()) }
    var keyword by remember { mutableStateOf(filter?.keyword_query ?: "") }
    var appSearch by remember { mutableStateOf("") }

    val filteredApps = remember(appSearch, installedApps) {
        if (appSearch.isBlank()) installedApps else installedApps.filter { it.name.contains(appSearch, ignoreCase = true) || it.packageName.contains(appSearch, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (filter == null) "Novo Filtro" else "Editar Filtro", style = MaterialTheme.typography.titleLarge)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Preset") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Palavra-chave (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Selecionar Aplicativos (${selectedPkgs.size})", style = MaterialTheme.typography.titleSmall)
                
                OutlinedTextField(
                    value = appSearch,
                    onValueChange = { appSearch = it },
                    label = { Text("Buscar app...") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true,
                    trailingIcon = { if (appSearch.isNotEmpty()) IconButton(onClick = { appSearch = "" }) { Icon(Icons.Default.Close, null) } }
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredApps) { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {
                                selectedPkgs = if (selectedPkgs.contains(app.packageName)) selectedPkgs - app.packageName else selectedPkgs + app.packageName
                            }).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedPkgs.contains(app.packageName),
                                onCheckedChange = { checked ->
                                    selectedPkgs = if (checked) selectedPkgs + app.packageName else selectedPkgs - app.packageName
                                }
                            )
                            Column {
                                Text(app.name, style = MaterialTheme.typography.bodyMedium)
                                Text(app.packageName, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (filter != null) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excluir")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(name, selectedPkgs, keyword.ifBlank { null }) },
                            enabled = name.isNotBlank()
                        ) { Text("Salvar") }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingList(
    notifications: List<NotificationEntity>, 
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onMuteClick: (NotificationEntity) -> Unit
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
                    onToggleSelect = { onToggleSelect(item.id) },
                    onMuteClick = { onMuteClick(item) }
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
    onToggleSelect: () -> Unit,
    onMuteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
        
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.package_name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                if (item.is_muted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text("SILENCIADO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Text(text = item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (item.is_muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
            Text(text = item.content, style = MaterialTheme.typography.bodyMedium, maxLines = 2, color = if (item.is_muted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!item.category.isNullOrEmpty()) {
                    Badge { Text(item.category, style = MaterialTheme.typography.labelSmall) }
                }
                if (!item.channel_id.isNullOrEmpty()) {
                    Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) { 
                        Text(item.channel_id, style = MaterialTheme.typography.labelSmall) 
                    }
                }
            }
            Text(text = time, style = MaterialTheme.typography.labelSmall)
        }

        IconButton(onClick = onMuteClick) {
            Icon(Icons.Default.NotificationsOff, contentDescription = "Silenciar", tint = MaterialTheme.colorScheme.outline)
        }
    }
}
