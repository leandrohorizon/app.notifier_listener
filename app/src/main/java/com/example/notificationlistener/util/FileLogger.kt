package com.example.notificationlistener.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileLogger {
    private const val FILE_NAME = "app_errors.txt"
    private const val MAX_SIZE = 1024 * 1024 // 1MB

    /**
     * Grava um registro de erro no arquivo local "app_errors.txt".
     * Se o arquivo ultrapassar 1MB, ele é resetado.
     */
    fun writeError(context: Context, tag: String, message: String) {
        try {
            val file = File(context.filesDir, FILE_NAME)

            // Trava de segurança: se ultrapassar 1MB, limpa o arquivo
            if (file.exists() && file.length() > MAX_SIZE) {
                file.delete()
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestamp = sdf.format(Date())
            val logEntry = "$timestamp [$tag] $message\n"

            file.appendText(logEntry)
        } catch (e: Exception) {
            // Em caso de erro na escrita do log, apenas imprime no Logcat para evitar loop infinito ou crash
            e.printStackTrace()
        }
    }
}
