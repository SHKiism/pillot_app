package com.example.myapplication.model


data class ApiResponse(
    val status: String,
    val message: String,
    val data: LocationData
)
