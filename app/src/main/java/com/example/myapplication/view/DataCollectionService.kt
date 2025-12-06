package com.example.myapplication.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.GnssClock
import android.location.GnssMeasurementsEvent
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.app.MyApplication
import com.example.myapplication.model.InfoModel
import com.example.myapplication.repository.SensorRepository
import com.example.myapplication.viewmodel.MainActivityViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CountDownLatch

class DataCollectionService : Service() {

    private val channelId = "data_collection_service"
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: MainActivityViewModel
    private val timer = Timer()
    private var latestMeasurements: GnssMeasurementsEvent? = null
    lateinit var clock: GnssClock
    private val gnssCallback =
        object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(eventArgs: GnssMeasurementsEvent) {
                latestMeasurements = eventArgs
                clock = eventArgs.clock
                Log.d("GNSS_DATA", "Received $latestMeasurements GNSS measurements")
            }
        }

    override fun onBind(intent: Intent?) = null

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())

        viewModel = ViewModelProvider.AndroidViewModelFactory
            .getInstance(MyApplication()).create(MainActivityViewModel::class.java)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.registerGnssMeasurementsCallback(
            gnssCallback,
            Handler(Looper.getMainLooper())
        )
        SensorRepository.initialize(this)

// Start all sensors
        SensorRepository.startAccelerometerSensor()
        SensorRepository.startGyroscopeSensor()
        SensorRepository.startMagneticFieldSensor()
        SensorRepository.startPressureSensor()
        SensorRepository.startGravitySensor()

        startRepeatingTask()
    }

    private fun startRepeatingTask() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                collectAndSendData()
            }
        }, 0, 5000)
    }

    private fun collectAndSendData() {
        try {
            val fusedLoc = getLocationInfo()
            val providersLoc = getProvidersLocationInfo()
            val cellInfo = getCellInfo()
            val wifiInfo = getWifiInfo()
            val deviceInfo = getDeviceInfo()
            val gnssInfo = getGnssInfo()
            val sensorsInfo = SensorRepository.getAllSensorsDataAsJson()

            val locInfo = JSONObject()
            locInfo.put("fused", fusedLoc)
            locInfo.put("providers", providersLoc)

            val pinData = CorrectLocHolder.json()

            val inf = InfoModel(
                locInfo.toString(),
                cellInfo.toString(),
                wifiInfo.toString(),
                deviceInfo.toString(),
                gnssInfo.toString(),
                pinData,
                sensorsInfo.toString()
            )
            viewModel.getUser(inf)

            Log.d("SERVICE", "Data sent to server via ViewModel")
        } catch (e: Exception) {
            Log.e("SERVICE_ERROR", "Error collecting/sending data", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getProvidersLocationInfo(): JSONObject {
        val json = JSONObject()
        val providers = locationManager.getProviders(false)

        for (provider in providers) {
            val lastKnown = locationManager.getLastKnownLocation(provider)
            val providerJson = JSONObject()
            providerJson.put("latitude", lastKnown?.latitude ?: "")
            providerJson.put("longitude", lastKnown?.longitude ?: "")
            providerJson.put("accuracy", lastKnown?.accuracy ?: "")
            providerJson.put("speed", lastKnown?.speed ?: "")
            providerJson.put("time", lastKnown?.time ?: "")
            providerJson.put("provider", lastKnown?.provider ?: "")
            providerJson.put("extras", lastKnown?.extras ?: "")
            providerJson.put("altitude", lastKnown?.altitude ?: "")
            providerJson.put("bearing", lastKnown?.bearing ?: "")

            json.put(provider, providerJson)
        }

        return json
    }

    @SuppressLint("MissingPermission")
    private fun getLocationInfo(): JSONObject {
        val json = JSONObject()
        val latch = CountDownLatch(1)
        fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
            loc?.let {
                json.put("latitude", it.latitude ?: "")
                json.put("longitude", it.longitude ?: "")
                json.put("accuracy", it.accuracy ?: "")
                json.put("speed", it.speed ?: "")
                json.put("time", it.time ?: "")
                json.put("provider", it.provider ?: "")
                json.put("extras", it.extras ?: "")
                json.put("altitude", it.altitude ?: "")
                json.put("bearing", it.bearing ?: "")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    json.put("speedAccuracyMetersPerSecond", it.speedAccuracyMetersPerSecond)
                }
            }
            latch.countDown()
        }
        latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
        return json
    }

    @SuppressLint("MissingPermission")
    private fun getCellInfo(): JSONObject {
        val tele = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val json =
            JSONObject()
        json.put("networkOperatorName", tele.networkOperatorName ?: "")
        json.put("networkOperator", tele.networkOperator ?: "")
        json.put("simOperatorName", tele.simOperatorName ?: "")
        json.put("isNetworkRoaming", tele.isNetworkRoaming ?: "")
        json.put("networkTypeName", tele.networkType ?: "")
        json.put("simState", tele.simState ?: "")
        json.put("phoneType", tele.phoneType ?: "")
        json.put("simOperator", tele.simOperator ?: "")
        json.put("simCountryIso", tele.simCountryIso ?: "")
        // json.put("simSerialNumber",tele.simSerialNumber ?: "")
        // json.put("subscriberId",tele.subscriberId ?: "")
        json.put("callState", tele.callState ?: "")
        json.put("dataActivity", tele.dataActivity ?: "")
        json.put("dataState", tele.dataState ?: "")
        json.put("deviceSoftwareVersion", tele.deviceSoftwareVersion ?: "")
        // json.put("dataSline1Numbertate",tele.line1Number ?: "")
        // json.put("imei", tele.imei ?: "")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            json.put("callStateString", tele.callStateForSubscription ?: "")
            json.put("MccMnc", tele.carrierIdFromSimMccMnc ?: "")
            // json.put("mnc",tele.carrierIdFromSimMccMnc ?: "")
            json.put("networkType", tele.dataNetworkType ?: "")
            json.put("gsmSignalStrength", tele.signalStrength?.gsmSignalStrength ?: "")
            json.put("cellSignalStrengths", tele.signalStrength?.cellSignalStrengths ?: "")
            json.put("signalStrength", tele.signalStrength ?: "")
        }
//        json.put("allCellInfo_Unwrapped", tele.allCellInfo?.toString() ?: "")

        val jsonArray = JSONArray()
        tele.allCellInfo?.forEach { cell ->
            val c = JSONObject()
            c.put("registered", cell.isRegistered)
            c.put("type", cell::class.simpleName)

            when (cell) {
                is CellInfoLte -> {
                    val id = cell.cellIdentity
                    val ss = cell.cellSignalStrength

                    c.put("ci", id.ci)
                    c.put("pci", id.pci)
                    c.put("tac", id.tac)
                    c.put("earfcn", id.earfcn)
                    c.put("mcc", id.mcc)
                    c.put("mnc", id.mnc)
                    c.put("dbm", ss.dbm)
                    c.put("level", ss.level)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        c.put("bandwidth", id.bandwidth)
                        c.put("rssnr", ss.rssnr)
                        c.put("rsrp", ss.rsrp)
                        c.put("rsrq", ss.rsrq)
                        c.put("alphaLong", id.operatorAlphaLong)
                        c.put("alphaShort", id.operatorAlphaShort)
                    }
                }

                is CellInfoWcdma -> {
                    val id = cell.cellIdentity
                    val ss = cell.cellSignalStrength

                    c.put("lac", id.lac)
                    c.put("cid", id.cid)
                    c.put("psc", id.psc)
                    c.put("uarfcn", id.uarfcn)
                    c.put("mcc", id.mcc)
                    c.put("mnc", id.mnc)

                    c.put("dbm", ss.dbm)
                    c.put("level", ss.level)
                    c.put("asuLevel", ss.asuLevel)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        c.put("alphaLong", id.operatorAlphaLong)
                        c.put("alphaShort", id.operatorAlphaShort)
                    }
                }

                is CellInfoGsm -> {
                    val id = cell.cellIdentity
                    val ss = cell.cellSignalStrength

                    c.put("lac", id.lac)
                    c.put("cid", id.cid)
                    c.put("arfcn", id.arfcn)
                    c.put("bsic", id.bsic)
                    c.put("mcc", id.mcc)
                    c.put("mnc", id.mnc)

                    c.put("dbm", ss.dbm)
                    c.put("level", ss.level)
                }
            }

            jsonArray.put(c)
        }

        json.put("allCellInfo", jsonArray)

