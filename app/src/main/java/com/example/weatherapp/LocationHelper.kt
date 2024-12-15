package com.example.weatherapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.io.IOException
import java.util.Locale

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getLocation(
        onLocationReceived: (latitude: Double, longitude: Double) -> Unit,
        onError: ((error: String) -> Unit)? = null
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {

            // Attempt to retrieve the last known location first
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Use the last known location if available
                    onLocationReceived(location.latitude, location.longitude)
                } else {
                    // Fall back to requesting location updates
                    requestLocationUpdates(onLocationReceived, onError)
                }
            }.addOnFailureListener {
                // Log and notify on failure to retrieve last known location
                Log.e("LocationHelper", "Failed to get last location", it)
                onError?.invoke("Failed to retrieve last known location.")
            }
        } else {
            Log.e("LocationHelper", "Location permission not granted")
            onError?.invoke("Location permission not granted. Please grant permissions.")
        }
    }

    fun getCityAndCountry(context: Context, latitude: Double, longitude: Double, onResult: (String?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val geocoder = Geocoder(context, Locale.getDefault())
            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val city = address.locality
                        val country = address.countryName
                        onResult("$city, $country")
                        Log.d("getCityAndCountry", "City: $city, Country: $country")
                    } else {
                        onResult(null)
                        Log.d("getCityAndCountry", "Error")
                    }
                }

                override fun onError(errorMessage: String?) {
                    super.onError(errorMessage)
                }

            })
        }
        else{
            // Fallback for lower API levels
            val geocoder = Geocoder(context, Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val city = address.locality
                    val country = address.countryName
                    onResult("$city, $country")
                }
            } catch (e: IOException) {
                Log.e("Geocoder", "Geocoding error", e)
            }
        }
    }

        private fun requestLocationUpdates(
            onLocationReceived: (latitude: Double, longitude: Double) -> Unit,
            onError: ((error: String) -> Unit)?
        ) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 10000
                fastestInterval = 1000
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        onLocationReceived(location.latitude, location.longitude)
                        fusedLocationClient.removeLocationUpdates(this)
                    } ?: run {
                        onError?.invoke("Location is null. Unable to retrieve location.")
                    }
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    if (!locationAvailability.isLocationAvailable) {
                        onError?.invoke("Location services are unavailable.")
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Log.e("LocationHelper", "Location permission revoked or restricted", e)
                onError?.invoke("Location permission revoked or restricted.")
            }
        }

    }