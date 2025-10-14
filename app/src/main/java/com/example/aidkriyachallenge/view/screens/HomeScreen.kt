package com.example.aidkriyachallenge.view.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aidkriyachallenge.viewmodel.MyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MyViewModel,
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Home") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Welcome to the Home Screen 🎉",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(5.dp))
            Button(
                onClick = {
                    navController.navigate("ReviewSubmit")
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(bottom = 40.dp)
            ) {
                Text("ReviewSubmit")
            }
            Spacer(modifier = Modifier.height(5.dp))
            Button(
                onClick = {
                    navController.navigate("ReviewSeen")
                },
                modifier = Modifier.align(Alignment.Center).padding(bottom = 30.dp)
            ) {
                Text("ReviewSeen")
            }
            Spacer(modifier = Modifier.height(5.dp))
            Button(
                onClick = {
                    navController.navigate("profileSk")
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
            ) {
                Text("profileScreen")
            }
            Button(
                onClick = {
                    viewModel.logout()
                    navController.navigate("welcome"){
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp)
            ) {
                Text("Logout")
            }
        }
    }
}