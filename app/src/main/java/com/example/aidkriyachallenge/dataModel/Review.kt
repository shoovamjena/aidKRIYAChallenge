package com.example.aidkriyachallenge.dataModel

data class Review(
    val walkId: String = "",
    val reviewerId: String = "",
    val reviewText: String = "",
    val rating: Int = 0,
    val timestamp: Long = System.currentTimeMillis()

)
