package com.example.myapplication.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.model.ApiResponse
import com.example.myapplication.model.InfoModel
import com.example.myapplication.retrofit.RetrofitClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object MainActivityRepository {

    val serviceSetterGetter = MutableLiveData<JSONObject>()
    fun sendInfoApiCall(inf: InfoModel): MutableLiveData<JSONObject> {

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
                val data = response.body()
                if (data != null) {
                    Log.d("DEBUG data", "Lat: ${data.data.latitude}, Lon: ${data.data.longitude}")

                    try {
                        val json = JSONObject()
                        json.put("status", data.status)

                        val dataObj = JSONObject()
                        dataObj.put("latitude", data.data.latitude)
                        dataObj.put("longitude", data.data.longitude)
                        dataObj.put("accuracy", data.data.accuracy)
                        dataObj.put("location_type", data.data.location_type)

                        json.put("data", dataObj)
                        json.put("request_time", requestTime)

                        serviceSetterGetter.postValue(json)

                    } catch (e: Exception) {
                        Log.e("DEBUG error", "Error creating JSON", e)
                    }
                } else {
                    Log.e("DEBUG error", "Response body is null")
                }
            }
        })

        return serviceSetterGetter
    }
}