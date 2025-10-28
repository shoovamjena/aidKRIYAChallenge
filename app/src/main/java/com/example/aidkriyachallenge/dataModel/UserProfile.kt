package com.example.aidkriyachallenge.dataModel

import java.util.Calendar

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val createdAt: Long = 0L,

    val username: String = "",
    val dob: Long? = null, // store DOB in millis
    val gender: String = "",
    val address: String = "",
    val isWanderer: Boolean? = null, // Wanderer or Walker
    val walkingSpeed: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val totalDistance: Double = 0.0,
    val totalCalories: Int = 0,
    val totalSteps: Int = 0,
    val lastWalkTimestamp: Long? = null
){
    fun calculateAge(): Int? {
        dob?.let {
            val dobCal = Calendar.getInstance().apply { timeInMillis = it }
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            return age
        }
        return null
    }
}


