package com.example.aidkriyachallenge.dataModel

data class User(
    val clientId: String = "",
    val role: String = "",
    val status: String = "",
    val lastActive: Long = 0
)