package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.weatherApi.Constant
import com.example.weatherapp.weatherApi.NetworkResponse
import com.example.weatherapp.weatherApi.RetrofitInstance
import com.example.weatherapp.weatherApi.WeatherModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    private val weatherApi = RetrofitInstance.weatherApi
    private val _weatherResult = MutableLiveData<NetworkResponse<WeatherModel>>()
    val weatherResult: LiveData<NetworkResponse<WeatherModel>> = _weatherResult

    fun getData(city: String) {
        _weatherResult.value = NetworkResponse.Loading
        viewModelScope.launch {
            try {
                val response = weatherApi.getWeather(Constant.weatherApiKey, city)
                if (response.isSuccessful) {
                    response.body()?.let {
                        _weatherResult.value = NetworkResponse.Success(it)
                    }
                } else {
                    _weatherResult.value =
                        NetworkResponse.Error(Exception("Error in fetching data"))
                }
            } catch (e: Exception) {
                _weatherResult.value = NetworkResponse.Error(e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getWeatherByLocation(context: Context) {
        _weatherResult.value = NetworkResponse.Loading

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // Optional: update interval
            fastestInterval = 5000 // Optional: fastest update interval
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    viewModelScope.launch {
                        try {
                            val response = weatherApi.getWeather(
                                Constant.weatherApiKey,
                                "${location.latitude},${location.longitude}"
                            )

                            if (response.isSuccessful) {
                                response.body()?.let {
                                    _weatherResult.postValue(NetworkResponse.Success(it))
                                }
                            } else {
                                _weatherResult.postValue(
                                    NetworkResponse.Error(Exception("Error fetching location weather"))
                                )
                            }
                        } catch (e: Exception) {
                            _weatherResult.postValue(NetworkResponse.Error(e))
                        }
                    }

                    // Stop location updates after getting the first result
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
}