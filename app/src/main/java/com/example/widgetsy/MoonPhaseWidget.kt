package com.example.widgetsy

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.widgetsy.moonApi.MoonViewModel
import com.example.widgetsy.weatherApi.NetworkResponse
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
                        try {
                            val moonData = result.data
                            val astronomy = moonData.astronomy

                            Log.d("Moon Data", "Moon Data: ${moonData.astronomy}")
                            Log.d("Moon Phase", "Moon phase: ${astronomy.moon_phase}")

                            val absIllumination = abs(astronomy.moon_illumination_percentage.toDouble())
                            val roundedIllumination = absIllumination.roundToInt()

                            // Moon phase
                            views.setImageViewResource(
                                R.id.moonPhaseImg,
                                getMoonPhaseImage(context, absIllumination, astronomy.moon_phase))

                            val formattedMoonPhase = when (astronomy.moon_phase) {
                                "FIRST_QUARTER" -> "Waxing Gibbous"
                                "LAST_QUARTER" -> "Waning Gibbous"
                                else -> astronomy.moon_phase
                                    .lowercase()
                                    .split("_")
                                    .joinToString(" ") { it.replaceFirstChar { it.uppercaseChar() } }
                            }
                            views.setTextViewText(R.id.moonPhaseTxt, formattedMoonPhase)

                            Log.d("Formatted Moon details", "Formatted Moon details: $formattedMoonPhase, Illumination: $roundedIllumination%")

                            // Moon illumination percentage
                            views.setTextViewText(
                                R.id.illuminationTxt,
                                "$roundedIllumination%"
                            )

                            // Moonrise & Moonset
                            val moonrise = convertTimeTo12HourFormat(astronomy.moonrise)
                            views.setTextViewText(R.id.moonriseTxt, moonrise)

                            val moonset = convertTimeTo12HourFormat(astronomy.moonset)
                            views.setTextViewText(R.id.moonsetTxt, moonset)
                        } catch (e: Exception) {
                            Log.e("Moon", "Failed to render widget from data: $e")
                        }
                    }
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun getLocation(context: Context): String {
            val prefs = context.getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
            return prefs.getString("location", "") ?: ""
        }

        /**
         * Picks a drawable based on illumination percentage (rounded to the nearest
         * 10) plus whether the moon is waxing or waning.
         *
         * NEW_MOON and FULL_MOON get their own dedicated images. Everything else
         * falls into a waxing or waning bucket -- including the quarters, which
         * don't get a dedicated image: FIRST_QUARTER counts as waxing,
         * LAST_QUARTER counts as waning.
         *
         * Expects drawables named: new_moon, full_moon, waxing10..waxing90,
         * waning10..waning90 (step of 10).
         */
        fun getMoonPhaseImage(context: Context, illumination: Double, moonPhase: String): Int {
            val imageIndex = (illumination / 10).roundToInt() * 10 // round to nearest 10
            Log.d("Moon", "Image Index: $imageIndex")

            val drawableName = when {
                imageIndex <= 0 -> "new_moon"
                imageIndex >= 100 -> "full_moon"
                moonPhase.contains("WAXING") || moonPhase == "FIRST_QUARTER" -> "waxing$imageIndex"
                moonPhase.contains("WANING") || moonPhase == "LAST_QUARTER" -> "waning$imageIndex"
                else -> {
                    Log.d("Moon", "Unrecognized moon phase: $moonPhase")
                    "full_moon"
                }
            }

            return context.resources.getIdentifier(
                drawableName,
                "drawable",
                context.packageName
            )
        }

        private fun convertTimeTo12HourFormat(time24: String): String {
            // API returns "-:-" when the moon doesn't rise/set on a given day
            // (e.g. near the poles, or certain phases) -- not a real time to parse.
            if (time24 == "-:-") return "-"

            return try {
                val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val date = inputFormat.parse(time24)
                date?.let { outputFormat.format(it) } ?: "-"
            } catch (e: Exception) {
                Log.d("Moon", "Failed to parse time: $time24")
                "-"
            }
        }
    }
}