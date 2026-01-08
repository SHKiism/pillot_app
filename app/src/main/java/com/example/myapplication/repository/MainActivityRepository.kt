package com.example.myapplication.repository

import android.util.Log
import com.example.myapplication.model.ApiResponse
import com.example.myapplication.model.InfoModel
import com.example.myapplication.retrofit.RetrofitClient
import com.example.myapplication.utils.SingleLiveEvent
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object MainActivityRepository {

    val serviceSetterGetter = SingleLiveEvent<JSONObject>()
    fun sendInfoApiCall(inf: InfoModel): SingleLiveEvent<JSONObject> {

        val call = RetrofitClient.apiInterface.sendInfo(inf)
        val requestTime = System.currentTimeMillis()

        call.enqueue(object : Callback<ApiResponse> {
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.v("DEBUG onFailure: ", t.message.toString())
            }

            override fun onResponse(
                call: Call<ApiResponse>,
                response: Response<ApiResponse>
            ) {
                Log.v("DEBUG onResponse", response.body().toString())
                Log.e("DEBUG ERROR BODY", response.errorBody()?.string().toString())
                val body = response.body()
                if (body == null) {
                    Log.e("DEBUG", "Response body is null")
                    return
                }

                try {
                    val json = JSONObject()
                    json.put("status", body.status)
                    json.put("data", JSONObject(body.data.toString()))
                    json.put("request_time", requestTime)

                    serviceSetterGetter.postValue(json)

                } catch (e: Exception) {
                    Log.e("DEBUG error", "Error creating JSON", e)
                }
            }
        })

        return serviceSetterGetter
    }
}