package com.example.aidkriyachallenge.view.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
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
    val activeRequest by viewModel.activeRequest.collectAsState()
    val sessionId by viewModel.sessionId.collectAsState()
    val currentUserLocation by viewModel.currentUserLocation.collectAsState()
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
            // Start location updates, as this screen is now only for active sessions
            LaunchedEffect(Unit) { viewModel.startLocationUpdates() }

            // --- REMOVED Companion branch ---
            // if (role == "Companion") { ... }

            // --- This is now the default view ---
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    activeRequest == null && sessionId == null -> {
                        Button(onClick = {
                            currentUserLocation?.let { location ->
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
                            onConfirm = {
                                Log.d("RequestScreen", "onConfirm lambda executed! Calling ViewModel...")
                                viewModel.confirmMatch(activeRequest!!.id, activeRequest!!.companionId!!) },
                            onReject = { viewModel.rejectCompanion(activeRequest!!.id, activeRequest!!.companionId!!) }
                        )
                    }
                }
            }
        }
        hasFineLocationPermission -> {
            // UI to ask for Background Permission (No Changes)
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
            // UI for when all permissions are denied (No Changes)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Location permission is required to use this app.")
            }
        }
    }
}

@Composable
fun CompanionFoundView(
    request: Request,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    // ... (This function remains exactly the same)
    if (request.companionLat == null || request.companionLng == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val walkerLocation = LatLng(request.lat, request.lng)
    val companionLocation = LatLng(request.companionLat, request.companionLng)
    val destinationLocation = LatLng(request.destLat, request.destLng)

    val distance = remember {
        val distanceInMeters = SphericalUtil.computeDistanceBetween(walkerLocation, companionLocation)
        String.format("%.1f km away", distanceInMeters / 1000)
    }

    val cameraPositionState = rememberCameraPositionState()
    LaunchedEffect(Unit) {
        val bounds = LatLngBounds.builder()
            .include(walkerLocation)
            .include(companionLocation)
            .include(destinationLocation)
            .build()
        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(state = MarkerState(position = walkerLocation), title = "Your Location")
            Marker(state = MarkerState(position = companionLocation), title = "Companion")
            Marker(
                state = MarkerState(position = destinationLocation),
                title = "Final Destination",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )
        }

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

