package com.example.notificationlistener.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileLogger {
    private const val FILE_NAME = "app_errors.txt"
    private const val MAX_SIZE = 1024 * 1024 // 1MB

    /**
     * Grava um erro estruturado no arquivo local "app_errors.txt".
     * Se o arquivo ultrapassar 1MB, ele é resetado.
     */
    fun writeError(
        context: Context,
        className: String,
        methodName: String,
        operation: String,
        contextMap: Map<String, Any?>,
        exception: Throwable?
    ) {
        try {
            val file = File(context.filesDir, FILE_NAME)

            // Trava de segurança: se ultrapassar 1MB, limpa o arquivo
            if (file.exists() && file.length() > MAX_SIZE) {
                file.delete()
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestamp = sdf.format(Date())

            // Mascarar dados sensíveis no contexto
            val maskedContext = contextMap.mapValues { (key, value) ->
                if (isSensitive(key)) "********" else value
            }

            val contextString = maskedContext.entries.joinToString("\n") { "  ${it.key}: ${it.value}" }
            
            val logEntry = """
                [$timestamp] - [ERROR]
                [$className].[$methodName]
                Operação: $operation
                Contexto:
                $contextString
                Resultado: FALHA
                Exceção: ${exception?.javaClass?.name}: ${exception?.localizedMessage}
                StackTrace:
                ${exception?.stackTraceToString() ?: "Nenhum StackTrace disponível"}
                ----------------------------------------------------------------------
                
            """.trimIndent() + "\n"

            file.appendText(logEntry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isSensitive(key: String): Boolean {
        val sensitiveKeywords = listOf("token", "password", "senha", "secret", "auth", "credential", "email")
        return sensitiveKeywords.any { key.contains(it, ignoreCase = true) }
    }
}
