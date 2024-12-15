package com.example.weatherapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.moonApi.Location

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val weatherViewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        window.statusBarColor = android.graphics.Color.BLACK

        requestIgnoreBatteryOptimizations(this)
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

