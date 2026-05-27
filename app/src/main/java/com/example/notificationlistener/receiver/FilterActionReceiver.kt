package com.example.notificationlistener.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.notificationlistener.data.AppDatabase
import com.example.notificationlistener.data.AppFilterEntity
import com.example.notificationlistener.data.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FilterActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("package_name") ?: return
        val isAllowed = intent.getBooleanExtra("is_allowed", false)
        val notificationId = intent.getIntExtra("notification_id", -1)

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            db.appFilterDao().insert(AppFilterEntity(packageName, isAllowed))
            
            val status = if (isAllowed) "Permitido" else "Bloqueado"
            LogManager.addLog("App [$packageName] definido como $status")
        }
    }
}
