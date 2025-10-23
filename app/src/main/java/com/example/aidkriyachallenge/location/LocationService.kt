package com.example.aidkriyachallenge.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.aidkriyachallenge.R
// Make sure this package name is correct for your project
import com.google.firebase.Firebase

import com.google.firebase.database.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    // A consistent tag for all logs from this service
    private val TAG = "LocationService"

    override fun onBind(intent: Intent?): IBinder? {
        return null // We are not using a bound service
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationClient(applicationContext)
        Log.d(TAG, "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent)
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start(intent: Intent) {
        val sessionId = intent.getStringExtra("sessionId")
        val currentUserId = intent.getStringExtra("currentUserId")

        if (sessionId == null || currentUserId == null) {
            Log.e(TAG, "Session ID or User ID is null. Stopping service.")
            stop()
            return
        }

        Log.d(TAG, "Service Starting for Session: $sessionId, User: $currentUserId")

        val notification = createNotification()

        locationClient.getLocationUpdates(10000L) // 10-second interval
            .catch { e -> Log.e(TAG, "LocationClient Error: ${e.message}") }
            .onEach { location ->
                Log.d(TAG, "New location received: Lat=${location.latitude}, Lng=${location.longitude}")

                val db = Firebase.database.reference
                val loc = mapOf(
                    "lat" to location.latitude,
                    "lng" to location.longitude,
                    "updatedAt" to com.google.firebase.database.ServerValue.TIMESTAMP
                )

                // Write to Firebase with listeners to see if it succeeds or fails
                db.child("sessions").child(sessionId).child("locations").child(currentUserId).setValue(loc)
                    .addOnSuccessListener {
                        Log.d(TAG, "Firebase write SUCCESSFUL!")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Firebase write FAILED: ${e.message}")
                    }
            }
            .launchIn(serviceScope)

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stop() {
        Log.d(TAG, "Service Stopping")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // This cancels all coroutines started in this scope
        Log.d(TAG, "Service Destroyed")
    }

    private fun createNotification(): android.app.Notification {
        val channelId = "location_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            "Live Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Session Active")
            .setContentText("Your location is being shared.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val NOTIFICATION_ID = 1
    }
}