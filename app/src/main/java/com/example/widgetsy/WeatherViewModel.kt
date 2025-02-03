package com.example.widgetsy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.widgetsy.weatherApi.Constant
import com.example.widgetsy.weatherApi.NetworkResponse
import com.example.widgetsy.weatherApi.RetrofitInstance
import com.example.widgetsy.weatherApi.WeatherModel
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    private val weatherApi = RetrofitInstance.weatherApi
    private val _weatherResult = MutableLiveData<NetworkResponse<WeatherModel>>()
    val weatherResult: LiveData<NetworkResponse<WeatherModel>> = _weatherResult

    fun getData(location: String) {
        _weatherResult.value = NetworkResponse.Loading
        viewModelScope.launch {
            try {
                val response = weatherApi.getWeather(Constant.weatherApiKey, location)
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
}