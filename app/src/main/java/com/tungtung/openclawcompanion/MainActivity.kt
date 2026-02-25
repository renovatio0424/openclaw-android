package com.tungtung.openclawcompanion

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tungtung.openclawcompanion.data.ConnectionState
import com.tungtung.openclawcompanion.data.FilterConfigStore
import com.tungtung.openclawcompanion.data.ForwardedEvent
import com.tungtung.openclawcompanion.service.GatewayService
import com.tungtung.openclawcompanion.service.GatewayService.Companion.ACTION_EVENT_BROADCAST
import com.tungtung.openclawcompanion.service.GatewayService.Companion.EXTRA_EVENT_SOURCE
import com.tungtung.openclawcompanion.service.GatewayService.Companion.EXTRA_EVENT_TEXT
import com.tungtung.openclawcompanion.service.GatewayService.Companion.EXTRA_EVENT_TIMESTAMP
import com.tungtung.openclawcompanion.service.GatewayService.Companion.EXTRA_EVENT_TYPE
import com.tungtung.openclawcompanion.service.NotificationListener
import com.tungtung.openclawcompanion.ui.FilterScreen
import com.tungtung.openclawcompanion.ui.MainScreen
import com.tungtung.openclawcompanion.ui.SettingsScreen
import com.tungtung.openclawcompanion.ui.theme.OpenClawCompanionTheme
import com.tungtung.openclawcompanion.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val eventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_EVENT_BROADCAST) return
            val event = ForwardedEvent(
                timestamp = intent.getLongExtra(EXTRA_EVENT_TIMESTAMP, System.currentTimeMillis()),
                type = intent.getStringExtra(EXTRA_EVENT_TYPE).orEmpty(),
                source = intent.getStringExtra(EXTRA_EVENT_SOURCE).orEmpty(),
                text = intent.getStringExtra(EXTRA_EVENT_TEXT).orEmpty()
            )
            if (event.type.isNotEmpty()) {
                viewModel.addEvent(event)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        registerReceiver(eventReceiver, IntentFilter(ACTION_EVENT_BROADCAST))
        setContent {
            OpenClawCompanionRoot(viewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(eventReceiver)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun OpenClawCompanionRoot(viewModel: MainViewModel) {
    val navController = rememberNavController()
    var filterConfig by remember { mutableStateOf(FilterConfigStore.load()) }
    var gatewayPrefs by remember { mutableStateOf(FilterConfigStore.loadGatewayPrefs()) }

    val events by viewModel.events.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    val context = LocalContext.current
    var showListenerDialog by remember { mutableStateOf(!isNotificationListenerEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                showListenerDialog = !isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OpenClawCompanionTheme {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    events = events,
                    state = connectionState,
                    onToggleService = {
                        if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
                            GatewayService.stop(context)
                        } else {
                            GatewayService.start(context)
                        }
                    },
                    onOpenFilters = { navController.navigate("filter") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
            composable("filter") {
                FilterScreen(
                    initialConfig = filterConfig,
                    onSave = { config ->
                        FilterConfigStore.save(config)
                        filterConfig = config
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    initialPrefs = gatewayPrefs,
                    onSave = { prefs ->
                        FilterConfigStore.saveGatewayPrefs(prefs)
                        gatewayPrefs = prefs
                        GatewayService.restart(context)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (showListenerDialog) {
            AlertDialog(
                onDismissRequest = { showListenerDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) {
                        Text(text = "설정 열기")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showListenerDialog = false }) {
                        Text(text = "닫기")
                    }
                },
                title = { Text(text = "알림 접근 필요") },
                text = { Text(text = "알림을 전달하려면 시스템 설정에서 OpenClaw Companion에 알림 접근 권한을 허용해야 합니다.") }
            )
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val cn = ComponentName(context, NotificationListener::class.java)
    val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        ?: return false
    return listeners.split(":").any { it.equals(cn.flattenToString(), ignoreCase = true) }
}
