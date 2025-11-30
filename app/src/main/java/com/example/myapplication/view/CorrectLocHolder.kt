package com.example.myapplication.view

import org.json.JSONObject

object CorrectLocHolder {

    var lat: Double? = null
    var lon: Double? = null

    fun setLocation(lat: Double, lon: Double) {
        this.lat = lat
        this.lon = lon
    }

    fun clear() {
        lat = null
        lon = null
    }

    fun json(): String {
        return if (lat == null || lon == null) {
            "{}"
        } else {
            val obj = JSONObject()
            obj.put("latitude", lat)
            obj.put("longitude", lon)
            obj.toString()
        }
    }
}
