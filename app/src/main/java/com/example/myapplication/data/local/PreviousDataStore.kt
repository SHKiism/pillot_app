package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences

object PreviousDataStore {

    private const val PREF_NAME = "sensor_previous_data"
    private const val KEY_LAST_PAYLOAD = "last_payload"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, json: String) {
        prefs(context).edit()
            .putString(KEY_LAST_PAYLOAD, json)
            .apply()
    }

    fun load(context: Context): String? {
        return prefs(context).getString(KEY_LAST_PAYLOAD, null)
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
