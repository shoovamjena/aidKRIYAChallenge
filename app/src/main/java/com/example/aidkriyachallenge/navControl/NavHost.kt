package com.example.aidkriyachallenge.navControl

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aidkriyachallenge.common.UserPreferences
import com.example.aidkriyachallenge.dataModel.Screen
import com.example.aidkriyachallenge.dummyUi.ReviewScreen
import com.example.aidkriyachallenge.dummyUi.ReviewSubmitScreen
import com.example.aidkriyachallenge.googleauthentication.GoogleAuthClient
import com.example.aidkriyachallenge.view.screens.DestinationSelectionScreen
import com.example.aidkriyachallenge.view.screens.HomeScreen
import com.example.aidkriyachallenge.view.screens.MapScreen
import com.example.aidkriyachallenge.view.screens.ProfileScreen
import com.example.aidkriyachallenge.view.screens.SplashScreen
import com.example.aidkriyachallenge.view.screens.WelcomeScreen
import com.example.aidkriyachallenge.viewModel.AuthEvent
import com.example.aidkriyachallenge.viewModel.MainViewModel
import com.example.aidkriyachallenge.viewModel.MyViewModel
import com.example.aidkriyachallenge.viewModel.ReviewViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun AppNavHost(
    viewModel: MyViewModel,
    reviewViewModel: ReviewViewModel,
    mapRoutingViewModel: MainViewModel
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val userid by viewModel.userId.collectAsState()
    val userrole by viewModel.userRole.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val googleAuthClient = remember { GoogleAuthClient(context) }

    // --- 1. COLLECT THE SESSION ID STATE ---
    val sessionId by mapRoutingViewModel.sessionId.collectAsState()
    val isViewModelInitialized by viewModel.isInitialized.collectAsState()
    val isProfileComplete by viewModel.isProfileComplete.collectAsState()

    // --- 2. ADD THE NAVIGATION TRIGGER ---
    // This LaunchedEffect will run every time the 'sessionId' changes.
    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            // We got a new session ID, so navigate to the map!
            Log.d("AppNavHost", "Session ID detected: $sessionId. Navigating to map...")
            navController.navigate(Screen.Map.route) {
                // Clear the back stack so the user can't go back
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    // A scope is needed to run DataStore checks in the onFinished lambda
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = "splash",
    ) {
        composable("splash") {
            SplashScreen(
                onFinished = {
                    scope.launch {
                        // 2. Wait for VM to finish initializing
                        viewModel.isInitialized.first { it == true }

                        if (userEmail != null) {
                            // ... (Your session check logic is correct)
                            val prefs = UserPreferences(context)
                            val sessionInfo: Pair<String?, String?> = prefs.getSessionInfo().first()
                            val savedSessionId = sessionInfo.first
                            val savedRequestId = sessionInfo.second

                            if (savedSessionId != null && savedRequestId != null) {
                                // ... (Navigate to Map)
                            }
                            else {
                                // --- 3. THIS IS THE RELIABLE CHECK ---
                                // We check the flag from DataStore, which is
                                // now guaranteed to be correct.
                                if (isProfileComplete) {
                                    navController.navigate("home") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("profileSk") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                // --- END OF NEW CHECK ---
                            }
                        } else {
                            // No user email, go to welcome
                            navController.navigate("welcome") {
                                popUpTo(0) { inclusive = true }
                            }
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
                    viewModel.onEvent(AuthEvent.SignIn(email, password))
                },
                onSignUp = { email, password, isWanderer ->
                    viewModel.onEvent(AuthEvent.SignUp(email, password, isWanderer))
                },
                googleAuthClient = googleAuthClient,
                viewModel = viewModel,
                onForgotPassword = { email ->
                    viewModel.onEvent(AuthEvent.ForgotPassword(email))
                },
            )
        }
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                mapRoutingViewModel = mapRoutingViewModel,
                navController = navController
            )
        }

        composable(route = "profileSk") {
            ProfileScreen(viewModel, navController)
        }

        composable("ReviewSubmit") {
            ReviewSubmitScreen(
                walkid = "Walk1s3", // Corrected the walkid
                isWanderer = userrole!!,
                userid = userid!!,
                reviewingId = "6EbQZHz17IUchzgmD9JbCP0fcit2",
                viewModel = reviewViewModel
            )
        }
        composable("ReviewSeen") {

            ReviewScreen(userId = userid!!, isWanderer = userrole!!, viewModel = reviewViewModel)

        }

        // --- MAP ROUTING SCREENS ---


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
                    // --- MODIFIED ---
                    // Now, we send the result back to the "home" screen's state handle
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("destination_location", latLng)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                viewModel = mapRoutingViewModel,
                navController = navController
            )
        }
    }
}