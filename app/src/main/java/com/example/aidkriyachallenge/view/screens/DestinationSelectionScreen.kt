package com.example.aidkriyachallenge.view.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSelectionScreen(
    startLocation: LatLng,
    onCancel: () -> Unit,
    onDestinationSelected: (LatLng) -> Unit
) {
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLocation, 15f)}

    Scaffold(
        floatingActionButton = {
            if (selectedLocation != null) {
                FloatingActionButton(
                    onClick = { onDestinationSelected(selectedLocation!!) }
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Confirm Destination")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Select a Destination") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        // In a real app, you'd use an ArrowBack icon
                        Text("X")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                onMapLongClick = { latLng ->
                    // When user long-presses, update the selected location
                    selectedLocation = latLng
                }
            ) {
                // Show a marker at the selected location
                if (selectedLocation != null) {
                    Marker(
                        state = MarkerState(position = selectedLocation!!),
                        title = "Destination"
                    )
                }
            }

            if (selectedLocation == null) {
                Text(
                    text = "Long-press on the map to select a destination",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}