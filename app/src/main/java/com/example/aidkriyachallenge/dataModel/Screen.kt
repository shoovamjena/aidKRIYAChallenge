package com.example.aidkriyachallenge.dataModel

sealed class Screen(val route: String) {
    object Payment : Screen("payment/{distanceInMeters}/{amountInPaise}") {
        fun createRoute(distanceInMeters: Int, amountInPaise: Int) = "payment/$distanceInMeters/$amountInPaise"
    }

    object Map : Screen("map")
    object DestinationSelection : Screen("destination_selection")
    object Home : Screen("home")
}