package com.example.myapplication.model

import com.google.gson.JsonObject

data class ApiResponse(
    val status: String,
    val message: String,
    val data: JsonObject
)
