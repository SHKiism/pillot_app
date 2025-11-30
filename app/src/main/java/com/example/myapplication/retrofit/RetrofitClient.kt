package com.example.myapplication.retrofit

import com.example.myapplication.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    const val MainServer = "http://stage.hive.dotone.ir:8004/logs/v1/api/"//todo

    val retrofitClient: Retrofit by lazy {

        val levelType: Level
        if (BuildConfig.BUILD_TYPE.contentEquals("debug"))
            levelType = Level.BODY else levelType = Level.NONE

        val logging = HttpLoggingInterceptor().apply { level = levelType }

        var okhttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request: Request = chain.request()
                    .newBuilder()
//                    .addHeader(
//                        "X-CSRFTOKEN",
//                        "FqqvjFNvJzKiFHjBZYHSxJP6C543YZaEfOpAO2lw2YfOCwEgsE8dHGq25EeFYSry"
//                    )
                    .addHeader("accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()


        Retrofit.Builder()
            .baseUrl(MainServer)
            .client(okhttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiInterface: ApiInterface by lazy {
        retrofitClient.create(ApiInterface::class.java)
    }
}