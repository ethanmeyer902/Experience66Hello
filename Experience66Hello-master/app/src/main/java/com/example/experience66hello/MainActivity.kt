package com.example.experience66hello

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import android.graphics.BitmapFactory
import android.graphics.Color
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.plugin.annotation.annotations

class MainActivity : ComponentActivity() {

    private lateinit var mapView: MapView

    // Request location permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                enableUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create and display MapView
        mapView = MapView(this)
        setContentView(mapView)

        // Set camera near Jack Rabbit area
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(-110.1042, 35.0245)) // Arizona
                .zoom(10.0)
                .build()
        )

        // Load Mapbox style and initialize features
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
            addJackRabbitMarker()
            requestLocationPermission()
        }
    }

    // Request location permission
    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            enableUserLocation()
        }
    }

    // Enable blue dot + draw circle around user location
    private fun enableUserLocation() {
        val locationComponent = mapView.location
        locationComponent.updateSettings {
            enabled = true
            locationPuck = LocationPuck2D(
                topImage = BitmapFactory.decodeResource(
                    resources,
                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_puck_icon // blue dot
                ) as ImageHolder?
            )
        }

        // When location updates, draw geofence circle
        locationComponent.addOnIndicatorPositionChangedListener { point ->
            drawUserGeofence(point)
        }
    }

    // Draw a blue circle (geofence) around user's current location
    private fun drawUserGeofence(userLocation: Point) {
        val circleManager = mapView.annotations.createCircleAnnotationManager()

        val circleOptions = CircleAnnotationOptions()
            .withPoint(userLocation)
            .withCircleRadius(150.0) // meters
            .withCircleColor("#ADD8E6") // blue
            .withCircleOpacity(0.3)

        circleManager.deleteAll()
        circleManager.create(circleOptions)
    }

    // Add red marker for Jack Rabbit Trading Post
    private fun addJackRabbitMarker() {
        val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        val jackRabbit = Point.fromLngLat(-110.1042, 35.0245)

        val redMarkerBitmap = BitmapFactory.decodeResource(resources, R.drawable.red_marker)

        val marker = PointAnnotationOptions()
            .withPoint(jackRabbit)
            .withIconImage(redMarkerBitmap)
            .withTextField("Jack Rabbit Trading Post")

        pointAnnotationManager.create(marker)
    }

    // Lifecycle management
    override fun onStart() {
        super.onStart()
        if (::mapView.isInitialized) mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        if (::mapView.isInitialized) mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::mapView.isInitialized) mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapView.isInitialized) mapView.onDestroy()
    }
}
