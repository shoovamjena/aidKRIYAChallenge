package com.example.aidkriyachallenge.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class LocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) {

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(interval: Long): Flow<Location> {
        // callbackFlow is a great way to convert callback-based APIs into a Flow
        return callbackFlow {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let { location ->
                        // When a new location is received, send it to the Flow
                        launch { send(location) }
                    }
                }
            }

            // Start location updates with listeners to log success or failure of the request itself
            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            ).addOnSuccessListener {
                Log.d("LocationClient", "Successfully requested location updates.")
            }.addOnFailureListener { e ->
                Log.e("LocationClient", "Failed to request location updates: ${e.message}")
            }

            // This is the cleanup block. It's called when the Flow is cancelled.
            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}