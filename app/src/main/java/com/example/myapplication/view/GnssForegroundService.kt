package com.example.myapplication.view

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.GnssMeasurement
import android.location.GnssMeasurementsEvent
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.Timer
import java.util.TimerTask

class GnssForegroundService : Service() {

    private lateinit var locationManager: LocationManager
    private val channelId = "gnss_service_channel"
    var latestMeasurements: List<GnssMeasurement> = emptyList()
    private val gnssCallback =
        object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(eventArgs: GnssMeasurementsEvent) {
                latestMeasurements = eventArgs.measurements as List<GnssMeasurement>

                for (m in eventArgs.measurements) {
                    m.constellationType
                    m.cn0DbHz
                    m.svid
                    m.receivedSvTimeNanos
                    m.carrierFrequencyHz
                    m.multipathIndicator
                    m.pseudorangeRateMetersPerSecond
                    m.carrierPhase
                    Log.d(
                        "GNSS_RAW",
                        "svid=${m.svid}, cn0=${m.cn0DbHz}, adr=${m.accumulatedDeltaRangeMeters}"
                    )
                }
            }
        }
    val timer = Timer()

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.registerGnssMeasurementsCallback(
            gnssCallback,
            Handler(Looper.getMainLooper())
        )

        Log.d("GNSS_RAW", "Foreground GNSS service started")

        timer.schedule(object : TimerTask() {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun run() {
                if (latestMeasurements.isNotEmpty()) {
                    for (m in latestMeasurements) {
                        Log.d(
                            "GNSS_TIMER", """
                    constellationType=${m.constellationType}
                    cn0DbHz=${m.cn0DbHz}
                    svid=${m.svid}
                    receivedSvTimeNanos=${m.receivedSvTimeNanos}
                    carrierFrequencyHz=${m.carrierFrequencyHz}
                    multipathIndicator=${m.multipathIndicator}
                    pseudorangeRateMetersPerSecond=${m.pseudorangeRateMetersPerSecond}
                    accumulatedDeltaRangeMeters=${m.accumulatedDeltaRangeMeters}
                    carrierPhase=${m.carrierPhase}
                    accumulatedDeltaRangeState=${m.accumulatedDeltaRangeState}
                    describeContents=${m.describeContents()}
                    automaticGainControlLevelDb=${m.automaticGainControlLevelDb}
                    accumulatedDeltaRangeUncertaintyMeters=${m.accumulatedDeltaRangeUncertaintyMeters}
                    timeOffsetNanos=${m.timeOffsetNanos}
                    state=${m.state}
                    snrInDb=${m.snrInDb}
                    satelliteInterSignalBiasUncertaintyNanos=${m.satelliteInterSignalBiasUncertaintyNanos}
                    satelliteInterSignalBiasNanos=${m.satelliteInterSignalBiasNanos}
                    fullInterSignalBiasUncertaintyNanos=${m.fullInterSignalBiasUncertaintyNanos}
                    receivedSvTimeUncertaintyNanos=${m.receivedSvTimeUncertaintyNanos}
                    fullInterSignalBiasNanos=${m.fullInterSignalBiasNanos}
                    pseudorangeRateUncertaintyMetersPerSecond=${m.pseudorangeRateUncertaintyMetersPerSecond}
                    codeType=${m.codeType}
                    carrierPhaseUncertainty=${m.carrierPhaseUncertainty}
                    carrierCycles=${m.carrierCycles}
                    basebandCn0DbHz=${m.basebandCn0DbHz}
                """.trimIndent()
                        )
                    }
                } else {
                    Log.d("GNSS_TIMER", "No GNSS data yet")
                }
            }
        }, 0, 15000) // 15000 ms = 15 ثانیه
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onDestroy() {
        super.onDestroy()
//        locationManager.unregisterGnssMeasurementsCallback(gnssCallback)
        Log.d("GNSS_RAW", "Foreground GNSS service stopped")

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.unregisterGnssMeasurementsCallback(gnssCallback)
        timer.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "GNSS Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        return Notification.Builder(this, channelId)
            .setContentTitle("GNSS Active")
            .setContentText("در حال دریافت داده‌های ماهواره‌ای...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
