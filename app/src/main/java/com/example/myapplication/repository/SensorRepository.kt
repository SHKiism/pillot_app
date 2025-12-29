package com.example.myapplication.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.model.SensorData
import com.example.myapplication.model.SensorInfo
import org.json.JSONArray
import org.json.JSONObject

object SensorRepository {

    private const val THROTTLE_INTERVAL = 500L
    private const val CHANGE_THRESHOLD = 0.3f

    private lateinit var sensorManager: SensorManager
    private val lastUpdateTime = mutableMapOf<Int, Long>()
    private val lastValues = mutableMapOf<Int, FloatArray>()

    // LiveData instances
    private val accelerometerLiveData = MutableLiveData<SensorData>()
    private val gyroscopeLiveData = MutableLiveData<SensorData>()
    private val magneticFieldLiveData = MutableLiveData<SensorData>()
    private val pressureLiveData = MutableLiveData<SensorData>()
    private val gravityLiveData = MutableLiveData<SensorData>()
    private val linearAccelerationLiveData = MutableLiveData<SensorData>()
    private val allSensorsLiveData = MutableLiveData<List<SensorInfo>>()

    // Sensors
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magneticField: Sensor? = null
    private var pressure: Sensor? = null
    private var gravity: Sensor? = null
    private var linearAcceleration: Sensor? = null

    // Listeners
    private val accelerometerListener = createSensorListener(accelerometerLiveData)
    private val gyroscopeListener = createSensorListener(gyroscopeLiveData)
    private val magneticFieldListener = createSensorListener(magneticFieldLiveData)
    private val pressureListener = createSensorListener(pressureLiveData)
    private val gravityListener = createSensorListener(gravityLiveData)
    private val linearAccelerationListener = createSensorListener(linearAccelerationLiveData)

