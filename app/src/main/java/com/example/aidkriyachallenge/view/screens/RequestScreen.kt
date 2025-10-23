package com.example.aidkriyachallenge.view.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.aidkriyachallenge.dataModel.Request
import com.example.aidkriyachallenge.dataModel.Screen
import com.example.aidkriyachallenge.viewModel.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun RequestScreen(
    viewModel: MainViewModel,
    role: String,
    navController: NavHostController // NavController is now required
) {
    // --- STATE COLLECTION ---
    val userStatus by viewModel.userStatus.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val activeRequest by viewModel.activeRequest.collectAsState()
    val sessionId by viewModel.sessionId.collectAsState()
    val currentUserLocation by viewModel.currentUserLocation.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()
    val context = LocalContext.current

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

    // --- NEW: LOGIC TO HANDLE DESTINATION SELECTION RESULT ---
    val backStackEntry = navController.currentBackStackEntry
    val destinationLocation by backStackEntry
        ?.savedStateHandle
        ?.getStateFlow<LatLng?>("destination_location", null)
        ?.collectAsState() ?:remember {   mutableStateOf(null)}

    // This effect triggers `raiseCall` when a destination is selected
    LaunchedEffect(destinationLocation) {
        destinationLocation?.let { dest ->
            viewModel.raiseCall(dest)
            // Clear the result so it doesn't trigger again on recomposition
            backStackEntry?.savedStateHandle?.remove<LatLng>("destination_location")
        }
    }

    // --- UI RENDER ---
    val allPermissionsGranted = hasFineLocationPermission && hasBackgroundLocationPermission
    when {
        allPermissionsGranted -> {
            LaunchedEffect(Unit) { viewModel.startLocationUpdates() }
            if (role == "Companion") {
                LaunchedEffect(Unit) { viewModel.listenToPendingRequests() }
                CompanionMap(
                    viewModel = viewModel,
                    pendingRequests = pendingRequests,
                    currentUserLocation = currentUserLocation,
                    selectedRequest = selectedRequest
                )
            } else { // Walker UI
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when {
                        activeRequest == null && sessionId == null -> {
                            // This button now navigates to the selection screen
                            Button(onClick = {
                                // Get the current location from the ViewModel
                                currentUserLocation?.let { location ->
                                    // Navigate with the user's current lat/lng in the route
                                    navController.navigate(
                                        "${Screen.DestinationSelection.route}/${location.latitude}/${location.longitude}"
                                    )
                                }
                            }) {
                                Text("Find a Companion")
                            }
                        }
                        activeRequest?.companionId == null -> {
                            Text("Looking for a companion...", style = MaterialTheme.typography.headlineSmall)
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        }
                        else -> {
                            CompanionFoundView(
                                request = activeRequest!!,
                                onConfirm = { viewModel.confirmMatch(activeRequest!!.id, activeRequest!!.companionId!!) },
                                onReject = { viewModel.rejectCompanion(activeRequest!!.id, activeRequest!!.companionId!!) }
                            )
                        }
                    }
                }
            }
        }
        hasFineLocationPermission -> {
            // UI to ask for Background Permission
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Background Location Needed", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("This app needs 'Allow all the time' permission to track your location when the app is in the background during a session.", textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            }
        }
        else -> {
            // UI for when all permissions are denied
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Location permission is required to use this app.")
            }
        }
    }
}

