package com.example.weatherapp.moonApi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.weatherApi.Constant
import com.example.weatherapp.weatherApi.NetworkResponse
import kotlinx.coroutines.launch

class MoonViewModel : ViewModel() {
    private val _moonResult = MutableLiveData<NetworkResponse<MoonModel>>()
    val moonResult: LiveData<NetworkResponse<MoonModel>> = _moonResult

    fun getMoonData(location: String) {
        // Set loading state before making the API call
        _moonResult.value = NetworkResponse.Loading

        viewModelScope.launch {
            try {
                val response = RetrofitInstance.moonApi.getMoon(Constant.moonApiKey, location)

                if (response.isSuccessful) {
                    val moonData = response.body()

                    // Check if moon data is not null
                    moonData?.let {
                        _moonResult.value = NetworkResponse.Success(it)
                    } ?: run {
                        _moonResult.value = NetworkResponse.Error(Exception("No moon data found"))
                    }
                } else {
                    // Handle unsuccessful response
                    _moonResult.value = NetworkResponse.Error(
                        Exception("API error: ${response.code()} - ${response.message()}")
                    )
                }
            } catch (e: Exception) {
                // Catch and log any unexpected errors
                _moonResult.value = NetworkResponse.Error(e)
            }
        }
    }
}