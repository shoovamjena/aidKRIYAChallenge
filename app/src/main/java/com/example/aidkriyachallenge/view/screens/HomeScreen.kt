package com.example.aidkriyachallenge.view.screens

// --- NEW IMPORTS ---
// --- END NEW IMPORTS ---

// --- NEW IMPORT ---
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.aidkriyachallenge.R
import com.example.aidkriyachallenge.dataModel.Request
import com.example.aidkriyachallenge.dataModel.Screen
import com.example.aidkriyachallenge.ui.theme.inspDoc
import com.example.aidkriyachallenge.ui.theme.odin
import com.example.aidkriyachallenge.viewModel.MainViewModel
import com.example.aidkriyachallenge.viewModel.MyViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

// --- Helper data class for the stats carousel ---
private data class StatItem(
    val title: String,
    val value: String,
    val icon: ImageVector
)

private fun getZoomForRadius(radiusInMeters: Int): Float {
    return when (radiusInMeters) {
        1000 -> 15f // 1km
        2000 -> 14f // 2km
        5000 -> 13f // 5km
        else -> 15f
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun HomeScreen(
    viewModel: MyViewModel,
    navController: NavController,
    mapRoutingViewModel: MainViewModel,
) {
    val state by viewModel.Profilestate.collectAsState()
    val mapRoutingUserRole by mapRoutingViewModel.userRole.collectAsState()
    LaunchedEffect(key1 = Unit) {
        viewModel.loadProfile()
    }

    // --- NEW: States from MainViewModel ---
    val currentUserLocation by mapRoutingViewModel.currentUserLocation.collectAsState()
    val pendingRequests by mapRoutingViewModel.pendingRequests.collectAsState()
    val selectedRequest by mapRoutingViewModel.selectedRequest.collectAsState()
    // --- END NEW STATES ---

    val radiusOptions = listOf(1000, 2000, 5000) // in meters
    var selectedRadius by remember { mutableIntStateOf(radiusOptions.first()) }

    val stats = listOf(
        StatItem("Distance", "0.0 km", Icons.AutoMirrored.Filled.DirectionsWalk),
        StatItem("Calories", "0 kcal", Icons.Default.LocalFireDepartment),
        StatItem("Steps", "0", Icons.Default.StackedLineChart),
        StatItem("Last Walk", "N/A", Icons.Default.History)
    )

    val context = LocalContext.current

    val defaultLocation = LatLng(20.5937, 78.9629) // Default to India
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 5f)
    }

    val pendingInterests by mapRoutingViewModel.pendingCompanionInterests.collectAsState()

    // --- NEW: States from RequestScreen ---
    val activeRequest by mapRoutingViewModel.activeRequest.collectAsState()
    val sessionId by mapRoutingViewModel.sessionId.collectAsState()

    // --- All LaunchedEffects for permissions, location, etc., are unchanged ---
    // ... (They are all correct)

    // --- NEW: Logic to handle destination selection result ---
    val backStackEntry = navController.currentBackStackEntry
    val destinationLocation by backStackEntry
        ?.savedStateHandle
        ?.getStateFlow<LatLng?>("destination_location", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) } // <-- Add this back

    // This effect triggers `raiseCall` when a destination is selected
    LaunchedEffect(destinationLocation) {
        destinationLocation?.let { dest ->
            Log.d("HomeScreen", "Got destination: $dest. Calling raiseCall...")
            mapRoutingViewModel.raiseCall(dest)
            // Clear the result so it doesn't trigger again
            backStackEntry?.savedStateHandle?.remove<LatLng>("destination_location")
        }
    }

    // --- NEW: Permission Handling (copied from RequestScreen) ---
    // This is for background location, which is only needed by the Walker
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
            } else {
                true
            }
        )
    }
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasFineLocationPermission =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            hasBackgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false
            } else {
                true
            }
        }
    )

    // --- NEW: Permission Launcher ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, start location updates via ViewModel
                mapRoutingViewModel.startLocationUpdates()
            }
            // else: Permission denied. Map will stay zoomed out.
        }
    )

    // --- This LaunchedEffect handles the zoom-in animation ---
    LaunchedEffect(Unit) {
        mapRoutingViewModel.observeRequestChanges()
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted, start updates
                mapRoutingViewModel.startLocationUpdates()
            }

            else -> {
                // Permission is NOT granted, launch the request
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // --- NEW: LaunchedEffect to animate camera ONCE to user's location ---
    val hasAnimatedToLocation = remember { mutableStateOf(false) }
    LaunchedEffect(currentUserLocation) {
        if (currentUserLocation != null && !hasAnimatedToLocation.value) {
            val userLatLng = LatLng(currentUserLocation!!.latitude, currentUserLocation!!.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(userLatLng, 15f), // Zoom in a bit
                1000
            )
            hasAnimatedToLocation.value = true // Mark as animated
        }
    }

    // --- NEW: LaunchedEffect to listen for requests if user is a Companion ---
    LaunchedEffect(mapRoutingUserRole) {
        if (mapRoutingUserRole == "Companion") {
            mapRoutingViewModel.listenToPendingRequests()
        }
        // Optional: You could add an `else` block to stop listening
        // if the role changes, but it's likely not necessary.
    }

    LaunchedEffect(selectedRadius, currentUserLocation) {
        // Only animate if we already have a location
        if (currentUserLocation != null) {
            val userLatLng = LatLng(currentUserLocation!!.latitude, currentUserLocation!!.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    userLatLng,
                    getZoomForRadius(selectedRadius)
                ),
                1000 // 1 second animation
            )
        }
    }


    Scaffold { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Top Welcome Box ---
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.3f) // Takes 30% of the screen height
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.surfaceTint,
                            )
                        ),
                        shape = RoundedCornerShape(bottomEndPercent = 20, bottomStartPercent = 20)
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .padding(15.dp)
                        .size(100.dp)
                        .align(Alignment.TopEnd)
                        .clickable {
                            navController.navigate("profileSk")
                        }
                ) {
                    Image(
                        painter = painterResource(R.drawable.profile),
                        contentDescription = "Profile Image",
                    )
                }
                IconButton(
                    onClick = {
                        viewModel.logout()
                        navController.navigate("welcome") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .padding(top = 85.dp)
                        .padding(40.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_power_settings_new_24),
                        contentDescription = "Profile Image",
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp, start = 25.dp),
                ) {
                    Text(
                        "WELCOME",
                        fontFamily = odin,
                        fontSize = 46.sp,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    )

                    Text(
                        state.username,
                        fontFamily = inspDoc,
                        fontSize = 96.sp,
                        color = Color(0xFF0F790E),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 10.dp)
                            .height(50.dp)
                            .clickable {
                                navController.navigate("ReviewSeen")
                            }
                            .innerShadow(
                                shape = RoundedCornerShape(50),
                                shadow = Shadow(
                                    radius = 25.dp,
                                    Color.White
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "SEE REVIEWS",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                }


            }

            // --- Stats Carousel (as LazyRow) ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), // This replaces 'itemSpacing'
                contentPadding = PaddingValues(horizontal = 10.dp),  // This is the same
                verticalAlignment = Alignment.CenterVertically // Good to add for content height
            ) {
                items(stats) { stat ->
                    Box(modifier = Modifier.width(110.dp)) {
                        StatCard(item = stat)
                    }
                }
            }

            // --- Map and Button Box ---
            Box(
                modifier = Modifier
                    .shadow(5.dp, RoundedCornerShape(15))
                    .clip(RoundedCornerShape(15))
                    .weight(0.1f) // Fills remaining vertical space
                    .fillMaxWidth(0.9f)

            ) {
                // --- LAYER 1: The Google Map ---
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    // This enables the blue dot (current location indicator)
                    properties = MapProperties(isMyLocationEnabled = (currentUserLocation != null)),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true)
                ) {
                    // --- NEW: Show Walker markers if user is a Companion ---
                    if (mapRoutingUserRole == "Companion" && currentUserLocation != null) {
                        val companionLatLng =
                            LatLng(currentUserLocation!!.latitude, currentUserLocation!!.longitude)

                        val nearbyRequests = pendingRequests.filter { request ->
                            val distanceInMeters = SphericalUtil.computeDistanceBetween(
                                companionLatLng,
                                LatLng(request.lat, request.lng)
                            )
                            distanceInMeters <= selectedRadius
                        }

                        for (request in nearbyRequests) {
                            // --- MODIFIED: Calculate distance for title ---
                            val distanceInMeters = SphericalUtil.computeDistanceBetween(
                                companionLatLng,
                                LatLng(request.lat, request.lng)
                            )
                            val distanceText = String.format("%.1f km", distanceInMeters / 1000)

                            Marker(
                                state = MarkerState(position = LatLng(request.lat, request.lng)),
                                title = distanceText, // <-- 1. SHOWS DISTANCE
                                snippet = "Click for details",
                                onClick = {
                                    mapRoutingViewModel.onMarkerClick(request)
                                    true // Consume the click event
                                }
                            )
                        }
                    }
                    // --- NEW: Show destination marker if a request is selected ---
                    selectedRequest?.let { request ->
                        // 1. Define LatLng points
                        val walkerLatLng = LatLng(request.lat, request.lng)
                        val destinationLatLng = LatLng(request.destLat, request.destLng)

                        // 2. Draw Destination Marker
                        Marker(
                            state = MarkerState(position = destinationLatLng),
                            title = "Final Destination",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )

                        // 3. Draw Polylines (ONLY if companion location is known)
                        currentUserLocation?.let { location ->
                            val companionLatLng = LatLng(location.latitude, location.longitude)

                            // Path 1: Companion to Walker (Blue)
                            Polyline(
                                points = listOf(companionLatLng, walkerLatLng),
                                color = Color.Blue,
                                width = 10f
                            )

                            // Path 2: Walker to Destination (Green)
                            Polyline(
                                points = listOf(walkerLatLng, destinationLatLng),
                                color = Color(0xFF0F790E), // Using your app's green
                                width = 10f
                            )
                        }
                    }
                }
                this@Column.AnimatedVisibility(
                    visible = selectedRequest != null,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    selectedRequest?.let { currentRequest ->

                        // --- NEW: Check if this request is pending ---
                        val isPending = pendingInterests.contains(currentRequest.id)

                        // --- B. SHOW INFO CARD ---
                        WalkerInfoCard(
                            request = currentRequest,
                            currentUserLocation = currentUserLocation,
                            onAccept = {
                                // 1. Show the Toast
                                Toast.makeText(
                                    context,
                                    "Walking interest sent to the wanderer \n They may accept or reject it",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // 2. Tell the VM to send interest
                                mapRoutingViewModel.sendInterestToWalker(currentRequest.id)
                                mapRoutingViewModel.acceptCall(currentRequest.id)
                                // 3. The card will auto-flip to "Waiting..."
                                //    on the next recomposition
                            },
                            onDismiss = {
                                mapRoutingViewModel.onMarkerClick(null)
                            }
                        )

                    }
                }
                if (mapRoutingUserRole == "Companion") {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        radiusOptions.forEach { radiusInMeters ->
                            val isSelected = radiusInMeters == selectedRadius
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRadius = radiusInMeters },
                                label = { Text("${radiusInMeters / 1000} km") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .heightIn(min = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                when (mapRoutingUserRole) {
                    "Walker" -> {
                        // --- Walker Flow UI ---
                        WalkerFlowUI(
                            activeRequest = activeRequest,
                            sessionId = sessionId,
                            currentUserLocation = currentUserLocation,
                            navController = navController,
                            onConfirm = {
                                mapRoutingViewModel.confirmMatch(
                                    activeRequest!!.id,
                                    activeRequest!!.companionId!!
                                )
                            },
                            onReject = {
                                mapRoutingViewModel.rejectCompanion(
                                    activeRequest!!.id,
                                    activeRequest!!.companionId!!
                                )
                            }
                        )
                    }

                    "Companion" -> {
                        // --- Companion Flow UI (is just the map, so show nothing here) ---
                        // Or you could show a status text
                        Text("Ready to accept requests")
                    }

                    else -> {
                        // --- Loading Role ---
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun WalkerFlowUI(
    activeRequest: Request?,
    sessionId: String?,
    currentUserLocation: Location?,
    navController: NavController,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    // This logic is copied directly from RequestScreen
    when {
        // State 1: No request. Show "START WALK" button.
        activeRequest == null && sessionId == null -> {
            Button(
                onClick = {
                    currentUserLocation?.let { location ->
                        navController.navigate(
                            "${Screen.DestinationSelection.route}/${location.latitude}/${location.longitude}"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("START WALK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        // State 2: Request sent, no companion yet.
        activeRequest?.companionId == null -> {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Looking for a companion...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
        }

        // State 3: Companion found! Show the confirmation view.
        // We'll show this as a full-screen composable.
        else -> {
            // We need to navigate to a new screen for the full-map view
            // OR show it as a dialog.
            // For now, let's re-use the CompanionFoundView as a Dialog
            // (A better solution would be to navigate to a new screen)

            // --- This part is tricky. CompanionFoundView IS a map. ---
            // You can't put a map in this small box.
            // Let's just show the CARD part.

            val distance = remember(activeRequest, currentUserLocation) {
                if (activeRequest.companionLat != null) {
                    val d = SphericalUtil.computeDistanceBetween(
                        LatLng(activeRequest.lat, activeRequest.lng),
                        activeRequest.companionLng?.let { LatLng(activeRequest.companionLat, it) }
                    )
                    String.format("%.1f km away", d / 1000)
                } else "..."
            }

            Card(
                modifier = Modifier.fillMaxWidth()
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
}

// --- Composable for the stats card ---
@Composable
private fun StatCard(item: StatItem) {
    Card(
        modifier = Modifier
            .height(130.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = item.value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
private fun WalkerInfoCard(
    request: Request,
    currentUserLocation: Location?,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Calculate Companion-to-Walker distance
    val distance = remember(currentUserLocation, request) {
        if (currentUserLocation != null) {
            val distanceInMeters = SphericalUtil.computeDistanceBetween(
                LatLng(currentUserLocation.latitude, currentUserLocation.longitude),
                LatLng(request.lat, request.lng)
            )
            String.format("%.1f km away", distanceInMeters / 1000)
        } else {
            "Calculating..."
        }
    }

    // --- NEW: Calculate Walker-to-Destination distance ---
    val walkDistance = remember(request) {
        val wLatLng = LatLng(request.lat, request.lng)
        val dLatLng = LatLng(request.destLat, request.destLng)
        val distanceInMeters = SphericalUtil.computeDistanceBetween(wLatLng, dLatLng)
        String.format("%.1f km", distanceInMeters / 1000)
    }
    // --- END NEW ---

    // 2. Build the Card UI
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Walker Profile Info ---
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Walker Request", // TODO: Replace with request.walkerName
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = distance, // This is Companion -> Walker
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // --- NEW: Display the walk distance ---
                Text(
                    text = "Walk Distance: $walkDistance", // This is Walker -> Dest
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                // --- END NEW ---

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // --- TODO: Populate this from your Request object ---
                Text(
                    text = "Gender: N/A",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Speed: N/A",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Rating: ☆ 0.0",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // --- Accept/Reject Buttons (Unchanged) ---
            Row {
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject")
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onAccept,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Accept")
                }
            }
        }
    }
}

// --- ADD THIS NEW COMPOSABLE TO HomeScreen.kt ---

