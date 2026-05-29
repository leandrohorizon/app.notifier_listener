package com.example.notificationlistener

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationlistener.service.ForegroundService
import com.example.notificationlistener.ui.LogScreen
import com.example.notificationlistener.ui.MainScreen
import com.example.notificationlistener.ui.MuteManagementScreen
import com.example.notificationlistener.ui.NotificationViewModel
import com.example.notificationlistener.ui.SettingsScreen
import com.example.notificationlistener.ui.theme.NotificationListenerTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            ForegroundService.startService(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        checkPermissions()

        setContent {
            NotificationListenerTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                val viewModel: NotificationViewModel = viewModel()
                val viewingNotification by viewModel.viewingNotification.collectAsState()

                Scaffold(
                    bottomBar = {
                        if (viewingNotification == null) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Pendentes") },
                                    label = { Text("Pendentes") },
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.NotificationsOff, contentDescription = "Silenciados") },
                                    label = { Text("Mute") },
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
                                    label = { Text("Logs") },
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Config") },
                                    label = { Text("Config") },
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> MainScreen(viewModel)
                            1 -> MuteManagementScreen(viewModel)
                            2 -> LogScreen(viewModel)
                            3 -> SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ForegroundService.startService(this)
            }
        } else {
            ForegroundService.startService(this)
        }
    }
}
