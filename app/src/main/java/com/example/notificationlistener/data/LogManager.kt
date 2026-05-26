package com.example.notificationlistener.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun addLog(message: String) {
        val timestamp = dateFormat.format(Date())
        val logLine = "[$timestamp] $message"
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(logLine) // Adiciona no final (ordem cronológica)
        if (currentLogs.size > 200) {
            currentLogs.removeAt(0)
        }
        _logs.value = currentLogs
    }
}
