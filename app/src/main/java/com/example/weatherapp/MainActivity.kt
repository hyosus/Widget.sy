package com.example.weatherapp

import android.content.Intent
import android.os.Bundle
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

        // Request location permission
//        val intent = Intent(this, PermissionRequestActivity::class.java)
//        startActivity(intent)

    }
}

