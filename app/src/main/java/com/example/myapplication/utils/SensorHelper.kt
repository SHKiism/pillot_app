package com.example.myapplication.utils

import android.hardware.Sensor

object SensorHelper {

    val SENSOR_TYPE_NAMES = mapOf(
        Sensor.TYPE_ACCELEROMETER to "Accelerometer",
        Sensor.TYPE_GYROSCOPE to "Gyroscope",
        Sensor.TYPE_MAGNETIC_FIELD to "Magnetometer",
        Sensor.TYPE_PRESSURE to "Barometer",
        Sensor.TYPE_GRAVITY to "Gravity",
        Sensor.TYPE_LIGHT to "Light",
        Sensor.TYPE_PROXIMITY to "Proximity",
        Sensor.TYPE_AMBIENT_TEMPERATURE to "Temperature",
        Sensor.TYPE_RELATIVE_HUMIDITY to "Humidity",
        Sensor.TYPE_LINEAR_ACCELERATION to "Linear Acceleration",
        Sensor.TYPE_ROTATION_VECTOR to "Rotation Vector",
        Sensor.TYPE_STEP_COUNTER to "Step Counter",
        Sensor.TYPE_STEP_DETECTOR to "Step Detector",
        Sensor.TYPE_HEART_RATE to "Heart Rate",
        Sensor.TYPE_GAME_ROTATION_VECTOR to "Game Rotation Vector",
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR to "Geomagnetic Rotation Vector"
    )
}
