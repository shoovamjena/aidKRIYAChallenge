package com.example.aidkriyachallenge.dummyUi

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aidkriyachallenge.dataModel.UserProfile
import com.example.aidkriyachallenge.viewmodel.MyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MyViewModel) {
    val state by viewModel.Profilestate.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val isWanderer by viewModel.userRole.collectAsState()

    var isEditing by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.onImageChanged(uri) }

    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.loadProfile()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ){
        // Username
        OutlinedTextField(
            value = state.username,
            onValueChange = { if (isEditing) viewModel.onUsernameChanged(it) },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !isEditing
        )

        // Show calculated age (always read-only, from DOB)
        state.dob?.let {
            val age = UserProfile(dob = it, isWanderer = isWanderer == true).calculateAge()
            Text("Age: $age years", style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedTextField(
            value = state.gender,
            onValueChange = { if (isEditing) viewModel.onGenderChanged(it) },
            label = { Text("Gender") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !isEditing
        )

        OutlinedTextField(
            value = state.address,
            onValueChange = { if (isEditing) viewModel.onAddressChanged(it) },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !isEditing
        )
        Log.d("ProfileScreen","$isWanderer")
        OutlinedTextField(
            value = if(isWanderer == true) "Wanderer" else "Walker",
            onValueChange = {},
            label = { Text("Role") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true
        )
        // Walking speed dropdown
        var speedExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = speedExpanded && isEditing, onExpandedChange = { speedExpanded = it }) {
            OutlinedTextField(
                value = state.walkingSpeed,
                onValueChange = {},
                label = { Text("Walking Speed") },
                readOnly = true,
                modifier = Modifier.menuAnchor()
            )
            if (isEditing) {
                ExposedDropdownMenu(expanded = speedExpanded, onDismissRequest = { speedExpanded = false }) {
                    listOf("Slow", "Medium", "Fast").forEach { speed ->
                        DropdownMenuItem(text = { Text(speed) }, onClick = {
                            viewModel.onWalkingSpeedChanged(speed)
                            speedExpanded = false
                        })
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.description,
            onValueChange = { if (isEditing) viewModel.onDescriptionChanged(it) },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !isEditing,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isEditing) {
                Button(
                    onClick = {
                        viewModel.saveProfile()
                        isEditing = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = { isEditing = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit")
                }
            }
        }
    }
}

