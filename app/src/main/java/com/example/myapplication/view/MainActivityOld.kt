//package com.example.myapplication.view
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.content.pm.PackageManager
//import android.graphics.Color
//import android.location.Location
//import android.location.LocationManager
//import android.net.wifi.WifiManager
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.provider.Settings
//import android.telephony.TelephonyManager
//import android.util.Log
//import android.view.WindowManager
//import android.widget.Toast
//import androidx.annotation.RequiresPermission
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.core.view.WindowInsetsControllerCompat
//import androidx.lifecycle.ViewModelProvider
//import com.example.myapplication.app.MyApplication
//import com.example.myapplication.app.MyApplication.Companion.context
//import com.example.myapplication.databinding.ActivityMainBinding
//import com.example.myapplication.model.InfoModel
//import com.example.myapplication.viewmodel.MainActivityViewModel
//import com.google.android.gms.location.FusedLocationProviderClient
////import com.google.android.gms.location.LocationServices
////import com.google.android.gms.maps.CameraUpdateFactory
////import com.google.android.gms.maps.GoogleMap
////import com.google.android.gms.maps.OnMapReadyCallback
////import com.google.android.gms.maps.SupportMapFragment
////import com.google.android.gms.maps.model.CameraPosition
////import com.google.android.gms.maps.model.LatLng
//import com.google.android.material.dialog.MaterialAlertDialogBuilder
//import org.json.JSONObject
//import java.util.Timer
//import java.util.TimerTask
//
//class MainActivity2 : AppCompatActivity(), OnMapReadyCallback {
//
//    private val permission =
//        arrayOf(
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_WIFI_STATE,
//            Manifest.permission.CHANGE_WIFI_STATE,
//            Manifest.permission.READ_PHONE_STATE,
//        )
//    private lateinit var binding: ActivityMainBinding
//    lateinit var mainActivityViewModel: MainActivityViewModel
//    private lateinit var mMap: GoogleMap
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    lateinit var loc: LatLng
//    lateinit var deviceInf: JSONObject
//    lateinit var cellInf: JSONObject
//    var wifiInf = JSONObject()
//    lateinit var wifiManager: WifiManager
//    val locInfo = JSONObject()
//    var aId = ""
//
//    companion object {
//        lateinit var timer: Timer
//        fun stopGetStatus() {
//            Log.i("TAG", "stopGetStatus: ")
//            try {
//                if (this::timer.isInitialized)
//                    timer.cancel()
//            } catch (e: java.lang.Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        MyApplication.currentActivity = this
//
//        val window = window
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        window.statusBarColor = Color.WHITE
//
//        mainActivityViewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]
//
//        checkPermission()
//
//        val mapFragment =
//            supportFragmentManager.findFragmentById(com.example.myapplication.R.id.map) as SupportMapFragment
//        mapFragment.getMapAsync(this)
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//
//        if (isLocationPermissionGranted()) checkGPS() else return
//        binding.imgGPS.setOnClickListener {
//            if (isLocationPermissionGranted()) checkGPS() else return@setOnClickListener
//
//            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
//                loc = LatLng(location!!.latitude, location.longitude)
//                if ((loc.latitude == 0.0) || (loc.longitude == 0.0)) {
//                    Toast.makeText(
//                        context,
//                        "درحال دریافت موقعیت لطفا بعد از چند ثانیه مجدد امتحان کنید",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                } else {
//                    val cameraPosition = CameraPosition.Builder().target(
//                        loc
//                    ).zoom(16f).build()
//
//                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
//
//                    locInfo.put("latitude", location.latitude)
//                    locInfo.put("longitude", location.longitude)
//                    locInfo.put("provider", location.provider)
//                    locInfo.put("extras", location.extras)
//                    locInfo.put("accuracy", location.accuracy)
//                    locInfo.put("altitude", location.altitude)
//                    locInfo.put("bearing", location.bearing)
//                    locInfo.put("speed", location.speed)
//                    locInfo.put("time", location.time)
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        locInfo.put(
//                            "speedAccuracyMetersPerSecond",
//                            location.speedAccuracyMetersPerSecond
//                        )
//                        locInfo.put("bearingAccuracyDegrees", location.bearingAccuracyDegrees)
//                        locInfo.put("verticalAccuracyMeters", location.verticalAccuracyMeters)
//                    }
//
//                    startGetStatus()
//                }
//            }
//        }
//
//        deviceInf = JSONObject()
//        deviceInf.put("Brand", Build.BRAND)
//        deviceInf.put("DeviceID", Settings.Secure.ANDROID_ID)
//        deviceInf.put("Model", Build.MODEL)
//        deviceInf.put("ID", Build.ID)
//        deviceInf.put("SDK", Build.VERSION.SDK_INT)
//        deviceInf.put("Manufacture", Build.MANUFACTURER)
//        deviceInf.put("Brand", Build.BRAND)
//        deviceInf.put("User", Build.USER)
//        deviceInf.put("Type", Build.TYPE)
//        deviceInf.put("Base", Build.VERSION_CODES.BASE)
//        deviceInf.put("Incremental", Build.VERSION.INCREMENTAL)
//        deviceInf.put("Board", Build.BOARD)
//        deviceInf.put("Host", Build.HOST)
//        deviceInf.put("FingerPrint", Build.FINGERPRINT)
//        deviceInf.put("Version Code", Build.VERSION.RELEASE)
//
//        aId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
//        Log.i("", "LOOK--------------------------------------->" + aId)
//
//
//        val intent = Intent(this, GnssForegroundService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            Log.i("TAG", "onCreate: start foreground")
//            startForegroundService(intent)
//        } else
//            startService(intent)
//
//    }
//
//    fun scanSuccess() {
//        wifiInf.put("scanResults", wifiManager.scanResults.toString())
//    }
//
//    fun scanFailure() {
//        // handle failure: new scan did NOT succeed
//        // consider using old scan results: these are the OLD results!
//        wifiInf.put("scanResults", wifiManager.scanResults.toString())
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        checkPermission()
//    }
//
//    fun checkPermission() {
//        Handler().postDelayed(
//            {
//                if (Build.VERSION.SDK_INT >= 23) {
//                    if ((ContextCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.ACCESS_FINE_LOCATION
//                        ) != PackageManager.PERMISSION_GRANTED) && ActivityCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.ACCESS_COARSE_LOCATION
//                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.CHANGE_WIFI_STATE
//                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.ACCESS_WIFI_STATE
//                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.READ_PHONE_STATE
//                        ) != PackageManager.PERMISSION_GRANTED
//                    ) {
//                        ActivityCompat.requestPermissions(
//                            MyApplication.currentActivity,
//                            permission,
//                            100
//                        )
//                    }
//                }
//            }, 500
//        )
//    }
//
//    override fun onMapReady(googleMap: GoogleMap) {
//        mMap = googleMap
//        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
//        mMap.uiSettings.isMyLocationButtonEnabled = false
//
//        val tehran = LatLng(35.7219, 51.3347)
//        val cameraPosition = CameraPosition.Builder().target(tehran).zoom(11f).build()
//        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
//
//        checkPermission()
//        mMap.isMyLocationEnabled = true
//    }
//
//    private fun checkGPS() {
//        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
//
//        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            Toast.makeText(this@MainActivity2, "GPS is enable", Toast.LENGTH_LONG).show()
//        } else {
//            val locationDialog = MaterialAlertDialogBuilder(this@MainActivity2)
//            locationDialog.setTitle("Attention")
//            locationDialog.setMessage("Location settings must be enabled from the settings to use the application")
//            locationDialog.setCancelable(false)
//            locationDialog.setPositiveButton(
//                "Open settings"
//            ) { _, _ ->
//                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//                startActivity(intent)
//            }
//            locationDialog.create().show()
//        }
//    }
//
//    private fun isLocationPermissionGranted(): Boolean {
//        return if (ActivityCompat.checkSelfPermission(
//                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                this, android.Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this, permission, 109
//            )
//            checkGPS()
//            false
//        } else {
//            true
//        }
//    }
//
//    private fun startGetStatus() {
//        stopGetStatus()
//        Log.i("TAG", "startGetStatus: ")
//        try {
//            timer = Timer()
//            timer.schedule(
//                object : TimerTask() {
//                    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE])
//                    override fun run() {
//                        print("start call api")
//                        updateInf()
////                        val inf = InfoModel(locInfo, cellInf, wifiInf, deviceInf, gnssInfo)
////                        mainActivityViewModel.getUser(inf)
//                    }
//                },
//                1000,
//                15000
//            )
//        } catch (e: java.lang.Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE])
//    private fun updateInf() {
//        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//            if (location != null) {
//                val lat = location.latitude
//                val lon = location.longitude
//                locInfo.put("latitude", location.latitude)
//                locInfo.put("longitude", location.longitude)
//                locInfo.put("provider", location.provider)
//                locInfo.put("extras", location.extras)
//                locInfo.put("accuracy", location.accuracy)
//                locInfo.put("altitude", location.altitude)
//                locInfo.put("bearing", location.bearing)
//                locInfo.put("speed", location.speed)
//                locInfo.put("time", location.time)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    locInfo.put(
//                        "speedAccuracyMetersPerSecond",
//                        location.speedAccuracyMetersPerSecond
//                    )
//                    locInfo.put("bearingAccuracyDegrees", location.bearingAccuracyDegrees)
//                    locInfo.put("verticalAccuracyMeters", location.verticalAccuracyMeters)
//                }
//                Log.d("LOCATION_APP", "Location: $lat, $lon")
//            } else {
//                Log.i("TAG", "Location not available")
//            }
//        }
//
//        val telemamanger = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
//        val cell = telemamanger.allCellInfo
//        Log.i("", "LOOK--------------------------------------->" + cell)
//        cellInf = JSONObject()
//        cellInf.put("aId", aId)
//        cellInf.put("networkOperatorName", telemamanger.networkOperatorName)
//        cellInf.put("networkOperator", telemamanger.networkOperator)
//        cellInf.put("networkTypeName", telemamanger.networkType)
//        cellInf.put("isNetworkRoaming", telemamanger.isNetworkRoaming)
//        cellInf.put("simState", telemamanger.simState)
//        cellInf.put("phoneType", telemamanger.phoneType)
//        cellInf.put("simOperatorName", telemamanger.simOperatorName)
//        cellInf.put("simOperator", telemamanger.simOperator)
//        cellInf.put("simCountryIso", telemamanger.simCountryIso)
////        cellInf.put("simSerialNumber",telemamanger.simSerialNumber)
////        cellInf.put("subscriberId",telemamanger.subscriberId)
//        cellInf.put("callState", telemamanger.callState)
//        cellInf.put("dataActivity", telemamanger.dataActivity)
//        cellInf.put("dataState", telemamanger.dataState)
//        cellInf.put("deviceSoftwareVersion", telemamanger.deviceSoftwareVersion)
////        cellInf.put("dataSline1Numbertate",telemamanger.line1Number)
////        cellInf.put("imei", telemamanger.imei)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            cellInf.put("callStateString", telemamanger.callStateForSubscription)
//            cellInf.put("MccMnc", telemamanger.carrierIdFromSimMccMnc)
////            cellInf.put("mnc",telemamanger.carrierIdFromSimMccMnc)
//            cellInf.put("networkType", telemamanger.dataNetworkType)
//            cellInf.put("gsmSignalStrength", telemamanger.signalStrength?.gsmSignalStrength ?: "")
//            cellInf.put(
//                "cellSignalStrengths",
//                telemamanger.signalStrength?.cellSignalStrengths ?: ""
//            )
//            cellInf.put("signalStrength", telemamanger.signalStrength)
//        }
//        cellInf.put("allCellInfo", telemamanger.allCellInfo.toString())
//
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
//
//            val wifiScanReceiver = object : BroadcastReceiver() {
//                override fun onReceive(context: Context, intent: Intent) {
//                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
//                    if (success) {
//                        scanSuccess()
//                    } else {
//                        scanFailure()
//                    }
//                }
//            }
//
//            val intentFilter = IntentFilter()
//            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
//            context.registerReceiver(wifiScanReceiver, intentFilter)
//
//            val success = wifiManager.startScan()
//            if (!success) {
//                scanFailure()
//            }
//
//            wifiInf.put("scanResults", wifiManager.scanResults.toString())
//            wifiInf.put("RSSI", wifiManager.connectionInfo.rssi)
//            wifiInf.put("freq", wifiManager.connectionInfo.frequency)
//            wifiInf.put("SSID", wifiManager.connectionInfo.ssid)
//            wifiInf.put("BSSID", wifiManager.connectionInfo.bssid)
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        stopGetStatus()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        val window = window
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//
//        WindowInsetsControllerCompat(
//            window,
//            binding.root
//        ).isAppearanceLightStatusBars = true
//        WindowInsetsControllerCompat(
//            window,
//            binding.root
//        ).isAppearanceLightNavigationBars = true
//    }
//
//    override fun onBackPressed() {
//        if (supportFragmentManager.backStackEntryCount > 0) {
//            supportFragmentManager.popBackStack()
//        } else {
//            Toast.makeText(
//                context, "Goodbye :D", Toast.LENGTH_LONG
//            ).show()
//            super.onBackPressed()
//        }
//    }
//}
//
//
