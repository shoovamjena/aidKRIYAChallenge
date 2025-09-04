package com.example.aidkriyachallenge.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidkriyachallenge.common.ResultState
import com.example.aidkriyachallenge.dataModel.UserProfile
import com.example.aidkriyachallenge.repo.repo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MyviewModel @Inject constructor(
    private val repo: repo
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUIstate())
    val state: StateFlow<LoginUIstate> = _state.asStateFlow()


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
            is ResultState.Success -> _state.value =
                LoginUIstate(user = res.data, isLoading = false)

            is ResultState.Error -> _state.value =
                LoginUIstate(error = res.message, isLoading = false)

            ResultState.Loading -> {}
        }
    }


    private fun signIn(email: String, password: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        when (val res = repo.SignIn(email, password)) {
            is ResultState.Success -> _state.value =
                LoginUIstate(user = res.data, error = null, isLoading = false)

            is ResultState.Error -> _state.value =
                LoginUIstate(error = res.message, isLoading = false)

            ResultState.Loading -> {}
        }
    }


    private fun google(idToken: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        when (val res = repo.signInWithGoogle(idToken)) {
            is ResultState.Success ->{
                Log.d("MyViewModel", "Google sign-in success: ${res.data}")
                _state.value = LoginUIstate(user = res.data, isLoading = false,error = null)
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

data class LoginUIstate(
    val isLoading: Boolean = false,
    val user: UserProfile? = null,
    val error: String? = null
)