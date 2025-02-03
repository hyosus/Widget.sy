// File: app/src/main/java/com/example/weatherapp/MoonViewModel.kt
package com.example.widgetsy.moonApi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.widgetsy.weatherApi.Constant
import com.example.widgetsy.weatherApi.NetworkResponse
import kotlinx.coroutines.launch

class MoonViewModel : ViewModel() {
    private val _moonResult = MutableLiveData<NetworkResponse<MoonModel>>()
    val moonResult: LiveData<NetworkResponse<MoonModel>> = _moonResult

    fun getMoonData(location: String) {
        _moonResult.value = NetworkResponse.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.moonApi.getMoon(Constant.moonApiKey, location)
                if (response.isSuccessful) {
                    val moonData = response.body()
                    moonData?.let {
                        _moonResult.value = NetworkResponse.Success(it)
                    } ?: run {
                        _moonResult.value = NetworkResponse.Error(Exception("No moon data found"))
                    }
                } else {
                    _moonResult.value = NetworkResponse.Error(
                        Exception("API error: ${response.code()} - ${response.message()}")
                    )
                }
            } catch (e: Exception) {
                _moonResult.value = NetworkResponse.Error(e)
            }
        }
    }
}