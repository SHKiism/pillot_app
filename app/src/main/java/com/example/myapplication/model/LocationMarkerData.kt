package com.example.myapplication.model

data class LocationMarkerData(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val color: Int,
    var isVisible: Boolean = true
)
