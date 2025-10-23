package com.example.aidkriyachallenge.dataModel

data class Request(
    val id: String = "",
    val walkerId: String = "",
    val companionId: String? = null,
    val status: String = "",
    val sessionId: String? = null,
    val createdAt: Long = 0,
    val lat: Double = 0.0, // <-- ADD THIS
    val lng: Double = 0.0, // <-- AND THIS
    val companionLat: Double? = null, // <-- ADD THIS
    val companionLng: Double? = null,
    val destLat: Double = 0.0, // <-- ADD THIS
    val destLng: Double = 0.0
)