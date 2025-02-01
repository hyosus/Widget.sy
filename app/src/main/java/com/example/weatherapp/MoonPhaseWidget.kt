package com.example.weatherapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.weatherapp.moonApi.MoonViewModel
import com.example.weatherapp.weatherApi.NetworkResponse
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Implementation of App Widget functionality.
 */
class MoonPhaseWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, MoonPhaseWidget::class.java)
                )
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.moon_phase_widget)

            // Create an intent to launch the desired activity (e.g., MainActivity)
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0, // Request code
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Set the PendingIntent on a clickable widget element (e.g., the root layout or a button)
            views.setOnClickPendingIntent(R.id.root, pendingIntent)

            // Create ViewModel directly
            val viewModel = MoonViewModel()

            val location = getLocation(context)
            Log.d("MoonLocation", location)
            viewModel.getMoonData(location)

            viewModel.moonResult.observeForever { result ->
                when (result) {
                    is NetworkResponse.Error -> {
                        Log.d("Moon", "Error: ${result.exception}")
                    }
                    NetworkResponse.Loading -> {
                        Log.d("Moon", "Loading...")
                    }
                    is NetworkResponse.Success -> {
                        val moonData = result.data

                        val absIllumination = abs(moonData.moon_illumination_percentage.toDouble())
                        // Moon phase
                        views.setImageViewResource(
                            R.id.moonPhaseImg,
                            getMoonPhaseImage(context, absIllumination, moonData.moon_phase))

                        Log.d("Moon", "FUCK YOU ${
                            getMoonPhaseImage(
                                context,
                                absIllumination,
                                moonData.moon_phase
                            )
                        }")

                        val formattedMoonPhase = moonData.moon_phase
                            .lowercase()
                            .split("_")
                            .joinToString(" ") { it.replaceFirstChar { it.uppercaseChar() } }
                        views.setTextViewText(R.id.moonPhaseTxt, formattedMoonPhase)

                        // Moon illumination percentage
                        views.setTextViewText(
                            R.id.illuminationTxt,
                            "${absIllumination}%"
                        )

                        // Moonrise & Moonset
                        val moonrise = convertTimeTo12HourFormat(moonData.moonrise)
                        views.setTextViewText(R.id.moonriseTxt, moonrise)

                        val moonset = convertTimeTo12HourFormat(moonData.moonset)
                        views.setTextViewText(R.id.moonsetTxt, moonset)
                    }
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun getLocation(context: Context): String {
            val prefs = context.getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
            return prefs.getString("location", "") ?: ""
        }

        fun getMoonPhaseImage(context: Context, illumination: Double, moonPhase: String): Int {
            val imageIndex = (illumination / 10).roundToInt() * 10 // Round to nearest 10
            Log.d("Moon", "Image Index: $imageIndex")

            if (imageIndex == 0) {
                return context.resources.getIdentifier(
                    "new_moon",
                    "drawable",
                    context.packageName
                )
            }
            else if (imageIndex == 100 || moonPhase == "FULL_MOON") {
                return context.resources.getIdentifier(
                    "full_moon",
                    "drawable",
                    context.packageName
                )
            }
            else if (moonPhase.contains("WAXING")) {
                return context.resources.getIdentifier(
                    "waxing$imageIndex",
                    "drawable",
                    context.packageName
                )
            }
            else if (moonPhase.contains("WANING")) {
                return context.resources.getIdentifier(
                    "waning$imageIndex",
                    "drawable",
                    context.packageName
                )
            }
            else {
                return context.resources.getIdentifier(
                    "waning$imageIndex",
                    "drawable",
                    context.packageName
                )
            }

        }

        private fun convertTimeTo12HourFormat(time24: String): String {
            val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(time24)
            return date?.let { outputFormat.format(it) } ?: "Invalid time"
        }
    }
}

