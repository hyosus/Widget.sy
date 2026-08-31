package com.example.widgetsy

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.widgetsy.musicWidget.MediaListenerService
import com.example.widgetsy.musicWidget.normal.MusicWidgetReceiver
import com.example.widgetsy.ui.theme.WeatherAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        CoroutineScope(Dispatchers.IO).launch {
            GlanceAppWidgetManager(applicationContext)
                .setWidgetPreviews(MusicWidgetReceiver::class)  // your receiver class
        }

        setContent {
            WeatherAppTheme {
                Scaffold()
                { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                        Text(
                            text = "Nothing implemented here yet 😶"
                        )
                    }
                }

            }
        }

        requestIgnoreBatteryOptimizations(this)
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(packageName) == true
    }

    override fun onStart() {
        super.onStart()

        if (!isNotificationServiceEnabled()) {
            Log.d("MainActivity", "Notification listener NOT enabled — opening settings")
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else if (!MediaListenerService.isConnected) {
            Log.d("MainActivity", "Permission granted but not connected — requesting rebind")
            val componentName = ComponentName(this, MediaListenerService::class.java)
            NotificationListenerService.requestRebind(componentName)
        }
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent()
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager

        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            Log.d("MainActivity", "Requesting ignore battery optimizations")
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:" + context.packageName)
            context.startActivity(intent)
        }
    }
}