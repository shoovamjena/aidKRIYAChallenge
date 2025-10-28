package com.example.aidkriyachallenge.view.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.aidkriyachallenge.location.LocationClient
import com.example.aidkriyachallenge.location.LocationService

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*


import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver


import kotlinx.coroutines.launch


import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material3.*
import androidx.compose.ui.draw.clip

import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.aidkriyachallenge.viewModel.MainViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory


@Composable
fun MapScreen(viewModel: MainViewModel, navController: NavHostController) {
    // --- STATE COLLECTION ---
    val locations by viewModel.locations.collectAsState()
    val sessionId by viewModel.sessionId.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val activeRequest by viewModel.activeRequest.collectAsState()
    val sessionStatus by viewModel.sessionStatus.collectAsState()
    val isCloseEnough by viewModel.isCloseEnoughToStartWalk.collectAsState()
    val context = LocalContext.current
    val isWalker = viewModel.currentUserId == activeRequest?.walkerId

    // --- PERMISSION HANDLING ---
    var hasFineLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasBackgroundLocationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else { true }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasFineLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            hasBackgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false
            } else { true }
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasFineLocationPermission) {
            val permissionsToRequest = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // --- MAP AND CAMERA SETUP ---
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(19.3142, 84.7941), 12f) // Brahmapur
    }
    val mapProperties by remember(hasFineLocationPermission) {
        mutableStateOf(MapProperties(isMyLocationEnabled = hasFineLocationPermission))
    }
    var latestUserLocation by remember { mutableStateOf<LatLng?>(null) }
    var autoFollow by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // --- UI ---
    val allPermissionsGranted = hasFineLocationPermission && hasBackgroundLocationPermission

    when {
        // Case 1: All permissions granted, show the map.
        allPermissionsGranted -> {
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            val locationClient = remember { LocationClient(context) }

            // --- SIDE EFFECTS ---
            LaunchedEffect(sessionId) { if (sessionId == null) { navController.popBackStack() } }
            LaunchedEffect(Unit) { locationClient.getLocationUpdates(5000L).collect { location -> latestUserLocation = LatLng(location.latitude, location.longitude) } }
            LaunchedEffect(latestUserLocation) { if (autoFollow && latestUserLocation != null) { cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(latestUserLocation!!, 15f)) } }
            LaunchedEffect(cameraPositionState.isMoving) { if (cameraPositionState.isMoving) { autoFollow = false } }
            DisposableEffect(sessionId, lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val userId = viewModel.currentUserId
                        if (sessionId != null && userId != null) {
                            viewModel.listenToLocationUpdates()
                            val intent = Intent(context, LocationService::class.java).apply {
                                action = LocationService.ACTION_START
                                putExtra("sessionId", sessionId)
                                putExtra("currentUserId", userId)
                            }
                            try { context.startService(intent) } catch (e: Exception) { Log.e("MapScreen", "Failed to start service: ${e.message}") }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    val intent = Intent(context, LocationService::class.java).apply { action = LocationService.ACTION_STOP }
                    context.startService(intent)
                }
            }

            // --- MAP UI ---
            Box(modifier = Modifier.fillMaxSize()) {
                // The GoogleMap is the bottom layer. Its content block is ONLY for map-related items.
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties
                ) {
                    // Draw markers for Walker and Companion
                    locations.forEach { (id, latLng) ->
                        val title = when (id) {
                            activeRequest?.walkerId -> "Walker"
                            activeRequest?.companionId -> "Companion"
                            else -> "User"
                        }
                        Marker(state = MarkerState(position = LatLng(latLng.first, latLng.second)), title = title)
                    }
                    // Draw marker for the final destination
                    activeRequest?.let { request ->
                        if (request.destLat != 0.0 && request.destLng != 0.0) {
                            Marker(
                                state = MarkerState(position = LatLng(request.destLat, request.destLng)),
                                title = "Final Destination",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                    }
                    // Draw the route line
                    if (routePoints.isNotEmpty()) {
                        Polyline(points = routePoints, color = Color.Blue, width = 15f)
                    }
                }

                // --- OVERLAYS ---
                // This logic is now a child of the Box, drawn ON TOP of the map. This fixes the crash.
                locations.forEach { (id, latLngPair) ->
                    val latLng = LatLng(latLngPair.first, latLngPair.second)
                    val title = when (id) {
                        activeRequest?.walkerId -> "Walker"
                        activeRequest?.companionId -> "Companion"
                        else -> "User"
                    }
                    cameraPositionState.projection?.toScreenLocation(latLng)?.let { screenPosition ->
                        Box(
                            modifier = Modifier
                                .offset(x = screenPosition.x.dp, y = (screenPosition.y - 100).dp) // Offset to appear above the marker
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.8f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color.Black)
                        }
                    }
                }

                // --- FLOATING ACTION BUTTONS ---
                FloatingActionButton(
                    onClick = {
                        autoFollow = true
                        if (latestUserLocation != null) {
                            coroutineScope.launch { cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(latestUserLocation!!, 15f)) }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Re-center Map")
                }
                FloatingActionButton(
                    onClick = { viewModel.endSession() },
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = "End Session")
                }
                AnimatedVisibility(
                    visible = isWalker && isCloseEnough && sessionStatus == "active",
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.beginWalkingSession() },
                        modifier = Modifier.padding(bottom = 16.dp),
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        text = { Text("Start Walking") }
                    )
                }

                // Companion Confirmation Dialog
                if (!isWalker && sessionStatus == "awaiting_walk_confirmation") {
                    AlertDialog(
                        onDismissRequest = { /* Cannot dismiss */ },
                        title = { Text("Ready to Walk?") },
                        text = { Text("The Walker has indicated you have met. Confirm to start the walk to the final destination.") },
                        confirmButton = { Button(onClick = { viewModel.confirmWalkingSession() }) { Text("Confirm and Start") } }
                    )
                }
            }
        }
        // Case 2: User granted foreground but needs background permission.
        hasFineLocationPermission -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Background Location Needed", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("This app needs 'Allow all the time' permission to track your location in the background during a session.", textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            }
        }
        // Case 3: No permissions granted.
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Location permission is required to use the map feature.")
            }
        }
    }
}