package com.example.aidkriyachallenge.viewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidkriyachallenge.common.ResultState
import com.example.aidkriyachallenge.common.UserPreferences
import com.example.aidkriyachallenge.dataModel.UserProfile
import com.example.aidkriyachallenge.googleauthentication.GoogleAuthClient
import com.example.aidkriyachallenge.repo.Repo
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyViewModel (
    private val repo: Repo,
    private val userPref: UserPreferences,
    private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val googleAuthClient = GoogleAuthClient(context)
    // In MyViewModel.kt, near your other state declarations
    val isProfileComplete = userPref.isProfileComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Session flow from DataStore
    val userEmail = userPref.getUserEmail()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userId = userPref.getUserid()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userRole = userPref.getUserRole()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    // ✅ Save email after successful login/signup
    private fun cacheUser(email: String,uid: String,isWanderer: Boolean) = viewModelScope.launch {
        userPref.saveUserEmail(email)
        userPref.saveUserID(uid = uid)
        userPref.saveRole(isWanderer)
    }

    // ✅ Clear email on logout
    fun logout() {
        //Resetting state
        _state.value = LoginUiState()
        _Profilestate.value = ProfileState() // <-- Good to reset this too

        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
            userPref.clearUserEmail()
            userPref.clearUid()
            userPref.clearUserRole()
            userPref.saveProfileCompleteStatus(false) // <-- ADD THIS LINE
            googleAuthClient.signOut()
        }
    }

    init {
        // Initialize authentication state
        initializeAuth()
    }

    // In MyViewModel.kt
    private fun initializeAuth() = viewModelScope.launch {
        try {
            val email = userEmail.first()
            if (email != null) {
                // User is logged in, so we MUST load their profile
                // and wait for it to set the DataStore flag.
                Log.d("MyViewModel", "User is logged in. Loading profile...")
                val loadProfileJob = loadProfile() // 1. Get the Job
                loadProfileJob.join() // 2. WAIT for it to complete
                Log.d("MyViewModel", "Profile load complete.")

                // Also update the LoginUiState
                val uid = userId.first()
                val role = userRole.first()
                if(uid != null && role != null) {
                    _state.update { it.copy(user = UserProfile(email = email, uid = uid, isWanderer = role)) }
                }
            } else {
                Log.d("MyViewModel", "User is not logged in.")
            }
        } catch (e: Exception) {
            Log.e("MyViewModel", "Error during initializeAuth", e)
        } finally {
            // Now it's safe to unblock the splash screen.
            _isInitialized.value = true
            Log.d("MyViewModel", "Initialization finished.")
        }
    }


    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SignUp -> signUp(event.email, event.password,event.isWanderer)
            is AuthEvent.SignIn -> signIn(event.email, event.password)
            is AuthEvent.Google -> google(event.idToken)
            is AuthEvent.ForgotPassword -> forgotPassword(event.email)
            AuthEvent.ClearError -> _state.value = _state.value.copy(error = null)
        }

    }


    private fun signUp(email: String, password: String,isWanderer: Boolean) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        when (val res = repo.signUp(email, password,isWanderer)) {
            is ResultState.Success -> {
                _state.value =
                    LoginUiState(user = res.data, isLoading = false)
                cacheUser(email, uid = res.data.uid,isWanderer)
            }

            is ResultState.Error -> _state.value =
                LoginUiState(error = res.message, isLoading = false)

            ResultState.Loading -> {}
        }
    }


    private fun signIn(email: String, password: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        when (val res = repo.signIn(email, password)) {
            is ResultState.Success -> {
                _state.value =
                    LoginUiState(user = res.data, error = null, isLoading = false)
                Log.d("viewModelRole","${res.data.isWanderer}")
                cacheUser(email, uid = res.data.uid, isWanderer = res.data.isWanderer == true)
            }

            is ResultState.Error -> _state.value =
                LoginUiState(error = res.message, isLoading = false)

            ResultState.Loading -> {}
        }
    }


    private fun google(idToken: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        when (val res = repo.signInWithGoogle(idToken)) {
            is ResultState.Success -> {
                Log.d("MyViewModel", "Google sign-in success: ${res.data}")
                _state.value = LoginUiState(user = res.data, isLoading = false, error = null)
                res.data.let { cacheUser(it.email, uid = it.uid, isWanderer = it.isWanderer == true) }
            }

            is ResultState.Error -> {
                Log.e("MyViewModel", "Google sign-in failed: ${res.message}")
                _state.value = _state.value.copy(error = res.message, isLoading = false)
            }

            ResultState.Loading -> {}
        }
    }


    private fun forgotPassword(email: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        when (val res = repo.forgotPassword(email)) {
            is ResultState.Success -> _state.value =
                _state.value.copy(isLoading = false, error = res.data) // show success as a message
            is ResultState.Error -> _state.value =
                _state.value.copy(isLoading = false, error = res.message)

            ResultState.Loading -> {}
        }
    }

    //ProfileViewModel
    private val _Profilestate = MutableStateFlow(ProfileState())
    val Profilestate = _Profilestate.asStateFlow()


    fun onUsernameChanged(value: String) { _Profilestate.update { it.copy(username = value) } }
    fun onDobChanged(value: Long) { _Profilestate.update { it.copy(dob = value) } }
    fun onGenderChanged(value: String) { _Profilestate.update { it.copy(gender = value) } }
    fun onAddressChanged(value: String) { _Profilestate.update { it.copy(address = value) } }

    fun onWalkingSpeedChanged(value: String) { _Profilestate.update { it.copy(walkingSpeed = value) } }
    fun onDescriptionChanged(value: String) { _Profilestate.update { it.copy(description = value) } }
    fun onImageChanged(value: Uri?) { _Profilestate.update { it.copy(imageUri = value) } }

    fun saveProfile() = viewModelScope.launch {
        // ... (your existing code to get uid, email, role, and build the profile object)
        val uid = userId.firstOrNull() ?: return@launch
        val email = userEmail.firstOrNull() ?: ""
        val role = userRole.firstOrNull()?:return@launch
        var imageUrl = ""
        val profile = UserProfile(
            // ... (all profile fields)
        )

        when (val result = repo.saveUserProfile(profile)) {
            is ResultState.Success -> {
                Toast.makeText(context, result.data, Toast.LENGTH_SHORT).show()

                // --- ADD THIS BLOCK ---
                // After saving, update the DataStore flag
                val isComplete = Profilestate.value.username.isNotBlank() &&
                        Profilestate.value.gender.isNotBlank() &&
                        Profilestate.value.walkingSpeed.isNotBlank()
                userPref.saveProfileCompleteStatus(isComplete)
                // --- END OF BLOCK ---
            }
            is ResultState.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
            is ResultState.Loading -> {
                // optional loading state
            }
        }
    }

    // In MyViewModel.kt
    fun loadProfile(): Job { // <-- 1. Change return type to Job
        return viewModelScope.launch { // <-- 2. Return the launch block
            val uid = userId.firstOrNull() ?: return@launch
            val role = userRole.firstOrNull() ?:return@launch

            var isComplete = false // Default to false
            when (val result = repo.getUserProfile(uid,role)) {
                is ResultState.Success -> {
                    result.data?.let { profile ->
                        _Profilestate.update {
                            it.copy(
                                username = profile.username,
                                dob = profile.dob,
                                gender = profile.gender,
                                address = profile.address,
                                walkingSpeed = profile.walkingSpeed,
                                description = profile.description,
                                imageUri = profile.imageUrl.toUri(),
                                // Your stats fields...
                                totalDistance = profile.totalDistance,
                                totalCalories = profile.totalCalories,
                                totalSteps = profile.totalSteps,
                                lastWalkTimestamp = profile.lastWalkTimestamp
                            )
                        }
                        // --- 3. CHECK COMPLETENESS ---
                        isComplete = profile.username.isNotBlank() &&
                                profile.gender.isNotBlank() &&
                                profile.walkingSpeed.isNotBlank()
                    }
                }
                is ResultState.Error -> {
                    Log.e("ProfileViewModel", "Failed to load profile: ${result.message}")
                }
                is ResultState.Loading -> {}
            }

            // --- 4. SAVE FLAG TO DATASTORE ---
            userPref.saveProfileCompleteStatus(isComplete)
        }
    }


}

sealed class AuthEvent {
    data class SignUp(val email: String, val password: String,val isWanderer: Boolean) : AuthEvent()
    data class SignIn(val email: String, val password: String) : AuthEvent()
    data class Google(val idToken: String) : AuthEvent()
    data object ClearError : AuthEvent()
    data class ForgotPassword(val email: String) : AuthEvent()
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val user: UserProfile? = null,
    var error: String? = null
)

data class ProfileState(
    val username: String = "",
    val dob: Long? = null,
    val gender: String = "",
    val address: String = "",
    val walkingSpeed: String = "",
    val description: String = "",
    val imageUri: Uri? = null,
    val totalDistance: Double = 0.0,
    val totalCalories: Int = 0,
    val totalSteps: Int = 0,
    val lastWalkTimestamp: Long? = null)