package com.example.aidkriyachallenge.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidkriyachallenge.common.ResultState
import com.example.aidkriyachallenge.common.UserPreferences
import com.example.aidkriyachallenge.dataModel.UserProfile
import com.example.aidkriyachallenge.googleauthentication.GoogleAuthClient
import com.example.aidkriyachallenge.repo.repo
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MyViewModel @Inject constructor(
    private val repo: repo,
    application: Application,
) : ViewModel() {
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)

    private val googleAuthClient = GoogleAuthClient(application.applicationContext)
    // Session flow from DataStore
    val userEmail = UserPreferences.getUserEmail(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ✅ Save email after successful login/signup
    private fun cacheUser(email: String) = viewModelScope.launch {
        UserPreferences.saveUserEmail(context, email)
    }

    // ✅ Clear email on logout
    fun logout() {
        //Resetting state
        _state.value = LoginUiState()

        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
            UserPreferences.clearUserEmail(context)
            googleAuthClient.signOut()
        }
    }

    init {
        // Initialize authentication state
        initializeAuth()
    }

    private fun initializeAuth() = viewModelScope.launch {
        try {
            // Wait for userEmail to be loaded from DataStore
            userEmail.collect { email ->
                if (email != null) {
                    // User is logged in, update state
                    _state.value = _state.value.copy(user = UserProfile(email = email))
                }
                _isInitialized.value = true
                return@collect // Stop collecting after first emit
            }
        } catch (e: Exception) {
            Log.e("MyViewModel", "Error initializing auth", e)
            _isInitialized.value = true
        }
    }


    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SignUp -> signUp(event.email, event.password)
            is AuthEvent.SignIn -> signIn(event.email, event.password)
            is AuthEvent.Google -> google(event.idToken)
            is AuthEvent.ForgotPassword -> forgotPassword(event.email)
            AuthEvent.ClearError -> _state.value = _state.value.copy(error = null)
        }

    }


    private fun signUp(email: String, password: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        when (val res = repo.SignUp(email, password)) {
            is ResultState.Success -> {_state.value =
                LoginUiState(user = res.data, isLoading = false)
                cacheUser(email)
                }

            is ResultState.Error -> _state.value =
                LoginUiState(error = res.message, isLoading = false)

            ResultState.Loading -> {}
        }
    }


    private fun signIn(email: String, password: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        when (val res = repo.SignIn(email, password)) {
            is ResultState.Success -> {
                _state.value =
                    LoginUiState(user = res.data, error = null, isLoading = false)
                cacheUser(email)
            }

            is ResultState.Error -> _state.value =
                LoginUiState(error = res.message, isLoading = false)

            ResultState.Loading -> {}
        }
    }


    private fun google(idToken: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        when (val res = repo.signInWithGoogle(idToken)) {
            is ResultState.Success ->{
                Log.d("MyViewModel", "Google sign-in success: ${res.data}")
                _state.value = LoginUiState(user = res.data, isLoading = false,error = null)
                res.data.email.let { cacheUser(it) }
            }

            is ResultState.Error ->{
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


}

sealed class AuthEvent {
    data class SignUp(val email: String, val password: String) : AuthEvent()
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