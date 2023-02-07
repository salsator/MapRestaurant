package com.example.mapall

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.mapall.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okio.IOException
import org.json.JSONObject


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener {
    //proměné mapy
    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationPermissionGranted = false

    //proměné pozice
    private lateinit var lastKnownLocation: Location
    private lateinit var currentLatLng: LatLng
    private val nearbyRestaurants = mutableListOf<Restaurant>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationPermision()
        setMyLocation()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        getActualLocation()

        searchNearbyRestaurants(currentLatLng, 10000)

        map.setOnMarkerClickListener(this)

        val latLng = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
        map.addMarker(MarkerOptions().position(currentLatLng).title("Moje Poloha"))
        map.animateCamera((CameraUpdateFactory.newLatLngZoom(latLng, 15f)))



        map.uiSettings.isZoomControlsEnabled = true

        lifecycleScope.launch {

            while (nearbyRestaurants.size == 0) {
                delay(200L)
                addMarkers()
                Log.e("MainActivity", "${nearbyRestaurants.size} ")
            }
        }


    }


    private fun checkLocationPermision() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
            Log.e("MainActivity", "zjištěna permise ")
        } else {
            Log.e("MainActivity", "nezjištěna permise a požádání o ni")
            requestPermissions()
        }
    }


    @SuppressLint("MissingPermission")
    private fun getActualLocation() {
        try {
            if (locationPermissionGranted) {
                map.isMyLocationEnabled = true
                map.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map.isMyLocationEnabled = false
                map.uiSettings?.isMyLocationButtonEnabled = false
                checkLocationPermision()
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "chyba v enable location")
        }
    }


    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != 1) {
            return
        }
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun setMyLocation() {
        if (locationPermissionGranted) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastKnownLocation = location
                    currentLatLng = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)

                    val mapFragment = supportFragmentManager
                        .findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync(this)
                }
            }
        }
    }


    private fun addMarkers() {
        try {
            for (i in 0..nearbyRestaurants.size) {

                val placeLatLng = nearbyRestaurants[i].latLng
                val placeName = nearbyRestaurants[i].name
                map.addMarker(MarkerOptions().position(placeLatLng).title(placeName))
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Restaurace nepřidány na mapu")
        }
    }


    fun searchNearbyRestaurants(currentLocation: LatLng, radiusMetr: Int) {


        val types = "restaurant"
        val apiKey = BuildConfig.MAPS_API_KEY
        val radius = radiusMetr
        val request = Request.Builder()
            .url("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + currentLocation.latitude + "," + currentLocation.longitude + "&radius=" + radius + "&types=" + types + "&key=" + apiKey)
            .build()


        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "nepodařilo se požádat o místa")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body?.string()
                val jsonObject = JSONObject(responseString)
                val resultsArray = jsonObject.getJSONArray("results")

                for (i in 0 until resultsArray.length()) {
                    val result = resultsArray.getJSONObject(i)
                    val restaurantName = result.getString("name")
                    val location = result.getJSONObject("geometry").getJSONObject("location")
                    val latitude = location.getDouble("lat")
                    val longitude = location.getDouble("lng")
                    val latLng = LatLng(latitude, longitude)
                    Log.e("MainActivity", "jmeno $restaurantName, latlng $latLng")
                    val restaurant = Restaurant(restaurantName, latLng)
                    nearbyRestaurants.add(restaurant)
                }
            }

        })
    }



    override fun onMarkerClick(p0: Marker): Boolean {
      if (p0.title=="Moje Poloha")
      {
          val intent = Intent (this, LoginActivity::class.java)
          this.startActivity(intent)
      }

       return false
    }

}

