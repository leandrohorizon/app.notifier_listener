package com.example.notificationlistener.ui

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.drawable.toBitmap
import com.example.notificationlistener.data.NotificationEntity
import com.example.notificationlistener.data.SavedFilterEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: NotificationViewModel) {
    val pending by viewModel.pendingNotifications.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val viewingNotification by viewModel.viewingNotification.collectAsState()
    val savedFilters by viewModel.savedFilters.collectAsState(initial = emptyList())
    val activePreset by viewModel.activePreset.collectAsState()
    val showMutedOnly by viewModel.showMutedOnly.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    
    val activeCount by viewModel.activeCount.collectAsState(initial = 0)
    val mutedCount by viewModel.mutedCount.collectAsState(initial = 0)

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSyncConfirm by remember { mutableStateOf(false) }
    var muteCandidate by remember { mutableStateOf<NotificationEntity?>(null) }
    var inspectionCandidate by remember { mutableStateOf<NotificationEntity?>(null) }
    
    var filterToEdit by remember { mutableStateOf<SavedFilterEntity?>(null) }
    var showFilterEditor by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<SavedFilterEntity?>(null) }

    if (viewingNotification != null) {
        BackHandler { viewModel.setViewingNotification(null) }
        NotificationDetailScreen(
            notification = viewingNotification!!,
            onBack = { viewModel.setViewingNotification(null) },
            onToggleMute = { viewModel.toggleMuteStatus(viewingNotification!!) }
        )
        return
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0F0F17))
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Top Bar / Busca
            if (selectedIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    color = Color(0xFF1A1A23),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF252535))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.White)
                        }
                        Text(
                            text = "${selectedIds.size} selecionadas",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                        
                        IconButton(onClick = { if (pending.isNotEmpty()) showSyncConfirm = true }) {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = "Sincronizar", tint = Color.Gray)
                        }
                        
                        IconButton(onClick = { if (selectedIds.isNotEmpty()) showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = Color(0xFFFF5252))
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Pesquisar notificações...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFF1E1E2A),
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedContainerColor = Color(0xFF1E1E2A),
                        focusedContainerColor = Color(0xFF1E1E2A),
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Tabs: Ativas / Silenciadas
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabButton(
                    label = "Ativas",
                    count = activeCount,
                    selected = !showMutedOnly,
                    icon = Icons.Default.FlashOn,
                    onClick = { if (showMutedOnly) viewModel.toggleMutedOnly() },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    label = "Silenciadas",
                    count = mutedCount,
                    selected = showMutedOnly,
                    icon = Icons.Default.NotificationsOff,
                    onClick = { if (!showMutedOnly) viewModel.toggleMutedOnly() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Filtros Horizontais
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChipSmall(
                        label = "Todas",
                        selected = activePreset == null,
                        onClick = { viewModel.setActivePreset(null) }
                    )
                }
                items(savedFilters) { filter ->
                    FilterChipSmall(
                        label = filter.name,
                        selected = activePreset?.id == filter.id,
                        onClick = { viewModel.setActivePreset(filter) },
                        onLongClick = {
                            filterToEdit = filter
                            showFilterEditor = true
                        }
                    )
                }
                item {
                    IconButton(
                        onClick = { 
                            filterToEdit = null
                            showFilterEditor = true 
                        },
                        modifier = Modifier
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Lista de Notificações
            Box(modifier = Modifier.weight(1f)) {
                PendingList(
                    notifications = pending,
                    selectedIds = selectedIds,
                    onToggleSelect = { viewModel.toggleSelection(it) },
                    onClick = { notification ->
                        if (selectedIds.isNotEmpty()) {
                            viewModel.toggleSelection(notification.id)
                        } else {
                            viewModel.setViewingNotification(notification)
                        }
                    },
                    onMuteClick = { muteCandidate = it },
                    onInspectClick = { inspectionCandidate = it }
                )
            }
        }
    }

    // Dialogs
    if (showFilterEditor) {
        FilterEditorDialog(
            filter = filterToEdit,
            installedApps = installedApps,
            onDismiss = { showFilterEditor = false },
            onSave = { name: String, pkgs: List<String>, keywordList: List<String> ->
                if (filterToEdit == null) viewModel.saveCurrentFilter(name, pkgs, keywordList)
                else viewModel.updateSavedFilter(filterToEdit!!.copy(
                    name = name, 
                    package_names = pkgs.joinToString(","), 
                    keyword_list = keywordList
                ))
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
            dismissButton = { TextButton(onClick = { presetToDelete = null }) { Text("Cancelar") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Tem certeza que deseja apagar as ${selectedIds.size} notificações selecionadas?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNotifications(selectedIds.toList())
                    showDeleteConfirm = false
                }) { Text("Apagar", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }

    if (showSyncConfirm) {
        AlertDialog(
            onDismissRequest = { showSyncConfirm = false },
            title = { Text("Sincronização") },
            text = { Text("Deseja sincronizar ${if (selectedIds.isEmpty()) "todas" else selectedIds.size} as notificações?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.syncNotifications(if (selectedIds.isEmpty()) null else selectedIds.toList())
                    showSyncConfirm = false
                }) { Text("Sincronizar") }
            },
            dismissButton = { TextButton(onClick = { showSyncConfirm = false }) { Text("Cancelar") } }
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
                    val muteTags = if (muteScope == 1 && keyword.isNotBlank()) listOf(keyword) else emptyList()
                    viewModel.addMuteRule(candidate.package_name, muteTags)
                    muteCandidate = null
                }) { Text("Silenciar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { muteCandidate = null }) { Text("Cancelar") }
            }
        )
    }

    inspectionCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { inspectionCandidate = null },
            title = { Text("Metadados (Debug)") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    val scrollState = androidx.compose.foundation.rememberScrollState()
                    Text(
                        text = candidate.raw_metadata ?: "Sem metadados capturados",
                        modifier = Modifier.verticalScroll(scrollState),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectionCandidate = null }) { Text("Fechar") }
            }
        )
    }
}

