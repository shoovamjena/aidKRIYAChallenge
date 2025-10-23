package com.example.aidkriyachallenge.utils

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- Data classes to match the JSON response from the API ---
// We only need the fields that are useful to us.

data class DirectionsResponse(val routes: List<Route>)

data class Route(val overview_polyline: OverviewPolyline)

data class OverviewPolyline(val points: String)


// --- The Retrofit Interface ---

interface DirectionsApiService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String
    ): Response<DirectionsResponse>
}

// --- A singleton object to create the Retrofit instance ---

object RetrofitClient {
    private const val BASE_URL = "https://maps.googleapis.com/"

    val instance: DirectionsApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(DirectionsApiService::class.java)
    }
}