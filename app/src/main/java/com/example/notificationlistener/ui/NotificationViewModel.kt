package com.example.notificationlistener.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificationlistener.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val db = AppDatabase.getDatabase(application)

    val logs = LogManager.logs
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterPackage = MutableStateFlow<String?>(null)
    val filterPackage = _filterPackage.asStateFlow()

    private val _showMutedOnly = MutableStateFlow(false)
    val showMutedOnly = _showMutedOnly.asStateFlow()

    private val _activePreset = MutableStateFlow<SavedFilterEntity?>(null)
    val activePreset = _activePreset.asStateFlow()

    private data class SearchParams(
        val query: String,
        val pkgs: List<String>,
        val hasFilter: Boolean,
        val muted: Boolean,
        val regex: String? = null
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pendingNotifications: Flow<List<NotificationEntity>> = combine(
        _searchQuery,
        _filterPackage,
        _showMutedOnly,
        _activePreset
    ) { query, pkg, mutedOnly, preset ->
        val effectivePkgs = if (preset != null) {
            preset.package_names?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        } else {
            pkg?.let { listOf(it) } ?: emptyList()
        }
        
        val hasPackageFilter = effectivePkgs.isNotEmpty()

        val regex = preset?.keyword_list?.let { list ->
            if (list.isNotEmpty()) {
                ".*(" + list.joinToString("|") { Regex.escape(it) } + ").*"
            } else null
        }
        
        SearchParams(query, effectivePkgs, hasPackageFilter, mutedOnly, regex)
    }.flatMapLatest { params ->
        db.notificationDao().searchNotifications(
            params.query, 
            params.pkgs, 
            params.hasFilter, 
            if (params.muted) true else null
        ).map { list ->
            val regexStr = params.regex
            if (regexStr != null) {
                val r = Regex(regexStr, RegexOption.IGNORE_CASE)
                list.filter { r.containsMatchIn("${it.title} ${it.content}") }
            } else {
                list
            }
        }
    }

    val distinctPackages: Flow<List<String>> = db.notificationDao().getDistinctPackagesFlow()
    val savedFilters: Flow<List<SavedFilterEntity>> = db.savedFilterDao().getAllFiltersFlow()

    val activeCount: Flow<Int> = db.notificationDao().getAllPendingFlow().map { list -> list.count { !it.is_muted } }
    val mutedCount: Flow<Int> = db.notificationDao().getAllPendingFlow().map { list -> list.count { it.is_muted } }

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    val muteRules: Flow<List<MuteRuleEntity>> = db.muteRuleDao().getAllRulesFlow()

    fun setSearchQuery(query: String) {
        _activePreset.value = null
        _searchQuery.value = query
    }

    fun setFilterPackage(pkg: String?) {
        _activePreset.value = null
        _filterPackage.value = pkg
    }

    fun toggleMutedOnly() {
        _showMutedOnly.value = !_showMutedOnly.value
    }

    fun setActivePreset(preset: SavedFilterEntity?) {
        _activePreset.value = preset
        if (preset == null) {
            _searchQuery.value = ""
            _filterPackage.value = null
        }
    }

    fun saveCurrentFilter(name: String, packageNames: List<String>?, keywordList: List<String> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            val filter = SavedFilterEntity(
                name = name,
                package_names = packageNames?.joinToString(","),
                keyword_list = keywordList
            )
            db.savedFilterDao().insert(filter)
            LogManager.addLog("Filtro '$name' salvo")
        }
    }

    fun updateSavedFilter(filter: SavedFilterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.savedFilterDao().insert(filter)
            LogManager.addLog("Filtro '${filter.name}' atualizado")
        }
    }

    fun deletePreset(preset: SavedFilterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_activePreset.value?.id == preset.id) {
                _activePreset.value = null
            }
            db.savedFilterDao().delete(preset)
            LogManager.addLog("Filtro '${preset.name}' removido")
        }
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedIds.value = current
    }

    fun selectAll(ids: List<Long>) {
        _selectedIds.value = ids.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun addMuteRule(pkg: String, category: String?, channelId: String?, keyword: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            db.muteRuleDao().insert(MuteRuleEntity(
                package_name = pkg, 
                category = category, 
                channel_id = channelId,
                text_keyword = if (keyword.isNullOrBlank()) null else keyword.trim()
            ))
            LogManager.addLog("Regra de silenciamento criada para [$pkg]")
        }
    }

    fun removeMuteRule(rule: MuteRuleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.muteRuleDao().delete(rule)
        }
    }

    fun addKeyword(word: String) {
        if (word.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            db.keywordDao().insert(KeywordEntity(word.trim().lowercase()))
        }
    }

    fun removeKeyword(word: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.keywordDao().deleteByWord(word)
        }
    }

    fun updateAppFilter(packageName: String, isAllowed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            db.appFilterDao().insert(AppFilterEntity(packageName, isAllowed))
        }
    }

    fun removeAppFilter(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.appFilterDao().deleteByPackage(packageName)
        }
    }

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.Default) {
            val pm = getApplication<Application>().packageManager
            val apps = try {
                // Using 0 as flag is safer/faster for just listing names and packages
                pm.getInstalledApplications(0)
                    .map {
                        AppInfo(
                            packageName = it.packageName,
                            name = it.loadLabel(pm).toString()
                        )
                    }.sortedBy { it.name }
            } catch (e: Exception) {
                emptyList()
            }
            _installedApps.value = apps
        }
    }

    fun getSyncUrl(): String = prefs.getString("sync_url", "") ?: ""

    fun setSyncUrl(url: String) {
        prefs.edit().putString("sync_url", url).apply()
    }

    fun deleteNotifications(ids: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            db.notificationDao().deleteByIds(ids)
            clearSelection()
            LogManager.addLog("${ids.size} notificações deletadas")
        }
    }

    fun syncNotifications(ids: List<Long>? = null) {
        val urlString = getSyncUrl()
        if (urlString.isEmpty()) {
            LogManager.addLog("Falha: URL de sincronização não configurada")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val dao = db.notificationDao()
            var totalSynced = 0
            
            if (ids != null) {
                val notifications = dao.getAllPendingFlow().first().filter { ids.contains(it.id) }
                if (notifications.isNotEmpty()) {
                    val success = sendBatch(urlString, notifications)
                    if (success) {
                        dao.deleteByIds(ids)
                        totalSynced = notifications.size
                        clearSelection()
                    } else {
                        LogManager.addLog("Falha ao sincronizar selecionados")
                    }
                }
            } else {
                while (true) {
                    val batch = dao.getNextBatch(100)
                    if (batch.isEmpty()) break

                    val success = sendBatch(urlString, batch)
                    if (success) {
                        dao.deleteByIds(batch.map { it.id })
                        totalSynced += batch.size
                    } else {
                        LogManager.addLog("Falha de rede ao sincronizar lote")
                        break
                    }
                }
            }
            
            if (totalSynced > 0) {
                LogManager.addLog("Sincronizados $totalSynced registros com sucesso")
            }
        }
    }

    private fun sendBatch(urlString: String, batch: List<NotificationEntity>): Boolean {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val dtos = batch.map { it.toDto() }
            val json = Json.encodeToString(dtos)
            conn.outputStream.use { it.write(json.toByteArray()) }

            val responseCode = conn.responseCode
            conn.disconnect()
            responseCode == 200
        } catch (e: Exception) {
            false
        }
    }
}

data class AppInfo(val packageName: String, val name: String)
