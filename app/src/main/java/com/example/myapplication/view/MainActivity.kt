package com.example.myapplication.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
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
import com.google.android.gms.maps.model.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var mainActivityViewModel: MainActivityViewModel
    private var currentSelectedMarker: Marker? = null
    var serverMarker: Marker? = null


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

        mainActivityViewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        mainActivityViewModel.getUserResponse().observe(this) { response ->
            try {
                if (response.getString("status") == "success") {
                    val data = response.getJSONObject("data")
                    val lat = data.getDouble("latitude")
                    val lon = data.getDouble("longitude")

                    updateMarker(lat, lon)
//                    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
//                    .format(Date(requestSentTime))
                    binding.txtLog.text =
                        response.toString() + " \nFormatted Time: " + SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                        )
                            .format(Date(response.getLong("request_time"))) + "\nTime in millis: " + response.getLong(
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


    fun updateMarker(lat: Double, lon: Double) {
        val serverLocation = LatLng(lat, lon)

        val bitmapdraw = resources.getDrawable(R.mipmap.pin) as BitmapDrawable
        val b = bitmapdraw.bitmap
        val smallMarker = b.scale(60, 100, false)

//        serverMarker.position(serverLocation)
//        mMap.clear()
//        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(serverLocation, 15f))
//        mMap.addMarker(serverMarker)
        serverMarker?.remove()

        serverMarker = mMap.addMarker(
            MarkerOptions()
                .position(serverLocation)
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
        )!!

    }

    private fun setupUi() {
        binding.imgGPS.setOnClickListener {
            if (isLocationPermissionGranted()) {
                startService()
// updateMarker(35.7676216, 51.4247806)
//                mainActivityViewModel.getUserResponse().observe(this) { response ->
//                    try {
//                        if (response.getString("status") == "success") {
//                            val data = response.getJSONObject("data")
//                            val lat = data.getDouble("latitude")
//                            val lon = data.getDouble("longitude")
//
//                            updateMarker(lat, lon)
//                        }
//                    } catch (e: Exception) {
//                        Log.e("MAP_ERROR", "Error parsing server response", e)
//                    }
//                }
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

    private fun setupMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.uiSettings.isMyLocationButtonEnabled = false

        val tehran = LatLng(35.7219, 51.3347)
        val cameraPosition = CameraPosition.Builder().target(tehran).zoom(11f).build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        mMap.isMyLocationEnabled = true
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

    private fun startService() {
        val intent = Intent(this, DataCollectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i("TAG", "onCreate: start foreground")
            startForegroundService(intent)
        } else
            startService(intent)
        Toast.makeText(this, "سرویس فعال شد", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, DataCollectionService::class.java))
    }
}
