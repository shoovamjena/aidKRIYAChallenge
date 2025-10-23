package com.example.aidkriyachallenge.dataModel

data class Session(
    val walkerId: String = "",
    val companionId: String = "",
    val status: String = "", // e.g., "active", "awaiting_walk_confirmation", "walking"
    val startedAt: Long = 0,
    val endedAt: Long? = null, // Optional: useful if you keep session history
    val walkedDistance: Double = 0.0 // <-- ADD THIS field (in meters)
)