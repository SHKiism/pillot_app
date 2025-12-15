package com.example.myapplication.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.app.MyApplication
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.viewmodel.MainActivityViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.graphics.scale
import com.example.myapplication.model.LocationMarkerData
import com.example.myapplication.repository.SensorRepository
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Marker
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.createBitmap

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var mainActivityViewModel: MainActivityViewModel
    private var currentSelectedMarker: Marker? = null

    private val locationMarkers = mutableMapOf<String, Pair<Marker?, Circle?>>()
    private val locationDataList = mutableListOf<LocationMarkerData>()
    private lateinit var locationSpinner: Spinner
    private var isMapReady = false

    private val locationColors = mapOf(
        "fused_location" to Color.rgb(255, 0, 0),      // Red
        "wifi_location" to Color.rgb(255, 165, 0),     // Orange
        "network_location" to Color.rgb(255, 255, 0),  // Yellow
        "gnss_location" to Color.rgb(0, 128, 0),       // Green
        "dead_rocking_location" to Color.rgb(0, 0, 255), // Blue
        "bts_location" to Color.rgb(75, 0, 130),      // Indigo
        "gps_location" to Color.rgb(238, 130, 238),    // Violet
        "default" to Color.GRAY                         // Fallback
    )

    private val locationPinResources = mapOf(
        "fused_location" to R.mipmap.pin1,
        "wifi_location" to R.mipmap.pin2,
        "network_Location" to R.mipmap.pin3,
        "gnss_location" to R.mipmap.pin4,
        "dead_rocking_location" to R.mipmap.pin5,
        "bts_location" to R.mipmap.pin6,
        "gps_location" to R.mipmap.pin7
    )

    private val permissionList = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.READ_PHONE_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MyApplication.currentActivity = this

        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK

        // Initialize repository
        SensorRepository.initialize(this)
        mainActivityViewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        // Start sensors and observe
        startObservingSensors()

        // Display sensor info
        displaySensorInfo()

        mainActivityViewModel.getUserResponse().observe(this) { response ->
            try {
                if (response.getString("status") == "success") {
                    val data = response.getJSONObject("data")

                    parseAllLocationsDynamically(data)
                    if (isMapReady) {
                        displayAllLocations()
                    }
                    setupLocationSpinner()

                    binding.txtLog.text = "Formatted Time: " + SimpleDateFormat(
                        "HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date(response.getLong("request_time"))) + "\nTime in millis: " + response.getLong(
                        "request_time"
                    )
                }
            } catch (e: Exception) {
                Log.e("MAP_ERROR", "Error parsing server response", e)
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        CorrectLocHolder.clear()

        setupMap()
        checkPermission()
        setupUi()
    }

    private fun parseAllLocationsDynamically(data: JSONObject) {
        locationDataList.clear()

        val keys = data.keys()
        while (keys.hasNext()) {
            val key = keys.next()

            if (key.endsWith("_location") || key == "gnss_location" ||
                key == "dead_rocking_location"
            ) {
                try {
                    val locationObj = data.getJSONObject(key)

                    if (locationObj.has("latitude") && locationObj.has("longitude")) {
                        val lat = locationObj.optDouble("latitude", Double.NaN)
                        val lon = locationObj.optDouble("longitude", Double.NaN)

                        // Validate coordinates
                        if (!lat.isNaN() && !lon.isNaN() &&
                            lat != 0.0 && lon != 0.0
                        ) {

                            val accuracy = locationObj.optDouble("accuracy", 0.0)
                            val color = locationColors[key] ?: locationColors["default"]!!

                            val readableName = formatLocationName(key)

                            locationDataList.add(
                                LocationMarkerData(
                                    name = readableName,
                                    latitude = lat,
                                    longitude = lon,
                                    accuracy = accuracy,
                                    color = color
                                )
                            )

                            Log.d("LOCATION_PARSER", "Added location: $readableName ($lat, $lon)")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LOCATION_PARSER", "Error parsing location key: $key", e)
                }
            }
        }

        Log.d("LOCATION_PARSER", "Total locations parsed: ${locationDataList.size}")
    }

    private fun formatLocationName(key: String): String {
        return key
            .replace("_location", "")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                    else it.toString()
                }
            } + " Location"
    }

    private fun displayAllLocations() {
        if (!isMapReady) return

        locationMarkers.values.forEach { (marker, circle) ->
            marker?.remove()
            circle?.remove()
        }
        locationMarkers.clear()

        locationDataList.forEach { locationData ->
            if (locationData.isVisible) {
                addLocationMarker(locationData)
            }
        }

        if (::locationSpinner.isInitialized) {
            updateSpinner()
        }
    }

    private fun addLocationMarker(locationData: LocationMarkerData) {
        val position = LatLng(locationData.latitude, locationData.longitude)

        val pinIcon = getCustomMarkerIcon(locationData.name, 100, 100)

        val marker = mMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(locationData.name)
                .snippet("Accuracy: ${locationData.accuracy.toInt()}m")
                .icon(pinIcon)
        )

        // Create accuracy circle
        val circle = mMap.addCircle(
            CircleOptions()
                .center(position)
                .radius(locationData.accuracy)
                .strokeColor(locationData.color)
                .strokeWidth(2f)
                .fillColor(
                    Color.argb(
                        30, Color.red(locationData.color),
                        Color.green(locationData.color), Color.blue(locationData.color)
                    )
                )
        )

        locationMarkers[locationData.name] = Pair(marker, circle)
    }

    private fun getCustomMarkerIcon(
        locationName: String,
        width: Int,
        height: Int
    ): BitmapDescriptor {
        val matchingKey = locationPinResources.keys.find { key ->
            locationName.lowercase().contains(key.replace("_location", "").replace("_", " "))
        }

        val drawableId =
            matchingKey?.let { locationPinResources[it] } ?: R.mipmap.pin

        return try {
            val drawable = ContextCompat.getDrawable(this, drawableId)

            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val scaledBitmap = bitmap.scale(width, height, false)
                BitmapDescriptorFactory.fromBitmap(scaledBitmap)
            } else {
                val bitmap = createBitmap(width, height)
                val canvas = Canvas(bitmap)
                drawable?.setBounds(0, 0, canvas.width, canvas.height)
                drawable?.draw(canvas)
                BitmapDescriptorFactory.fromBitmap(bitmap)
            }
        } catch (e: Exception) {
            Log.e("MARKER_ERROR", "Error loading custom marker", e)
            BitmapDescriptorFactory.defaultMarker() // Fallback
        }
    }

    private fun setupLocationSpinner() {
        locationSpinner = binding.spinnerLocations

        binding.btnToggleLocation.setOnClickListener {
            val selectedPosition = locationSpinner.selectedItemPosition
            if (selectedPosition in locationDataList.indices) {
                val selectedLocation = locationDataList[selectedPosition]
                selectedLocation.isVisible = !selectedLocation.isVisible

                if (selectedLocation.isVisible) {
                    addLocationMarker(selectedLocation)
                    Toast.makeText(
                        this,
                        "${selectedLocation.name} نمایش داده شد",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    locationMarkers[selectedLocation.name]?.let { (marker, circle) ->
                        marker?.remove()
                        circle?.remove()
                    }
                    locationMarkers.remove(selectedLocation.name)
                    Toast.makeText(this, "${selectedLocation.name} پنهان شد", Toast.LENGTH_SHORT)
                        .show()
                }
                updateSpinner()
            }
        }
    }

    private fun updateSpinner() {
        val locationNames = locationDataList.map {
            "${it.name} (${if (it.isVisible) "نمایش" else "پنهان"})"
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            locationNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        locationSpinner.adapter = adapter
    }

    private fun setupUi() {
        binding.imgGPS.setOnClickListener {
            if (isLocationPermissionGranted()) {
                val interval = getValidatedInterval()
                startService(interval)
                centerMapOnCurrentLocation()
            } else {
                checkPermission()
            }
        }

        binding.imgStopGPS.setOnClickListener {
            stopService(Intent(this, DataCollectionService::class.java))
            Toast.makeText(this, "سرویس متوقف شد", Toast.LENGTH_SHORT).show()
        }

        val bitmapdraw = resources.getDrawable(R.mipmap.hover_pin) as BitmapDrawable
        val b = bitmapdraw.bitmap
        val smallMarker = b.scale(100, 100, false)

        binding.btnPinLoc.setOnClickListener {

            val center = getCenterLatLng()

            currentSelectedMarker?.remove()

            currentSelectedMarker = mMap.addMarker(
                MarkerOptions()
                    .position(center)
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
            )
            CorrectLocHolder.setLocation(center.latitude, center.longitude)

            binding.imgHoverPin.visibility = View.GONE
        }

        binding.btnUnpinLoc.setOnClickListener {
            currentSelectedMarker?.remove()
            binding.imgHoverPin.visibility = View.VISIBLE
            CorrectLocHolder.clear()
        }
    }

    private fun getValidatedInterval(): Int {
        val inputText = binding.edtInterval.text.toString()

        return try {
            val value = inputText.toIntOrNull() ?: 5

            if (value < 5) {
                Toast.makeText(
                    this,
                    "حداقل فاصله ارسال 5 ثانیه است",
                    Toast.LENGTH_SHORT
                ).show()
                binding.edtInterval.setText("5")
                5
            } else {
                value
            }
        } catch (e: Exception) {
            binding.edtInterval.setText("5")
            5
        }
    }

    private fun setupMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true

        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.uiSettings.isMyLocationButtonEnabled = false

        val tehran = LatLng(35.7219, 51.3347)
        val cameraPosition = CameraPosition.Builder().target(tehran).zoom(11f).build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        mMap.isMyLocationEnabled = true
        if (locationDataList.isNotEmpty()) {
            displayAllLocations()
        }
    }

    @SuppressLint("MissingPermission")
    private fun centerMapOnCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                val cameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .zoom(16f)
                    .build()
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            } else {
                Toast.makeText(this, "در حال دریافت موقعیت...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermission() {
        val missing = permissionList.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return permissionList.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getCenterLatLng(): LatLng {
        return mMap.cameraPosition.target
    }

    private fun startService(intervalSeconds: Int) {
        val intent = Intent(this, DataCollectionService::class.java)
        intent.putExtra("INTERVAL_SECONDS", intervalSeconds)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i("TAG", "onCreate: start foreground")
            startForegroundService(intent)
        } else
            startService(intent)
        Toast.makeText(this, "سرویس فعال شد - فاصله: $intervalSeconds ثانیه", Toast.LENGTH_SHORT)
            .show()
    }

    private fun startObservingSensors() {
        // Start and observe Accelerometer
        SensorRepository.startAccelerometerSensor().observe(this) { data ->
            data?.let {
                if (it.isAvailable) {
                    println("Accelerometer - X: ${it.values[0]}, Y: ${it.values[1]}, Z: ${it.values[2]}")
                }
            }
        }

        // Start and observe Gyroscope
        SensorRepository.startGyroscopeSensor().observe(this) { data ->
            data?.let {
                if (it.isAvailable) {
                    println("Gyroscope - X: ${it.values[0]}, Y: ${it.values[1]}, Z: ${it.values[2]}")
                }
            }
        }

        // Start and observe Magnetometer
        SensorRepository.startMagneticFieldSensor().observe(this) { data ->
            data?.let {
                if (it.isAvailable) {
                    println("Magnetometer - X: ${it.values[0]}, Y: ${it.values[1]}, Z: ${it.values[2]}")
                }
            }
        }

        // Start and observe Barometer
        SensorRepository.startPressureSensor().observe(this) { data ->
            data?.let {
                if (it.isAvailable) {
                    println("Barometer - Pressure: ${it.values[0]} hPa")
                }
            }
        }

        // Start and observe Gravity
        SensorRepository.startGravitySensor().observe(this) { data ->
            data?.let {
                if (it.isAvailable) {
                    println("Gravity - X: ${it.values[0]}, Y: ${it.values[1]}, Z: ${it.values[2]}")
                }
            }
        }

        // Get all available sensors
        SensorRepository.getAllAvailableSensors().observe(this) { sensors ->
            println("Total sensors available: ${sensors.size}")
            sensors.forEach { sensor ->
                println("Sensor: ${sensor.name}, Type: ${sensor.type}")
            }
        }
    }

    private fun displaySensorInfo() {
        SensorRepository.getAccelerometerInfo()?.let { info ->
            println("Accelerometer Info:")
            println("  Vendor: ${info.vendor}")
            println("  Max Range: ${info.maxRange}")
            println("  Resolution: ${info.resolution}")
            println("  Power: ${info.power} mA")
        }

        SensorRepository.getGyroscopeInfo()?.let { info ->
            println("Gyroscope Info:")
            println("  Vendor: ${info.vendor}")
            println("  Max Range: ${info.maxRange}")
            println("  Resolution: ${info.resolution}")
            println("  Power: ${info.power} mA")
        }

        SensorRepository.getMagnetometerInfo()?.let { info ->
            println("Magnetometer Info:")
            println("  Vendor: ${info.vendor}")
            println("  Max Range: ${info.maxRange}")
            println("  Resolution: ${info.resolution}")
            println("  Power: ${info.power} mA")
        }

        SensorRepository.getBarometerInfo()?.let { info ->
            println("Pressure Info:")
            println("  Vendor: ${info.vendor}")
            println("  Max Range: ${info.maxRange}")
            println("  Resolution: ${info.resolution}")
            println("  Power: ${info.power} mA")
        }

        SensorRepository.getGravityInfo()?.let { info ->
            println("Gravity Info:")
            println("  Vendor: ${info.vendor}")
            println("  Max Range: ${info.maxRange}")
            println("  Resolution: ${info.resolution}")
            println("  Power: ${info.power} mA")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, DataCollectionService::class.java))
        SensorRepository.stopAllSensors()
    }
}