//        json.put("cellLocation", tele.cellLocation.toString() ?: "")

        return json
    }

    private fun getWifiInfo(): JSONObject {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val json = JSONObject()

        val scanArray = JSONArray()
        wifiManager.scanResults.forEach { r ->
            val item = JSONObject()
            item.put("SSID", r.SSID)
            item.put("BSSID", r.BSSID)
            item.put("RSSI", r.level)
            item.put("frequency", r.frequency)
            item.put("capabilities", r.capabilities)
            item.put("timestamp", r.timestamp)
            scanArray.put(item)
        }

        json.put("scanResults", scanArray)
        json.put("SSID", wifiManager.connectionInfo.ssid ?: "")
        json.put("BSSID", wifiManager.connectionInfo.bssid ?: "")
        json.put("RSSI", wifiManager.connectionInfo.rssi)
        json.put("frequency", wifiManager.connectionInfo.frequency)

        return json
    }


    private fun getDeviceInfo(): JSONObject {
        val json = JSONObject()
        json.put("Brand", Build.BRAND)
        json.put("Model", Build.MODEL)
        json.put("Device", Build.DEVICE)
        json.put("SDK", Build.VERSION.SDK_INT)
        json.put(
            "Android_ID",
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        )
        json.put("DeviceID", Settings.Secure.ANDROID_ID)
        json.put("ID", Build.ID)
        json.put("Manufacture", Build.MANUFACTURER)
        json.put("User", Build.USER)
        json.put("Type", Build.TYPE)
        json.put("Base", Build.VERSION_CODES.BASE)
        json.put("Incremental", Build.VERSION.INCREMENTAL)
        json.put("Board", Build.BOARD)
        json.put("Host", Build.HOST)
        json.put("FingerPrint", Build.FINGERPRINT)
        json.put("Version Code", Build.VERSION.RELEASE)
        return json
    }

    private fun getGnssInfo(): JSONObject {
        val data = JSONObject()
        val event = latestMeasurements

        if (event != null) {
            val clock = event.clock
            val clockData = JSONObject()
            clockData.put("timeNanos", clock?.timeNanos ?: "")
            clockData.put("fullBiasNanos", clock?.fullBiasNanos ?: "")
            clockData.put("biasNanos", clock?.biasNanos?.takeIf { !it.isNaN() } ?: 0)
            Log.d("CLOCK", "biasNanos=${clock?.biasNanos}")//todo

            clockData.put("leapSecond", clock?.leapSecond ?: "")
            data.put("clock", clockData)

            // Measurements data
            val measurementsArray = JSONArray()
            for (m in event.measurements.toList()) {
                val j = JSONObject()
                j.put("constellationType", m.constellationType ?: "")
                j.put("cn0DbHz", m.cn0DbHz ?: "")
                j.put("svid", m.svid ?: "")
                j.put("receivedSvTimeNanos", m.receivedSvTimeNanos ?: "")
                j.put("carrierFrequencyHz", m.carrierFrequencyHz ?: "")
                j.put("multipathIndicator", m.multipathIndicator ?: "")
                j.put("pseudorangeRateMetersPerSecond", m.pseudorangeRateMetersPerSecond ?: "")
                j.put("accumulatedDeltaRangeMeters", m.accumulatedDeltaRangeMeters ?: "")
                j.put("carrierPhase", m.carrierPhase.takeIf { !it.isNaN() } ?: 0)
                j.put("accumulatedDeltaRangeState", m.accumulatedDeltaRangeState ?: "")
                j.put("describeContents", m.describeContents() ?: "")
                j.put(
                    "accumulatedDeltaRangeUncertaintyMeters",
                    m.accumulatedDeltaRangeUncertaintyMeters ?: ""
                )
                j.put("timeOffsetNanos", m.timeOffsetNanos ?: "")
                j.put("state", m.state ?: "")
                j.put("snrInDb", m.snrInDb.takeIf { !it.isNaN() } ?: 0)
                j.put("receivedSvTimeUncertaintyNanos", m.receivedSvTimeUncertaintyNanos ?: "")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    j.put("automaticGainControlLevelDb", m.automaticGainControlLevelDb ?: "")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    j.put(
                        "satelliteInterSignalBiasUncertaintyNanos",
                        m.satelliteInterSignalBiasUncertaintyNanos ?: ""
                    )
                    j.put(
                        "satelliteInterSignalBiasNanos",
                        m.satelliteInterSignalBiasNanos ?: ""
                    )
                    j.put(
                        "fullInterSignalBiasUncertaintyNanos",
                        m.fullInterSignalBiasUncertaintyNanos ?: ""
                    )
                    j.put("fullInterSignalBiasNanos", m.fullInterSignalBiasNanos ?: "")
                    j.put("basebandCn0DbHz", m.basebandCn0DbHz ?: "")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    j.put("codeType", m.codeType ?: "")
                }
                measurementsArray.put(j)
            }

            data.put("measurements", measurementsArray)
        } else {
            data.put("clock", JSONObject())
            data.put("measurements", JSONArray())
        }

        return data
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Data Collection Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        return Notification.Builder(this, channelId)
            .setContentTitle("در حال ارسال داده‌ها")
            .setContentText("سرویس جمع‌آوری اطلاعات فعال است")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        locationManager.unregisterGnssMeasurementsCallback(gnssCallback)
        Log.i("SERVICE", "DataCollectionService stopped")
    }
}