    fun initialize(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    private fun createSensorListener(liveData: MutableLiveData<SensorData>): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val currentTime = System.currentTimeMillis()
                val lastTime = lastUpdateTime[event.sensor.type] ?: 0L

                if (currentTime - lastTime < THROTTLE_INTERVAL) return

                val lastVal = lastValues[event.sensor.type]
                if (lastVal != null && !hasSignificantChange(event.values, lastVal)) {
                    return
                }

                val data = SensorData(
                    sensorName = event.sensor.name,
                    sensorType = event.sensor.type,
                    values = event.values.clone(),
                    accuracy = event.accuracy,
                    timestamp = event.timestamp,
                    isAvailable = true
                )
                liveData.postValue(data)

                lastUpdateTime[event.sensor.type] = currentTime
                lastValues[event.sensor.type] = event.values.clone()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    private fun hasSignificantChange(newValues: FloatArray, oldValues: FloatArray): Boolean {
        return newValues.indices.any { i ->
            kotlin.math.abs(newValues[i] - oldValues[i]) > CHANGE_THRESHOLD
        }
    }

    // Start methods
    fun startAccelerometerSensor(): LiveData<SensorData> {
        accelerometer?.let {
            sensorManager.registerListener(
                accelerometerListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        return accelerometerLiveData
    }

    fun startGyroscopeSensor(): LiveData<SensorData> {
        gyroscope?.let {
            sensorManager.registerListener(
                gyroscopeListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        return gyroscopeLiveData
    }

    fun startMagneticFieldSensor(): LiveData<SensorData> {
        magneticField?.let {
            sensorManager.registerListener(
                magneticFieldListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        return magneticFieldLiveData
    }

    fun startPressureSensor(): LiveData<SensorData> {
        pressure?.let {
            sensorManager.registerListener(
                pressureListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        return pressureLiveData
    }

    fun startGravitySensor(): LiveData<SensorData> {
        gravity?.let {
            sensorManager.registerListener(
                gravityListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        return gravityLiveData
    }

    fun startLinearAccelerationSensor(): LiveData<SensorData> {
        linearAcceleration?.let {
            sensorManager.registerListener(
                linearAccelerationListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        return linearAccelerationLiveData
    }

    val allSensorsResponse: MutableLiveData<List<SensorInfo>>
        get() = allSensorsLiveData

    fun getSensorInfo(sensor: Sensor?): SensorInfo? {
        return sensor?.let {
            SensorInfo(
                name = it.name,
                type = it.type,
                vendor = it.vendor,
                version = it.version,
                power = it.power,
                resolution = it.resolution,
                maxRange = it.maximumRange,
                minDelay = it.minDelay,
                maxDelay = it.maxDelay,
                isAvailable = true
            )
        }
    }

    fun getAccelerometerInfo(): SensorInfo? = getSensorInfo(accelerometer)
    fun getGyroscopeInfo(): SensorInfo? = getSensorInfo(gyroscope)
    fun getMagnetometerInfo(): SensorInfo? = getSensorInfo(magneticField)
    fun getBarometerInfo(): SensorInfo? = getSensorInfo(pressure)
    fun getGravityInfo(): SensorInfo? = getSensorInfo(gravity)
    fun getLinearAccelerationInfo(): SensorInfo? = getSensorInfo(linearAcceleration)

    fun getAllSensorsDataAsJson(): JSONObject {
        val json = JSONObject()

        accelerometerLiveData.value?.let {
            json.put("accelerometer", sensorDataToJson(it))
        } ?: json.put("accelerometer", JSONObject().apply { put("isAvailable", false) })

        gyroscopeLiveData.value?.let {
            json.put("gyroscope", sensorDataToJson(it))
        } ?: json.put("gyroscope", JSONObject().apply { put("isAvailable", false) })

        magneticFieldLiveData.value?.let {
            json.put("magneticField", sensorDataToJson(it))
        } ?: json.put("magneticField", JSONObject().apply { put("isAvailable", false) })

        pressureLiveData.value?.let {
            json.put("pressure", sensorDataToJson(it))
        } ?: json.put("pressure", JSONObject().apply { put("isAvailable", false) })

        gravityLiveData.value?.let {
            json.put("gravity", sensorDataToJson(it))
        } ?: json.put("gravity", JSONObject().apply { put("isAvailable", false) })

        linearAccelerationLiveData.value?.let {
            json.put("linearAcceleration", sensorDataToJson(it))
        } ?: json.put("linearAcceleration", JSONObject().apply { put("isAvailable", false) })

        return json
    }

    private fun sensorDataToJson(data: SensorData): JSONObject {
        val json = JSONObject()
        json.put("sensorName", data.sensorName)
        json.put("sensorType", data.sensorType)
        json.put("timestamp", data.timestamp)
        json.put("accuracy", data.accuracy)
        json.put("isAvailable", data.isAvailable)

        val valuesArray = JSONArray()
        data.values.forEach { valuesArray.put(it) }
        json.put("values", valuesArray)

        // Add labeled values based on sensor type
        when (data.sensorType) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION  -> {
                json.put("x", data.values.getOrNull(0) ?: 0f)
                json.put("y", data.values.getOrNull(1) ?: 0f)
                json.put("z", data.values.getOrNull(2) ?: 0f)
            }

            Sensor.TYPE_PRESSURE -> {
                json.put("pressure", data.values.getOrNull(0) ?: 0f)
            }
        }

        return json
    }

    fun startMagnetometerSensor(): LiveData<SensorData> {
        magneticField?.let {
            sensorManager.registerListener(
                magneticFieldListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        return magneticFieldLiveData
    }

    fun getAllAvailableSensors(): LiveData<List<SensorInfo>> {
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        val sensorInfoList = allSensors.map { sensor ->
            SensorInfo(
                name = sensor.name,
                type = sensor.type,
                vendor = sensor.vendor,
                version = sensor.version,
                power = sensor.power,
                resolution = sensor.resolution,
                maxRange = sensor.maximumRange,
                minDelay = sensor.minDelay,
                maxDelay = sensor.maxDelay,
                isAvailable = true
            )
        }
        allSensorsLiveData.postValue(sensorInfoList)
        return allSensorsLiveData
    }

    fun stopAllSensors() {
        sensorManager.unregisterListener(accelerometerListener)
        sensorManager.unregisterListener(gyroscopeListener)
        sensorManager.unregisterListener(magneticFieldListener)
        sensorManager.unregisterListener(pressureListener)
        sensorManager.unregisterListener(gravityListener)
        sensorManager.unregisterListener(linearAccelerationListener)
    }
}


