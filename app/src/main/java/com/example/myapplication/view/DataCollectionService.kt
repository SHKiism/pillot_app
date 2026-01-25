package com.example.myapplication.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.GnssClock
import android.location.GnssMeasurementsEvent
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.telephony.CellInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.app.MyApplication
import com.example.myapplication.data.local.PreviousDataStore
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
    private var intervalSeconds: Int = 5
    private val gnssCallback =
        object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(eventArgs: GnssMeasurementsEvent) {
                latestMeasurements = eventArgs
                clock = eventArgs.clock
                Log.d("GNSS_DATA", "Received $latestMeasurements GNSS measurements")
            }
        }

    override fun onBind(intent: Intent?) = null

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
        SensorRepository.startLinearAccelerationSensor()
        SensorRepository.startRotationVectorSensor()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get interval from intent, default to 5 if not provided
        intervalSeconds = intent?.getIntExtra("INTERVAL_SECONDS", 5) ?: 5

        // Ensure minimum 5 seconds
        if (intervalSeconds < 5) {
            intervalSeconds = 5
        }

        Log.d("SERVICE", "Starting service with interval: $intervalSeconds seconds")

        // Cancel any existing timer and start new one with updated interval
//        timer?.cancel()
//        timer = null
        startRepeatingTask()

        return START_STICKY
    }

    private fun startRepeatingTask() {
        val intervalMillis = intervalSeconds * 1000L

        timer?.schedule(object : TimerTask() {
            override fun run() {
                collectAndSendData()
            }
        }, 0, intervalMillis)

        Log.d("SERVICE", "Repeating task started with ${intervalSeconds}s interval")
    }

    private fun collectAndSendData() {
        try {
            val fusedLoc = getLocationInfo()
            val providersLoc = getProvidersLocationInfo()
            val cellInfo = getCellTowerInfoDetailed()
            val wifiInfo = getWifiInfo()
            val deviceInfo = getDeviceInfo()
            val gnssInfo = getGnssInfo()
            val sensorsInfo = SensorRepository.getAllSensorsDataAsJson()

            val locInfo = JSONObject()
            locInfo.put("fused", fusedLoc)
            locInfo.put("providers", providersLoc)

            val pinData = CorrectLocHolder.json()

            val currentJson = JSONObject().apply {
                put("sensorsInfo", sensorsInfo)
                put("location", locInfo)
                put("timestamp", System.currentTimeMillis())
            }

            val previousJsonString = PreviousDataStore.load(this)

            val infoModel = InfoModel(
                location = locInfo.toString(),
                cellTowerInfo = cellInfo.toString(),
                availableWifi = wifiInfo.toString(),
                deviceInfo = deviceInfo.toString(),
                gnssInfo = gnssInfo.toString(),
                correctLoc = pinData,
                sensorsInfo = sensorsInfo.toString(),
                previousData = previousJsonString
            )

            viewModel.getUser(infoModel)

            PreviousDataStore.save(this, currentJson.toString())

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
    private fun getCellTowerInfoDetailed(): JSONObject {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val rootJson = JSONObject()
        val cellInfoArray = JSONArray()

        val sm = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val activeSubs = sm.activeSubscriptionInfoList
        if (activeSubs != null && activeSubs.isNotEmpty()) {
            activeSubs.forEach { sub ->
                val tmSub = tm.createForSubscriptionId(sub.subscriptionId)
                tmSub.allCellInfo?.forEach { info ->
                    val cell = processCell(info)
                    cell.put("simSlot", sub.simSlotIndex)
                    cellInfoArray.put(cell)
                }
            }
        } else {
            tm.allCellInfo?.forEach { info -> cellInfoArray.put(processCell(info)) }
        }

        rootJson.put("allCellInfo", cellInfoArray)
        rootJson.put("networkOperatorName", tm.networkOperatorName)
        return rootJson
    }

    private fun processCell(info: CellInfo): JSONObject {
        val cell = JSONObject()
        cell.put("type", info.javaClass.simpleName)
        cell.put("registered", info.isRegistered)

        try {
            val identity = info.javaClass.getMethod("getCellIdentity").invoke(info)
            val signal = info.javaClass.getMethod("getCellSignalStrength").invoke(info)

            val idFields = listOf(
                "getCi",
                "getAdditionalPlmns",
                "getBands",
                "getBsic",
                "getCid",
                "getPci",
                "getTac",
                "getLac",
                "getEarfcn",
                "getUarfcn",
                "getArfcn",
                "getNci",
                "getNrarfcn",
                "getMccString",
                "getMncString",
                "getMcc",
                "getMnc",
                "getOperatorAlphaLong",
                "getOperatorAlphaShort"
            )
            idFields.forEach { name ->
                try {
                    val value = identity?.javaClass?.getMethod(name)?.invoke(identity)
                    if (value != null) {
                        val key = name.removePrefix("get").lowercase()

                        val jsonValue = when (value) {
                            is IntArray -> JSONArray(value.toList())      // getBands()
                            is Set<*> -> JSONArray(value.toList())        // getAdditionalPlmns()
                            else -> value
                        }

                        cell.put(key, jsonValue)
                    }
                } catch (e: Exception) {
                    print(e)
                }
            }

            val sigFields = listOf(
                "getDbm",
                "getCqiTableIndex",
                "getRsrp",
                "getRsrq",
                "getRssnr",
                "getCsiRsrp",
                "getSsRsrp",
                "getTimingAdvance",
                "getRssi",
                "getRscp",
                "getEcno",
                "getCqi",
                "getAsuLevel",
                "getLevel"
            )
            sigFields.forEach { name ->
                try {
                    val value = signal?.javaClass?.getMethod(name)?.invoke(signal)
                    if (value != null) {
                        val key = name.removePrefix("get").lowercase()
                        cell.put(key, value)
                    }
                } catch (e: Exception) {
                    print(e)
                }
            }
        } catch (e: Exception) {
            print(e)
        }

        return cell
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
            item.put("timestamp", System.currentTimeMillis())
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

            clockData.put("leapSecond", clock?.leapSecond ?: "")
            data.put("clock", clockData)

            // Measurements data
            val measurementsArray = JSONArray()
            for (m in event.measurements.toList()) {
                val j = JSONObject()
                j.put("constellationType", m.constellationType ?: "")
                j.put("cn0DbHz", m.cn0DbHz.takeIf { !it.isNaN() } ?: 0)
                j.put("svid", m.svid ?: "")
                j.put("receivedSvTimeNanos", m.receivedSvTimeNanos ?: "")
                j.put("carrierFrequencyHz", m.carrierFrequencyHz.takeIf { !it.isNaN() } ?: 0)
                j.put("multipathIndicator", m.multipathIndicator ?: "")
                j.put(
                    "pseudorangeRateMetersPerSecond",
                    m.pseudorangeRateMetersPerSecond.takeIf { !it.isNaN() } ?: 0)
                j.put(
                    "accumulatedDeltaRangeMeters",
                    m.accumulatedDeltaRangeMeters.takeIf { !it.isNaN() } ?: 0)
                j.put("carrierPhase", m.carrierPhase.takeIf { !it.isNaN() } ?: 0)
                j.put("accumulatedDeltaRangeState", m.accumulatedDeltaRangeState ?: "")
                j.put("describeContents", m.describeContents() ?: "")
                j.put(
                    "accumulatedDeltaRangeUncertaintyMeters",
                    m.accumulatedDeltaRangeUncertaintyMeters.takeIf { !it.isNaN() } ?: 0
                )
                j.put("timeOffsetNanos", m.timeOffsetNanos.takeIf { !it.isNaN() } ?: 0)
                j.put("state", m.state ?: "")
                j.put("snrInDb", m.snrInDb.takeIf { !it.isNaN() } ?: 0)
                j.put("receivedSvTimeUncertaintyNanos", m.receivedSvTimeUncertaintyNanos ?: "")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    j.put(
                        "automaticGainControlLevelDb",
                        m.automaticGainControlLevelDb.takeIf { !it.isNaN() } ?: 0)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    j.put(
                        "satelliteInterSignalBiasUncertaintyNanos",
                        m.satelliteInterSignalBiasUncertaintyNanos.takeIf { !it.isNaN() } ?: 0
                    )
                    j.put(
                        "satelliteInterSignalBiasNanos",
                        m.satelliteInterSignalBiasNanos.takeIf { !it.isNaN() } ?: 0
                    )
                    j.put(
                        "fullInterSignalBiasUncertaintyNanos",
                        m.fullInterSignalBiasUncertaintyNanos.takeIf { !it.isNaN() } ?: 0
                    )
                    j.put(
                        "fullInterSignalBiasNanos",
                        m.fullInterSignalBiasNanos.takeIf { !it.isNaN() } ?: 0)
                    j.put("basebandCn0DbHz", m.basebandCn0DbHz.takeIf { !it.isNaN() } ?: 0)
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
        timer?.cancel()
        locationManager.unregisterGnssMeasurementsCallback(gnssCallback)
        Log.i("SERVICE", "DataCollectionService stopped")
    }
}
