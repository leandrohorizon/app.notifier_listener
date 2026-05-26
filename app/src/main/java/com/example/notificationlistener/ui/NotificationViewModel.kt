package com.example.notificationlistener.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pendingNotifications: Flow<List<NotificationEntity>> = combine(
        _searchQuery,
        _filterPackage
    ) { query, pkg ->
        query to pkg
    }.flatMapLatest { (query, pkg) ->
        db.notificationDao().searchNotifications(query, pkg)
    }

    val distinctPackages: Flow<List<String>> = db.notificationDao().getDistinctPackagesFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterPackage(pkg: String?) {
        _filterPackage.value = pkg
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
            
            // Se IDs forem fornecidos, sincroniza apenas eles. Caso contrário, sincroniza tudo via paginação.
            if (ids != null) {
                // Sincronização de selecionados (simplificada para o exemplo)
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
                // Comportamento padrão: Tudo
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
