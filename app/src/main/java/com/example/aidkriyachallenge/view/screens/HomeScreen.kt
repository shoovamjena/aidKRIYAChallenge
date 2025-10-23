package com.example.aidkriyachallenge.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aidkriyachallenge.viewModel.MainViewModel
import com.example.aidkriyachallenge.viewmodel.MyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MyViewModel, // Your existing ViewModel
    mapRoutingViewModel: MainViewModel, // The ViewModel for the tracking feature
    navController: NavController
) {
    // --- Collect the role needed for MapRouting ---
    val mapRoutingUserRole by mapRoutingViewModel.userRole.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Home") })
        }
    ) { padding ->
        // Use Column for better arrangement of multiple buttons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Center content vertically
        ) {
            Text(
                text = "Welcome to the Home Screen 🎉",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(20.dp)) // Add space

            // --- ADDED: Start Live Tracking Session Button ---
            Button(
                onClick = {
                    // Navigate directly to the RequestScreen with the fetched role
                    mapRoutingUserRole?.let { role ->
                        // Use the correct route name (e.g., "tracking_request")
                        navController.navigate("tracking_request/$role")
                    }
                },
                enabled = mapRoutingUserRole != null // Enable only when role is loaded
            ) {
                if (mapRoutingUserRole == null) {
                    // Show a spinner while fetching the role
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Start Live Tracking Session")
                }
            }
            Spacer(modifier = Modifier.height(10.dp)) // Add space

            // --- Your Existing Buttons ---
            Button(onClick = { navController.navigate("ReviewSubmit") }) {
                Text("ReviewSubmit")
            }
            Spacer(modifier = Modifier.height(10.dp))

            Button(onClick = { navController.navigate("ReviewSeen") }) {
                Text("ReviewSeen")
            }
            Spacer(modifier = Modifier.height(10.dp))

            Button(onClick = { navController.navigate("profileSk") }) {
                Text("profileScreen")
            }
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    viewModel.logout()
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            ) {
                Text("Logout")
            }
        }
    }
}