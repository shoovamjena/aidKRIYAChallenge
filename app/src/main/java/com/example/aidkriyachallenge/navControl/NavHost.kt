package com.example.aidkriyachallenge.navControl

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aidkriyachallenge.dataModel.Screen
import com.example.aidkriyachallenge.dummyui.ReviewScreen
import com.example.aidkriyachallenge.dummyui.ReviewSubmitScreen
import com.example.aidkriyachallenge.view.screens.ProfileScreen

import com.example.aidkriyachallenge.googleauthentication.GoogleAuthClient
import com.example.aidkriyachallenge.view.screens.DestinationSelectionScreen
import com.example.aidkriyachallenge.view.screens.HomeScreen
import com.example.aidkriyachallenge.view.screens.MapScreen
import com.example.aidkriyachallenge.view.screens.RequestScreen
import com.example.aidkriyachallenge.view.screens.SplashScreen
import com.example.aidkriyachallenge.view.screens.WelcomeScreen
import com.example.aidkriyachallenge.viewModel.MainViewModel
import com.example.aidkriyachallenge.viewModel.ReviewViewModel

import com.example.aidkriyachallenge.view.screens.ProfileScreen
import com.example.aidkriyachallenge.view.screens.SplashScreen
import com.example.aidkriyachallenge.view.screens.WelcomeScreen
import com.example.aidkriyachallenge.viewModel.ReviewViewModel
import com.example.aidkriyachallenge.viewmodel.AuthEvent
import com.example.aidkriyachallenge.viewmodel.MyViewModel
import com.google.android.gms.maps.model.LatLng

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun AppNavHost(viewModel: MyViewModel,reviewViewModel: ReviewViewModel,mapRoutingViewModel: MainViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val userid by viewModel.userId.collectAsState()
    val userrole by viewModel.userRole.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val googleAuthClient = remember { GoogleAuthClient(context) }

    val mapRoutingUserRole by mapRoutingViewModel.userRole.collectAsState()

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
                onLogin = { email, password ->
                    viewModel.onEvent(AuthEvent.SignIn(email, password))},
                onSignUp = { email, password ,isWanderer->
                    viewModel.onEvent(AuthEvent.SignUp(email, password,isWanderer))
                },
                googleAuthClient = googleAuthClient,
                viewModel = viewModel,
                onForgotPassword = {email->
                    viewModel.onEvent(AuthEvent.ForgotPassword(email))
                },
            )
        }
        composable("home") {
            HomeScreen(viewModel = viewModel,               // Pass the main MyViewModel
                mapRoutingViewModel = mapRoutingViewModel, // Pass the MapRouting ViewModel
                navController = navController)
        }

        composable(route = "profileSk"){
            ProfileScreen(viewModel,navController)
        }

        composable("ReviewSubmit") {
            ReviewSubmitScreen(
                walkid = "Walk123",
                isWanderer = userrole!!,
                userid = userid!!,
                reviewingId = "6EbQZHz17IUchzgmD9JbCP0fcit2",
                viewModel = reviewViewModel
            )
        }
        composable("ReviewSeen") {

            ReviewScreen(userId = userid!!, isWanderer = userrole!!, viewModel = reviewViewModel)

        }
        composable(
            route = "tracking_request/{role}", // e.g., use Screen.TrackingRequest.route
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "Walker"
            RequestScreen(
                viewModel = mapRoutingViewModel,
                role = role,
                navController = navController // Pass NavController for destination selection
            )
        }

        // 2. Destination Selection Screen
        composable(
            route = "${Screen.DestinationSelection.route}/{lat}/{lng}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lng") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
            DestinationSelectionScreen(
                startLocation = LatLng(lat, lng),
                onCancel = { navController.popBackStack() },
                onDestinationSelected = { latLng ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("destination_location", latLng)
                    navController.popBackStack()
                }
            )
        }

        // 3. Map Screen
        composable(Screen.Map.route) {
            MapScreen(
                viewModel = mapRoutingViewModel,
                navController = navController // Pass NavController for back navigation
            )
        }
    }
}

