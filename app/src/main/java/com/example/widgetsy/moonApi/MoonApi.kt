package com.example.widgetsy.moonApi

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MoonApi {
    @GET("/astronomy")
    suspend fun getMoon(
        @Query("apiKey") apiKey: String,
        @Query("location") location: String
    ): Response<MoonModel>
}