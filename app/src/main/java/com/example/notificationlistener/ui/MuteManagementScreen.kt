package com.example.notificationlistener.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.notificationlistener.data.MuteRuleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuteManagementScreen(viewModel: NotificationViewModel) {
    val rules by viewModel.muteRules.collectAsState(initial = emptyList())
    val installedApps by viewModel.installedApps.collectAsState()
    
    var ruleToEdit by remember { mutableStateOf<MuteRuleEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF0F0F17),
        topBar = {
            TopAppBar(
                title = { Text("Regras de Silenciamento", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F17))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    ruleToEdit = null
                    showEditor = true
                },
                containerColor = Color(0xFF6C63FF),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Regra")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text(
                "Notificações que batem com estas regras são canceladas automaticamente.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (rules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma regra configurada", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(rules) { rule ->
                        MuteRuleCard(
                            rule = rule,
                            appName = installedApps.find { it.packageName == rule.package_name }?.name ?: rule.package_name ?: "Global",
                            onEdit = {
                                ruleToEdit = rule
                                showEditor = true
                            },
                            onDelete = { viewModel.removeMuteRule(rule) }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        MuteRuleEditorDialog(
            rule = ruleToEdit,
            installedApps = installedApps,
            onDismiss = { showEditor = false },
            onSave = { rule ->
                if (rule.id == 0) viewModel.addMuteRule(rule.package_name, rule.keywords_to_mute ?: emptyList(), rule.keywords_to_bypass ?: emptyList())
                else viewModel.updateMuteRule(rule)
                showEditor = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MuteRuleCard(rule: MuteRuleEntity, appName: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A23),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF252535))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(appName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(rule.package_name ?: "Todos os aplicativos", color = Color.Gray, fontSize = 12.sp)
                
                if (!rule.keywords_to_mute.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Muta se contiver:", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(rule.keywords_to_mute.joinToString(", "), color = Color.LightGray, fontSize = 13.sp)
                }

                if (!rule.keywords_to_bypass.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Exceto se contiver:", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(rule.keywords_to_bypass.joinToString(", "), color = Color.LightGray, fontSize = 13.sp)
                }
                
                if (rule.keywords_to_mute.isNullOrEmpty() && rule.keywords_to_bypass.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Silencia tudo deste app", color = Color(0xFFFF5252), fontSize = 11.sp)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252).copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MuteRuleEditorDialog(
    rule: MuteRuleEntity?,
    installedApps: List<AppInfo>,
    onDismiss: () -> Unit,
    onSave: (MuteRuleEntity) -> Unit
) {
    var selectedPkg by remember { mutableStateOf(rule?.package_name) }
    var muteTags by remember { mutableStateOf(rule?.keywords_to_mute ?: emptyList()) }
    var bypassTags by remember { mutableStateOf(rule?.keywords_to_bypass ?: emptyList()) }
    
    var muteInput by remember { mutableStateOf("") }
    var bypassInput by remember { mutableStateOf("") }
    var appSearch by remember { mutableStateOf("") }
    var showAppPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A23)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(if (rule == null) "Nova Regra" else "Editar Regra", style = MaterialTheme.typography.titleLarge, color = Color.White)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // App Selector
                Surface(
                    onClick = { showAppPicker = !showAppPicker },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF252535)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Aplicativo Alvo", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                if (selectedPkg == null) "Todos os Aplicativos (Global)" 
                                else installedApps.find { it.packageName == selectedPkg }?.name ?: selectedPkg!!,
                                color = Color.White
                            )
                        }
                    }
                }

                if (showAppPicker) {
                    Card(modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF252535))) {
                        Column {
                            OutlinedTextField(
                                value = appSearch,
                                onValueChange = { appSearch = it },
                                placeholder = { Text("Buscar app...") },
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                singleLine = true
                            )
                            LazyColumn {
                                item {
                                    ListItem(
                                        headlineContent = { Text("Global (Todos os apps)", color = Color.White) },
                                        modifier = Modifier.combinedClickable { selectedPkg = null; showAppPicker = false },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                                items(installedApps.filter { it.name.contains(appSearch, true) || it.packageName.contains(appSearch, true) }) { app ->
                                    ListItem(
                                        headlineContent = { Text(app.name, color = Color.White) },
                                        supportingContent = { Text(app.packageName, color = Color.Gray) },
                                        modifier = Modifier.combinedClickable { selectedPkg = app.packageName; showAppPicker = false },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mute Tags
                TagInputSection(
                    title = "Palavras-chave para Mutar (Gatilhos)",
                    input = muteInput,
                    onInputChange = { muteInput = it },
                    tags = muteTags,
                    onTagsChange = { muteTags = it },
                    color = Color(0xFFFF5252)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bypass Tags
                TagInputSection(
                    title = "Exceto se contiver (Exceções)",
                    input = bypassInput,
                    onInputChange = { bypassInput = it },
                    tags = bypassTags,
                    onTagsChange = { bypassTags = it },
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            onSave(MuteRuleEntity(
                                id = rule?.id ?: 0,
                                package_name = selectedPkg,
                                keywords_to_mute = muteTags,
                                keywords_to_bypass = bypassTags
                            ))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                    ) { Text("Salvar") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TagInputSection(
    title: String,
    input: String,
    onInputChange: (String) -> Unit,
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    color: Color
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, color = color)
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth().onKeyEvent {
                if (it.key == Key.Enter && input.isNotBlank()) {
                    if (!tags.contains(input.trim())) onTagsChange(tags + input.trim())
                    onInputChange("")
                    true
                } else false
            },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = {
                    if (input.isNotBlank()) {
                        if (!tags.contains(input.trim())) onTagsChange(tags + input.trim())
                        onInputChange("")
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = color)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = {},
                    label = { Text(tag, fontSize = 12.sp) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp).combinedClickable {
                            onTagsChange(tags - tag)
                        })
                    },
                    colors = InputChipDefaults.inputChipColors(selectedContainerColor = color.copy(alpha = 0.2f), selectedLabelColor = Color.White),
                    border = InputChipDefaults.inputChipBorder(enabled = true, selected = true, selectedBorderColor = color)
                )
            }
        }
    }
}
