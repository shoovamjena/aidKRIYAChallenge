package com.example.aidkriyachallenge.dataModel

sealed class Screen(val route: String) {
    // Removed RoleSelection as it's no longer used
    // object RoleSelection : Screen("role_selection")

    // Renamed 'Home' to avoid conflict with your main app's home screen
    object TrackingRequest : Screen("tracking_request")

    object Map : Screen("map")
    object DestinationSelection : Screen("destination_selection")
}