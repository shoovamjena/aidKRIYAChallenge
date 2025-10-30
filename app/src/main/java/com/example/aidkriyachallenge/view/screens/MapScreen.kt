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
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material3.*
import androidx.compose.ui.draw.clip

import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import com.example.aidkriyachallenge.R
import com.example.aidkriyachallenge.dataModel.Request
import com.example.aidkriyachallenge.viewModel.MainViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.request.ImageRequest
import com.example.aidkriyachallenge.dataModel.Screen


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    val navigateToPayment by viewModel.navigateToPayment.collectAsState()

    LaunchedEffect(navigateToPayment) {
        navigateToPayment?.let { (distance, amount) ->
            // Navigate to Payment screen
            navController.navigate(Screen.Payment.createRoute(distance, amount))
            viewModel.onPaymentNavigationComplete() // Reset the trigger
        }
    }

    LaunchedEffect(sessionId) {
        if (sessionId == null) {
            // Session is truly over. Go to Home and clear the stack.
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    when {
        // Case 1: All permissions granted, show the map.
        allPermissionsGranted -> {
            val lifecycleOwner = LocalLifecycleOwner.current
            val locationClient = remember { LocationClient(context) }

            // --- SIDE EFFECTS ---
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
                if (sessionStatus == "payment_pending" && !isWalker) {
                    AlertDialog(
                        onDismissRequest = { /* Cannot dismiss */ },
                        title = { Text("Session Ending") },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("The walker is processing the payment.")
                                Spacer(Modifier.height(16.dp))
                                ContainedLoadingIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Please wait...")
                            }
                        },
                        confirmButton = {} // No button, not dismissible
                    )
                }
                // The GoogleMap is the bottom layer. Its content block is ONLY for map-related items.
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties
                ) {

                    // --- MARKER LOGIC UPDATED ---
                    locations.forEach { (id, latLng) ->
                        val position = LatLng(latLng.first, latLng.second)

                        when (id) {
                            activeRequest?.walkerId -> {
                                // Use the custom composable marker for the Walker
                                MarkerComposable(
                                    state = MarkerState(position = position)
                                ) {
                                    // We pass the whole request so WalkerMapMarker
                                    // can get the profileImageUrl from it
                                    activeRequest?.let { WalkerMapMarker(request = it) }
                                }
                            }
                            activeRequest?.companionId -> {
                                // Use a green default marker for the Companion
                                Marker(
                                    state = MarkerState(position = position),
                                    title = "Companion",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                )
                            }
                            else -> {
                                // Fallback just in case
                                Marker(state = MarkerState(position = position), title = "User")
                            }
                        }
                    }
                    // --- END OF UPDATED MARKER LOGIC ---

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
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).windowInsetsPadding(
                        WindowInsets.navigationBars
                    )
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Re-center Map")
                }
                if(isWalker){
                    FloatingActionButton(
                        onClick = { viewModel.endSession(isWalker) },
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).windowInsetsPadding(
                            WindowInsets.navigationBars
                        ),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = "End Session")
                    }
                }
                AnimatedVisibility(
                    visible = isWalker && isCloseEnough && sessionStatus == "active",
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.beginWalkingSession() },
                        modifier = Modifier.padding(bottom = 16.dp).windowInsetsPadding(
                            WindowInsets.navigationBars
                        ),
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

// --- ADD THIS COMPOSABLE TO THE END OF YOUR FILE ---
// (Copied from HomeScreen)

// In /view/screens/MapScreen.kt

@Composable
private fun WalkerMapMarker(request: Request) {
    val context = LocalContext.current // Get the context

    // --- THIS IS THE FIX ---
    // We build a custom request to pass to AsyncImage
    val imageRequest = ImageRequest.Builder(context)
        .data(request.profileImageUrl) // The URL
        .placeholder(R.drawable.profile)
        .error(R.drawable.profile)
        .allowHardware(false) // <-- This is the key to fix the crash
        .build()
    // --- END OF FIX ---

    Box(
        modifier = Modifier.size(56.dp), // Total size of the marker
        contentAlignment = Alignment.TopCenter // Aligns the AsyncImage to the top
    ) {
        // 1. The background teardrop pin
        Icon(
            imageVector = Icons.Default.Place, // Built-in material icon
            contentDescription = "Map Pin",
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.primary // Tint it to your app's theme
        )
        // 2. The circular profile image
        AsyncImage(
            model = imageRequest, // <-- Pass the custom request here
            contentDescription = "Walker Profile",
            modifier = Modifier
                .padding(top = 4.dp) // Adjust padding to center it nicely
                .size(36.dp) // Size of the circular image
                .clip(CircleShape)
                .border(1.dp, Color.White, CircleShape), // Optional white border
            contentScale = ContentScale.Crop
        )
    }
}