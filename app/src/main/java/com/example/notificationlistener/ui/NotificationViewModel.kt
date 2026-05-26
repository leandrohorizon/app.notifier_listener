package com.example.notificationlistener.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificationlistener.data.AppDatabase
import com.example.notificationlistener.data.LogManager
import com.example.notificationlistener.data.NotificationEntity
import com.example.notificationlistener.data.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val logs = LogManager.logs

    fun getSyncUrl(): String = prefs.getString("sync_url", "") ?: ""

    fun setSyncUrl(url: String) {
        prefs.edit().putString("sync_url", url).apply()
    }

    fun syncNotifications() {
        val urlString = getSyncUrl()
        if (urlString.isEmpty()) {
            LogManager.addLog("Falha: URL de sincronização não configurada")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(getApplication()).notificationDao()
            var totalSynced = 0

            try {
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
                if (totalSynced > 0) {
                    LogManager.addLog("Sincronizados $totalSynced registros com sucesso")
                } else {
                    LogManager.addLog("Nada para sincronizar")
                }
            } catch (e: Exception) {
                LogManager.addLog("Erro na sincronização: ${e.message}")
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
