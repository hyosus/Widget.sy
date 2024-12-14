package com.example.weatherapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.moonApi.MoonViewModel
import com.example.weatherapp.ui.theme.WeatherAppTheme
import com.example.weatherapp.weatherApi.NetworkResponse

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val weatherViewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        window.statusBarColor = android.graphics.Color.BLACK

        setContent {
            WeatherAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WeatherPage(weatherViewModel)

                    // Moon
                    /*val moonViewModel = MoonViewModel()
                    moonViewModel.getMoonData("singapore")

                    moonViewModel.moonResult.observeForever { result ->
                        when (result) {
                            is NetworkResponse.Error -> Log.e("Moon", "Error: ${result.exception}")
                            NetworkResponse.Loading -> Log.d("Moon", "Loading...")
                            is NetworkResponse.Success -> Log.d("Moon", "Success: ${result.data.moon_phase}")
                        }
                    }*/
                }
            }
        }
    }
}

