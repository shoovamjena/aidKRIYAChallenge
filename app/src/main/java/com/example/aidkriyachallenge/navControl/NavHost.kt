package com.example.aidkriyachallenge.navControl

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aidkriyachallenge.dummyui.ProfileScreen
import com.example.aidkriyachallenge.googleauthentication.GoogleAuthClient
import com.example.aidkriyachallenge.view.screens.HomeScreen
import com.example.aidkriyachallenge.view.screens.SplashScreen
import com.example.aidkriyachallenge.view.screens.WelcomeScreen
import com.example.aidkriyachallenge.viewModel.AuthEvent
import com.example.aidkriyachallenge.viewModel.MyViewModel

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun AppNavHost(viewModel: MyViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val userEmail by viewModel.userEmail.collectAsState()
    val googleAuthClient = remember { GoogleAuthClient(context) }

    NavHost(
        navController = navController,
        startDestination = "splash",
    ) {
        composable("splash") {
            SplashScreen(
                onFinished = {
                    if (userEmail!= null) {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate("welcome") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("welcome") {
            LaunchedEffect(state.user) {
                if (state.user != null) {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            WelcomeScreen(
                state = state,
                onLogin = {email, password ->
                    viewModel.onEvent(AuthEvent.SignIn(email, password))},
                onSignUp = { email, password ->
                    viewModel.onEvent(AuthEvent.SignUp(email, password))
                },
                googleAuthClient = googleAuthClient,
                viewModel = viewModel,
                onForgotPassword = {email->
                    viewModel.onEvent(AuthEvent.ForgotPassword(email))
                },
            )
        }
        composable("home") {
            HomeScreen(viewModel,navController)
        }

        composable(route = "profileSk"){
            ProfileScreen()
        }
    }
}