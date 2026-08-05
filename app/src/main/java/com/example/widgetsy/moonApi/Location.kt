package com.example.widgetsy.moonApi

data class Location(
    val location_string: String,
    val country_name: String,
    val state_prov: String,
    val city: String,
    val locality: String,
    val latitude: String,
    val longitude: String,
    val elevation: String
)