@Composable
fun CompanionMap(
    viewModel: MainViewModel,
    pendingRequests: List<Request>,
    currentUserLocation: Location?,
    selectedRequest: Request?
) {
    val cameraPositionState = rememberCameraPositionState()

    // This effect animates the camera to the user's location when available
    LaunchedEffect(currentUserLocation) {
        if (currentUserLocation != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentUserLocation.latitude, currentUserLocation.longitude), 12f
                )
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // Only show markers if the companion's location is known
            if (currentUserLocation != null) {
                // Filter requests to be within a 5km radius
                pendingRequests.filter { request ->
                    val distanceInMeters = SphericalUtil.computeDistanceBetween(
                        LatLng(currentUserLocation.latitude, currentUserLocation.longitude),
                        LatLng(request.lat, request.lng)
                    )
                    distanceInMeters <= 5000
                }.forEach { request ->
                    // Draw a marker for each nearby walker request
                    Marker(
                        state = MarkerState(position = LatLng(request.lat, request.lng)),
                        title = "Walker Request",
                        snippet = "Click for details",
                        onClick = {
                            viewModel.onMarkerClick(request)
                            true // Consume the click event
                        }
                    )
                }
            }

            // --- THIS IS THE NEW LOGIC ---
            // If a request has been selected by tapping a marker...
            selectedRequest?.let { request ->
                // ...draw a special marker for its final destination.
                Marker(
                    state = MarkerState(position = LatLng(request.destLat, request.destLng)),
                    title = "Final Destination",
                    // Use a different color (blue) to distinguish it
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
            // --- END OF NEW LOGIC ---
        }

        // Show the dialog if a request is selected
        if (selectedRequest != null) {
            RequestInfoDialog(
                request = selectedRequest,
                currentUserLocation = currentUserLocation,
                onDismiss = { viewModel.onMarkerClick(null) },
                onAccept = {
                    viewModel.acceptCall(selectedRequest.id)
                    viewModel.onMarkerClick(null)
                }
            )
        }
    }
}


@Composable
fun RequestInfoDialog(
    request: Request,
    currentUserLocation: Location?,
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    val distance = remember(currentUserLocation, request) {
        if (currentUserLocation != null) {
            val distanceInMeters = SphericalUtil.computeDistanceBetween(
                LatLng(currentUserLocation.latitude, currentUserLocation.longitude),
                LatLng(request.lat, request.lng)
            )
            String.format("%.1f km away", distanceInMeters / 1000)
        } else { "Calculating distance..." }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Request") },
        text = {
            Column {
                Text("A user is requesting a companion.")
                Spacer(Modifier.height(8.dp))
                // --- THIS IS THE NEW TEXT ---
                Text("A destination has been set for this walk.")
                Spacer(Modifier.height(8.dp))
                // --- END OF NEW TEXT ---
                Text("Distance from you: $distance")
            }
        },
        confirmButton = { Button(onClick = onAccept) { Text("Accept") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun CompanionFoundView(
    request: Request,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    // Ensure we have all the location data we need before displaying the map.
    if (request.companionLat == null || request.companionLng == null) {
        // Show a loading indicator if the companion's location isn't available yet.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Define the three key locations.
    val walkerLocation = LatLng(request.lat, request.lng)
    val companionLocation = LatLng(request.companionLat, request.companionLng)
    val destinationLocation = LatLng(request.destLat, request.destLng)

    // Calculate the distance between the walker and companion.
    val distance = remember {
        val distanceInMeters = SphericalUtil.computeDistanceBetween(walkerLocation, companionLocation)
        String.format("%.1f km away", distanceInMeters / 1000)
    }

    // Set up the map camera to show all three points.
    val cameraPositionState = rememberCameraPositionState()
    LaunchedEffect(Unit) {
        val bounds = LatLngBounds.builder()
            .include(walkerLocation)
            .include(companionLocation)
            .include(destinationLocation)
            .build()
        // Animate the camera to fit all markers with 150px padding.
        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // Marker for the Walker
            Marker(state = MarkerState(position = walkerLocation), title = "Your Location")
            // Marker for the Companion
            Marker(state = MarkerState(position = companionLocation), title = "Companion")
            // A distinct, blue marker for the final destination
            Marker(
                state = MarkerState(position = destinationLocation),
                title = "Final Destination",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )
        }

        // Card with confirmation controls at the bottom of the screen
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Companion Found!", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text("Your potential companion is $distance")
                Spacer(Modifier.height(16.dp))
                Row {
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reject")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = onConfirm) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}