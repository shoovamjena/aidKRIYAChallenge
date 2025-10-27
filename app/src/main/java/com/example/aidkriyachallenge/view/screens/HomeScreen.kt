package com.example.aidkriyachallenge.view.screens

// --- NEW IMPORTS ---
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
// --- END NEW IMPORTS ---

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// --- NEW IMPORT ---
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.aidkriyachallenge.R
import com.example.aidkriyachallenge.ui.theme.inspDoc
import com.example.aidkriyachallenge.ui.theme.odin
import com.example.aidkriyachallenge.viewModel.MainViewModel
import com.example.aidkriyachallenge.viewModel.MyViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

// --- Helper data class for the stats carousel ---
private data class StatItem(
    val title: String,
    val value: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
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

    val stats = listOf(
        StatItem("Distance", "0.0 km", Icons.Default.DirectionsWalk),
        StatItem("Calories", "0 kcal", Icons.Default.LocalFireDepartment),
        StatItem("Steps", "0", Icons.Default.StackedLineChart),
        StatItem("Last Walk", "N/A", Icons.Default.History)
    )

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val defaultLocation = LatLng(20.5937, 78.9629) // Default to India
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 5f)
    }

    // --- NEW: Coroutine scope for the launcher's callback ---
    val scope = rememberCoroutineScope()

    // --- NEW: Function to get location and animate camera ---
    // We need this to avoid duplicating code
    val getLocationAndAnimate = {
        // We must check permission *again* here for safety
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    // We need a coroutine scope to animate
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(userLatLng, 17f), 1000
                        )
                    }
                }
            }
        }
    }

    // --- NEW: Permission Launcher ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                // Permission was granted. Now get location.
                getLocationAndAnimate()
            }
            // else: Permission denied. The map will stay zoomed out.
        }
    )

    // --- This LaunchedEffect handles the zoom-in animation ---
    LaunchedEffect(Unit) {
        when {
            // Check if we already have permission
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted, just get location
                getLocationAndAnimate()
            }
            // TODO: You can add logic for shouldShowRequestPermissionRationale here if desired
            else -> {
                // Permission is NOT granted, launch the request
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }


    Scaffold(
    ) { padding ->
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
                        ), shape = RoundedCornerShape(bottomEndPercent = 20, bottomStartPercent = 20)
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
                Text(
                    "WELCOME",
                    fontFamily = odin,
                    fontSize = 46.sp,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(top = 50.dp, start = 20.dp)
                )

                Text(
                    state.username,
                    fontFamily = inspDoc,
                    fontSize = 96.sp,
                    color = Color(0xFF0F790E),
                    modifier = Modifier.padding(top = 70.dp, start = 20.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .align(Alignment.BottomCenter)
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
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true)
                )


            }
            Button(
                onClick = {
                    mapRoutingUserRole?.let { role ->
                        navController.navigate("tracking_request/$role")
                    }
                },
                enabled = mapRoutingUserRole != null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(56.dp)
            ) {
                if (mapRoutingUserRole == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        "START WALK",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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