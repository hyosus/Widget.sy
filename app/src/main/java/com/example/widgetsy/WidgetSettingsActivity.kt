package com.example.widgetsy

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.widgetsy.ui.theme.WeatherAppTheme

class WidgetSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If the appWidgetId is invalid, cancel the configuration
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        else {
            setContent {
                WeatherAppTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Setting(onSave = { location ->
                            completeWidgetConfiguration(appWidgetId)
                        })
                    }
                }
            }
        }
    }

    /**
     * Completes the widget configuration by sending the result to the system.
     */
    private fun completeWidgetConfiguration(appWidgetId: Int) {
        val context = this

        // Broadcast an update intent for all widgets
        val updateIntent = Intent(context, CloudWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        context.sendBroadcast(updateIntent)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)

        // Finish the configuration activity
        finish()
    }

    private fun checkLocationPermission() {
        val context = this
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun saveLocation(context: Context, location: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("WeatherAppPrefs",
            MODE_PRIVATE
        )
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("location", location)
        editor.apply()
    }

    private fun getLocation(context: Context): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("WeatherAppPrefs",
            MODE_PRIVATE
        )
        return sharedPreferences.getString("location", null)
    }

    private fun getGPSLocation(context: Context, onLocationReceived: (String?) -> Unit) {
        val locationHelper = LocationHelper(context)

        locationHelper.getLocation(
            this,
            onLocationReceived = { latitude, longitude ->
                Log.d("getGPSLocation", "Lat: $latitude, Lon: $longitude")

                locationHelper.getCityAndCountry(context, latitude, longitude) { location ->
                    if (location != null) {
                        saveLocation(context, location)
                        onLocationReceived(location) // Pass the resolved location back
                        Log.d("getGPSLocation", "Resolved location: $location")
                    } else {
                        Log.e("getGPSLocation", "Location resolution failed")
                    }
                }
            },
            onError = { error ->
                onLocationReceived(null) // Pass null to indicate failure
                Log.e("getGPSLocation", "Error retrieving location: $error")
            }
        )
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    @Composable
    fun Setting(onSave: (String) -> Unit) {
        val context = LocalContext.current
        val savedLocation = getLocation(context) ?: ""
        val locationTxt = remember { mutableStateOf(savedLocation) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header()
            LocationInputField(locationTxt)
            SaveButton(locationTxt, onSave)
            SetAutomaticallyButton(locationTxt)
        }
    }

    @Composable
    fun Header() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Widget Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
    }

    @Composable
    fun LocationInputField(locationTxt: MutableState<String>) {
        Row(
            modifier = Modifier.padding(8.dp).padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = locationTxt.value,
                onValueChange = { locationTxt.value = it },
                label = { Text(text = "Search for any location") },
                leadingIcon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Location") }
            )
        }
    }

    @Composable
    fun SaveButton(locationTxt: MutableState<String>, onSave: (String) -> Unit) {
        val context = LocalContext.current
        Button(
            onClick = {
                if (locationTxt.value.isEmpty()) {
                    Toast.makeText(context, "Location cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    onSave(locationTxt.value)
                    saveLocation(context, locationTxt.value)
                }
            },
            modifier = Modifier.padding(8.dp).fillMaxWidth()
        ) {
            Text(text = "Save")
        }
    }

    @Composable
    fun SetAutomaticallyButton(locationTxt: MutableState<String>) {
        val context = LocalContext.current
        FilledTonalButton(
            onClick = {
                getGPSLocation(context) { location ->
                    locationTxt.value = location ?: "Failed to get location"
                }
            },
            modifier = Modifier.padding(8.dp).fillMaxWidth()
        ) {
            Text(text = "Set Automatically")
        }
    }

    @Preview
    @Composable
    fun SettingPreview() {
        WeatherAppTheme {
            Setting(onSave = {})
        }
    }
}






