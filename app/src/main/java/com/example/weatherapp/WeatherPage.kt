package com.example.weatherapp

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weatherapp.weatherApi.NetworkResponse
import com.example.weatherapp.weatherApi.WeatherModel


@Composable
fun WeatherPage(viewModel: WeatherViewModel) {
    val city = remember { mutableStateOf("") }

    val weatherResult = viewModel.weatherResult.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedTextField(
                value = city.value,
                onValueChange = { newValue ->
                    city.value = newValue
                },
                label = {
                    Text(text = "Search for any location")
                }
            )
            IconButton(onClick = {
                viewModel.getData(city.value)
            }) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            }
        }

        when (val result = weatherResult.value){
            is NetworkResponse.Error -> {
                Text(text = result.exception.message.toString())
            }
            NetworkResponse.Loading -> {

                CircularProgressIndicator()
            }
            is NetworkResponse.Success -> WeatherDetails(data = result.data)
            null -> {}
            }
        }
}

@Composable
fun WeatherDetails(data: WeatherModel) {
    Log.d("WeatherDetails", data.current.cloud.toString())
}
