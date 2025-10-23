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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.aidkriyachallenge.R
import com.example.aidkriyachallenge.dataModel.UserProfile
import com.example.aidkriyachallenge.viewmodel.MyViewModel

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

    // Helper function to display default "-" if empty
    fun String.orDefault() = ifEmpty { "-" }

    Box(
        Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
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
                    ) {
                        Image(
                            painter = painterResource(R.drawable.profile),
                            contentDescription = "Profile Image",

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
                                .align(Alignment.CenterHorizontally)
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
                                    if (isWanderer == true) "Wanderer" else "Walker"
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
                                    state.gender.orDefault()
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
                                    state.walkingSpeed.orDefault()
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
                            fontSize = 38.sp
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "4.5",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Rating"
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
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Walked"
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
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 300.dp)
            ,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "ADDRESS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
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
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                "DESCRIPTION",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
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
                    modifier = Modifier.padding(10.dp)
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
    }


    // Dialog for editing profile
    if (showDialog) {
        Dialog(onDismissRequest = {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Edit Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Username
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Show calculated age (always read-only, from DOB)
                    state.dob?.let {
                        val age = UserProfile(dob = it, isWanderer = isWanderer == true).calculateAge()
                        Text("Age: $age years", style = MaterialTheme.typography.bodyMedium)
                    }

                    // Gender Selection - Segmented Button
                    Column {
                        Text(
                            "Gender",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
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
                                    )
                                ) {
                                    Text(gender)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.address,
                        onValueChange = { viewModel.onAddressChanged(it) },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Role - Read Only Segmented Button
                    Log.d("ProfileScreen","$isWanderer")
                    Column {
                        Text(
                            "Role",
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
                                    enabled = false
                                ) {
                                    Text(role)
                                }
                            }
                        }
                    }

                    // Walking Speed Selection - Segmented Button
                    Column {
                        Text(
                            "Walking Speed",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
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
                                    )
                                ) {
                                    Text(speed)
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
                            Text("Save")
                        }
                        OutlinedButton(
                            onClick = { showDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}