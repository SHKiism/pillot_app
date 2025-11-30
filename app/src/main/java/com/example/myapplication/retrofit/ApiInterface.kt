package com.example.myapplication.retrofit

import com.example.myapplication.model.ApiResponse
import com.example.myapplication.model.InfoModel
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


interface ApiInterface {

//    @Headers(
//        "X-CSRFTOKEN: FqqvjFNvJzKiFHjBZYHSxJP6C543YZaEfOpAO2lw2YfOCwEgsE8dHGq25EeFYSry",
//        "Content-Type: application/json",
//        "accept: application/json"
//    )
    @POST("submit-data/")
    fun sendInfo(@Body infoModel: InfoModel): Call<ApiResponse>

}