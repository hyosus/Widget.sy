package com.example.widgetsy.moonApi

data class MoonModel(
    val location: Location,
    val astronomy: Astronomy
)

data class Astronomy(
    val date: String,
    val current_time: String,
    val sunrise: String,
    val sunset: String,
    val sun_status: String,
    val solar_noon: String,
    val day_length: String,
    val sun_altitude: Double,
    val sun_distance: Double,
    val sun_azimuth: Double,
    val moonrise: String,
    val moonset: String,
    val moon_status: String,
    val moon_altitude: Double,
    val moon_distance: Double,
    val moon_azimuth: Double,
    val moon_parallactic_angle: Double,
    val moon_phase: String,
    val moon_illumination_percentage: String,
    val moon_angle: Double
)