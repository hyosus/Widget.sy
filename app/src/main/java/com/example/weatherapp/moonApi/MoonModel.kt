package com.example.weatherapp.moonApi

data class MoonModel(
    val current_time: String,
    val date: String,
    val day_length: String,
    val location: Location,
    val moon_altitude: Double,
    val moon_angle: Double,
    val moon_azimuth: Double,
    val moon_distance: Double,
    val moon_illumination_percentage: String,
    val moon_parallactic_angle: Double,
    val moon_phase: String,
    val moon_status: String,
    val moonrise: String,
    val moonset: String,
    val solar_noon: String,
    val sun_altitude: Double,
    val sun_azimuth: Double,
    val sun_distance: Double,
    val sun_status: String,
    val sunrise: String,
    val sunset: String
)