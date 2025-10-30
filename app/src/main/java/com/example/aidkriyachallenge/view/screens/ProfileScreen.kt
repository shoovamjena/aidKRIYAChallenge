package com.example.aidkriyachallenge.view.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.aidkriyachallenge.R
import com.example.aidkriyachallenge.dataModel.UserProfile
import com.example.aidkriyachallenge.ui.theme.fredoka
import com.example.aidkriyachallenge.viewModel.MyViewModel

@Composable
fun ProfileScreen(viewModel: MyViewModel,
                  navController: NavController
) {
    val state by viewModel.Profilestate.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val isWanderer by viewModel.userRole.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.onImageChanged(uri) }

    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.loadProfile()
        }
    }

    val isProfileComplete = state.username.isNotBlank() &&
            state.gender.isNotBlank() &&
            state.walkingSpeed.isNotBlank()

    // Helper function to display default "-" if empty
    fun String.orDefault() = ifEmpty { "-" }

    Box(
        Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .shadow(5.dp, RoundedCornerShape(bottomEndPercent = 25, bottomStartPercent = 25))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.primaryContainer,
                        )
                    ), shape = RoundedCornerShape(bottomEndPercent = 25, bottomStartPercent = 25)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape) // Make it circular
                            .border(2.dp, Color.White, CircleShape) // Add a border
                    ) {
                        AsyncImage(
                            model = state.imageUri, // <-- Only pass the URI here
                            error = painterResource(id = R.drawable.profile), // <-- Use 'error' for the default
                            placeholder = painterResource(id = R.drawable.profile), // Optional: Show default while loading
                            contentDescription = "Profile Image",
                            modifier = Modifier.fillMaxSize(), // Fill the Box
                            contentScale = ContentScale.Crop // Crop to fit circle
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            state.username.orDefault(),
                            fontSize = 28.sp,
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .align(Alignment.CenterHorizontally),fontFamily = fredoka

                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .border(2.dp, Color.White, RoundedCornerShape(50))
                                    .background(Color.White.copy(0.5f))
                                    .padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    if (isWanderer == true) "Wanderer" else "Walker",fontFamily = fredoka
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .border(2.dp, Color.White, RoundedCornerShape(50))
                                    .background(Color.White.copy(0.5f))
                                    .padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    state.gender.orDefault(),fontFamily = fredoka
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .border(2.dp, Color.White, RoundedCornerShape(50))
                                    .background(Color.White.copy(0.5f))
                                    .padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    state.walkingSpeed.orDefault(),fontFamily = fredoka, maxLines = 1, autoSize = TextAutoSize.StepBased(maxFontSize = 24.sp)
                                )
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "⭐",
                            fontSize = 32.sp
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "5",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,fontFamily = fredoka
                            )
                            Text(
                                "Rating",fontFamily = fredoka
                            )
                        }
                    }


                    Column(
                        modifier = Modifier
                            .padding(horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "50 km",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,fontFamily = fredoka

                        )
                        Text(
                            "Walked",fontFamily = fredoka

                        )
                    }

                }
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
                        fontWeight = FontWeight.ExtraBold,fontFamily = fredoka
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 350.dp)
            ,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "ADDRESS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),fontFamily = fredoka

            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(25))
                    .padding(10.dp)
            ){
                Text(
                    state.address.ifEmpty { "No address given" },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(10.dp),
                    fontFamily = fredoka
                )
            }
            Text(
                "DESCRIPTION",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontFamily = fredoka
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(25))
                        .padding(10.dp)
                    ){
                Text(
                    state.description.ifEmpty { "No description available" },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(10.dp),fontFamily = fredoka
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        IconButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .padding(20.dp)
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(25))
                .align(Alignment.BottomEnd)
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit Profile"
            )
        }
        if (isProfileComplete) {
            IconButton(
                onClick = {
                    // Check if we came from 'home' or 'splash'
                    val cameFromHome = navController.previousBackStackEntry?.destination?.route == "home"
                    if (cameFromHome) {
                        // If we came from Home, just go back.
                        navController.popBackStack()
                    } else {
                        // If we came from Splash, navigate to Home and clear the stack.
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .padding(20.dp)
                    .size(100.dp) // Matched to your Edit button
                    .background(MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(25))
                    .align(Alignment.BottomStart) // Aligned to the bottom-left
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Go to Home"
                )
            }
        }
    }


    // Dialog for editing profile
    if (showDialog) {
        Dialog(onDismissRequest = {
            viewModel.loadProfile()
            showDialog = false
        }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Edit Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp),
                        fontFamily = fredoka
                    )
                    Box(
                        modifier = Modifier
                            .size(120.dp) // Larger preview in dialog
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { launcher.launch("image/*") } // Click image to change
                    ) {
                        AsyncImage(
                            model = state.imageUri ?: R.drawable.profile,
                            contentDescription = "Selected Profile Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Simple overlay to hint it's clickable
                        Box(modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.3f)))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Image",
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Button(onClick = { launcher.launch("image/*") }) {
                        Text("Change Profile Picture",fontFamily = fredoka)
                    }

                    // Username
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        label = { Text("Username",fontFamily = fredoka) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Show calculated age (always read-only, from DOB)
                    state.dob?.let {
                        val age = UserProfile(dob = it, isWanderer = isWanderer == true).calculateAge()
                        Text("Age: $age years", style = MaterialTheme.typography.bodyMedium,fontFamily = fredoka)
                    }

                    // Gender Selection - Segmented Button
                    Column {
                        Text(
                            "Gender",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp),fontFamily = fredoka
                        )
                        val genderOptions = listOf("Male", "Female", "Other")
                        val selectedGenderIndex = genderOptions.indexOf(state.gender).takeIf { it >= 0 } ?: -1

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            genderOptions.forEachIndexed { index, gender ->
                                SegmentedButton(
                                    selected = index == selectedGenderIndex,
                                    onClick = { viewModel.onGenderChanged(gender) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = genderOptions.size
                                    ), colors = SegmentedButtonDefaults.colors(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Text(gender,fontFamily = fredoka,color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.address,
                        onValueChange = { viewModel.onAddressChanged(it) },
                        label = { Text("Address",fontFamily = fredoka) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Role - Read Only Segmented Button
                    Log.d("ProfileScreen","$isWanderer")
                    Column {
                        Text(
                            "Role",
                            fontFamily = fredoka,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        val roleOptions = listOf("Walker", "Wanderer")
                        val selectedRoleIndex = if (isWanderer == true) 1 else 0

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            roleOptions.forEachIndexed { index, role ->
                                SegmentedButton(
                                    selected = index == selectedRoleIndex,
                                    onClick = { }, // Read only
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = roleOptions.size
                                    ),
                                    enabled = false, colors = SegmentedButtonDefaults.colors(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Text(role,fontFamily = fredoka)
                                }
                            }
                        }
                    }

                    // Walking Speed Selection - Segmented Button
                    Column {
                        Text(
                            "Walking Speed",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp),fontFamily = fredoka
                        )
                        val speedOptions = listOf("Slow", "Medium", "Fast")
                        val selectedSpeedIndex = speedOptions.indexOf(state.walkingSpeed).takeIf { it >= 0 } ?: -1

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            speedOptions.forEachIndexed { index, speed ->
                                SegmentedButton(
                                    selected = index == selectedSpeedIndex,
                                    onClick = { viewModel.onWalkingSpeedChanged(speed) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = speedOptions.size
                                    ), colors = SegmentedButtonDefaults.colors(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Text(speed,fontFamily = fredoka, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.description,
                        onValueChange = { viewModel.onDescriptionChanged(it) },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveProfile()
                                showDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save",fontFamily = fredoka)
                        }
                        OutlinedButton(
                            onClick = {
                                showDialog = false
                                viewModel.loadProfile()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel",fontFamily = fredoka)
                        }
                    }
                }
            }
        }
    }
}