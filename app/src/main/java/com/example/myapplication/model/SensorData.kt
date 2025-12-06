package com.example.myapplication.model

data class SensorData(
    val sensorName: String,
    val sensorType: Int,
    val values: FloatArray,
    val accuracy: Int,
    val timestamp: Long,
    val isAvailable: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (sensorName != other.sensorName) return false
        if (sensorType != other.sensorType) return false
        if (!values.contentEquals(other.values)) return false
        if (accuracy != other.accuracy) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sensorName.hashCode()
        result = 31 * result + sensorType
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

data class SensorInfo(
    val name: String,
    val type: Int,
    val vendor: String,
    val version: Int,
    val power: Float,
    val resolution: Float,
    val maxRange: Float,
    val minDelay: Int,
    val maxDelay: Int,
    val isAvailable: Boolean
)
