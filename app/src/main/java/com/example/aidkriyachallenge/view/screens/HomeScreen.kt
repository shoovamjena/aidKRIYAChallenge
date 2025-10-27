package com.example.aidkriyachallenge.view.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.aidkriyachallenge.R
import com.example.aidkriyachallenge.ui.theme.inspDoc
import com.example.aidkriyachallenge.ui.theme.odin
import com.example.aidkriyachallenge.viewModel.MainViewModel
import com.example.aidkriyachallenge.viewModel.MyViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MyViewModel,
    navController: NavController,
    mapRoutingViewModel: MainViewModel,
) {
    val state by viewModel.Profilestate.collectAsState()
    // --- Collect the role needed for MapRouting ---
    val mapRoutingUserRole by mapRoutingViewModel.userRole.collectAsState()

    Scaffold(
    ) { padding ->
        //This is the top box of the screen
        Box(
            modifier = Modifier
                .fillMaxHeight(0.3f)
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
                    navController.navigate("welcome"){
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .padding(top =85.dp)
                    .padding(40.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    .align(Alignment.TopEnd)
            ){
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
                modifier = Modifier.padding(top = 100.dp, start = 20.dp)
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
        //TODO
        //This will show scrollable card corousel showing distance walked, calories burned, steps taken and last walk (if that exists)
        //And then it will show a map that will show the current location of the device and then replace the satrt Live Tracking Session with a large button with a text "START WALK"
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


        }
    }
}