@Composable
fun TabButton(
    label: String,
    count: Int,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) Color(0xFF1E1E2A) else Color.Transparent
    val borderColor = if (selected) Color(0xFF6C63FF) else Color(0xFF1E1E2A)
    val contentColor = if (selected) Color.White else Color.Gray

    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = if (selected) Color(0xFF6C63FF).copy(alpha = 0.2f) else Color(0xFF1E1E2A),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    color = if (selected) Color(0xFF6C63FF) else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterChipSmall(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val backgroundColor = if (selected) Color(0x226C63FF) else Color(0xFF1E1E2A)
    val contentColor = if (selected) Color(0xFF6C63FF) else Color.White

    Surface(
        modifier = Modifier
            .height(36.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C63FF)) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(label, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PendingList(
    notifications: List<NotificationEntity>, 
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onClick: (NotificationEntity) -> Unit,
    onMuteClick: (NotificationEntity) -> Unit,
    onInspectClick: (NotificationEntity) -> Unit
) {
    if (notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Vazio", color = Color.Gray)
        }
    } else {
        val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notifications, key = { it.id }) { item ->
                NotificationCard(
                    item = item, 
                    time = sdf.format(Date(item.created_at)),
                    isSelected = selectedIds.contains(item.id),
                    selectionActive = selectedIds.isNotEmpty(),
                    onToggleSelect = { onToggleSelect(item.id) },
                    onClick = { onClick(item) },
                    onMuteClick = { onMuteClick(item) },
                    onInspectClick = { onInspectClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationCard(
    item: NotificationEntity, 
    time: String, 
    isSelected: Boolean,
    selectionActive: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onMuteClick: () -> Unit,
    onInspectClick: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(item.package_name) {
        try {
            context.packageManager.getApplicationIcon(item.package_name)
        } catch (e: Exception) {
            null
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF252538) else Color(0xFF1A1A23),
        label = "backgroundColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onToggleSelect
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C63FF)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionActive) {
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            // App Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF252535)),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(Icons.Default.Android, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.package_name, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                Text(text = item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (!item.sub_text.isNullOrBlank()) {
                    Text(text = item.sub_text, color = Color(0xFF6C63FF), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Text(text = item.content, color = Color.LightGray, fontSize = 13.sp, maxLines = 2)
                
                Text(text = time, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
            }

            // Right Actions
            if (!selectionActive) {
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onInspectClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Inspecionar",
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notification: NotificationEntity,
    onBack: () -> Unit,
    onToggleMute: () -> Unit
) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    val appIcon = remember(notification.package_name) {
        try { pm.getApplicationIcon(notification.package_name) } catch (e: Exception) { null }
    }
    val appLabel = remember(notification.package_name) {
        try {
            val info = pm.getApplicationInfo(notification.package_name, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) { notification.package_name }
    }
    val timestamp = remember(notification.created_at) {
        val sdf = SimpleDateFormat("dd 'de' MMM. 'de' yyyy 'às' HH:mm", Locale.getDefault())
        "Recebida em ${sdf.format(Date(notification.created_at))}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Notificação", color = Color.White, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mais", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F17))
            )
        },
        containerColor = Color(0xFF0F0F17)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. App Header Card
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = Color(0xFF1A1A23),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF252535)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(Icons.Default.Android, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = appLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = " • ", color = Color.Gray)
                            Text(text = notification.category ?: "Notificação", color = Color.Gray, fontSize = 14.sp)
                        }
                        Text(text = timestamp, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Content
            DetailField(label = "Título", content = notification.title, isBold = true)
            notification.sub_text?.let {
                if (it.isNotBlank()) {
                    DetailField(label = "Subtexto", content = it)
                }
            }
            DetailField(label = "Mensagem", content = notification.content)

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Silence Switch Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1A1A23),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF252535)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Notificações silenciadas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Você não receberá mais notificações deste tipo.", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = notification.is_muted,
                        onCheckedChange = { onToggleMute() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFF5252),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF252535)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Information Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Informações", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoItem(icon = Icons.Default.Inventory2, label = "Pacote", value = notification.package_name)
            if (!notification.channel_id.isNullOrEmpty()) {
                InfoItem(icon = Icons.Default.CellTower, label = "Canal", value = notification.channel_id!!)
            }
            if (!notification.category.isNullOrEmpty()) {
                InfoItem(icon = Icons.Default.Folder, label = "Categoria", value = notification.category!!)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DetailField(label: String, content: String, isBold: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            color = Color.White,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp
        )
    }
}

@Composable
fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, color = Color.Gray, fontSize = 12.sp)
            Text(text = value, color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun BadgeBadge(text: String, bgColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color(0xFFA080A0),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F17)
@Composable
fun PreviewNotificationCard() {
    NotificationCard(
        item = NotificationEntity(
            id = 1,
            package_name = "com.instagram.android",
            title = "leandro_horizon: Wagner",
            content = "Enviou um reel para você",
            category = "msg",
            channel_id = "ig_direct",
            is_muted = false,
            created_at = System.currentTimeMillis()
        ),
        time = "11:49:41",
        isSelected = true,
        selectionActive = false,
        onToggleSelect = {},
        onClick = {},
        onMuteClick = {},
        onInspectClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F17)
@Composable
fun PreviewTabButtons() {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TabButton(
            label = "Ativas",
            count = 12,
            selected = true,
            icon = Icons.Default.FlashOn,
            onClick = {},
            modifier = Modifier.weight(1f)
        )
        TabButton(
            label = "Silenciadas",
            count = 7,
            selected = false,
            icon = Icons.Default.NotificationsOff,
            onClick = {},
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F17)
@Composable
fun PreviewFilterChips() {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChipSmall(
            label = "Todas",
            selected = false,
            onClick = {}
        )
        FilterChipSmall(
            label = "Social",
            selected = true,
            onClick = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterEditorDialog(
    filter: SavedFilterEntity?,
    installedApps: List<AppInfo>,
    onDismiss: () -> Unit,
    onSave: (String, List<String>, List<String>) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(filter?.name ?: "") }
    var selectedPkgs by remember { mutableStateOf(filter?.package_names?.split(",")?.filter { it.isNotBlank() } ?: emptyList()) }
    var keywordList by remember { mutableStateOf(filter?.keyword_list ?: emptyList()) }
    var tagInput by remember { mutableStateOf("") }
    var appSearch by remember { mutableStateOf("") }

    val filteredApps = remember(appSearch, installedApps) {
        if (appSearch.isBlank()) installedApps else installedApps.filter { it.name.contains(appSearch, ignoreCase = true) || it.packageName.contains(appSearch, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A23)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(if (filter == null) "Novo Filtro" else "Editar Filtro", style = MaterialTheme.typography.titleLarge, color = Color.White)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Preset") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Tags Section
                Text("Tags de busca", style = MaterialTheme.typography.titleSmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Adicionar tag...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onKeyEvent { 
                            if (it.key == Key.Enter && tagInput.isNotBlank()) {
                                val tag = tagInput.trim()
                                if (tag.isNotEmpty() && !keywordList.any { it.equals(tag, ignoreCase = true) }) {
                                    keywordList = keywordList + tag
                                }
                                tagInput = ""
                                true
                            } else false
                        },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (tagInput.isNotBlank()) {
                                val tag = tagInput.trim()
                                if (tag.isNotEmpty() && !keywordList.any { it.equals(tag, ignoreCase = true) }) {
                                    keywordList = keywordList + tag
                                }
                                tagInput = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF6C63FF))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    keywordList.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = {},
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp).combinedClickable {
                                        keywordList = keywordList - tag
                                    }
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = Color(0xFF252538),
                                selectedLabelColor = Color.White
                            ),
                            border = InputChipDefaults.inputChipBorder(
                                enabled = true,
                                selected = true,
                                selectedBorderColor = Color(0xFF6C63FF)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Selecionar Aplicativos (${selectedPkgs.size})", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                
                OutlinedTextField(
                    value = appSearch,
                    onValueChange = { appSearch = it },
                    label = { Text("Buscar app...") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true,
                    trailingIcon = { if (appSearch.isNotEmpty()) IconButton(onClick = { appSearch = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) } }
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
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6C63FF))
                            )
                            Column {
                                Text(app.name, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (filter != null) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excluir")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(name, selectedPkgs, keywordList) },
                            enabled = name.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                        ) { Text("Salvar") }
                    }
                }
            }
        }
    }
}
