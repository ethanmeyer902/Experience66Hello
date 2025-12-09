package com.example.experience66hello

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var mapView: MapView
    private lateinit var geofenceManager: GeofenceManager
    private lateinit var offlineMapManager: OfflineMapManager
    private lateinit var offlineDataCache: OfflineDataCache
    
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var circleAnnotationManager: CircleAnnotationManager? = null
    
    // Track which landmarks user is currently inside
    private val activeLandmarks = mutableSetOf<String>()
    
    // Geofence event log for demo purposes
    private val geofenceEventLog = mutableListOf<GeofenceEvent>()
    
    // UI Components for Geofence Monitor
    private lateinit var monitorPanel: LinearLayout
    private lateinit var geofenceListTextView: TextView
    private lateinit var eventLogTextView: TextView
    private lateinit var toggleButton: Button
    private var isMonitorVisible = false
    
    // C1: Offline Status UI Components
    private lateinit var offlineStatusBar: LinearLayout
    private lateinit var offlineStatusText: TextView
    private lateinit var offlineStatusIcon: TextView
    private lateinit var downloadButton: Button
    private lateinit var downloadProgress: ProgressBar
    private var isOnline = true
    
    // Network callback for connectivity monitoring
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    
    data class GeofenceEvent(
        val landmarkId: String,
        val landmarkName: String,
        val transitionType: String,
        val timestamp: Long
    )

    // Permission launcher for fine location
    private val requestFineLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                checkBackgroundLocationPermission()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show()
            }
        }
    
    // Permission launcher for background location (required for geofencing)
    private val requestBackgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                initializeGeofencing()
            } else {
                Toast.makeText(
                    this,
                    "Background location needed for geofence detection",
                    Toast.LENGTH_LONG
                ).show()
                // Still enable location but geofencing won't work in background
                enableUserLocation()
            }
        }

    // Receiver for geofence events
    private val geofenceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val landmarkId = intent.getStringExtra("landmark_id") ?: return
            val landmarkName = intent.getStringExtra("landmark_name") ?: "Unknown"
            val transitionType = intent.getStringExtra("transition_type") ?: return
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
            
            handleGeofenceEvent(landmarkId, landmarkName, transitionType, timestamp)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        geofenceManager = GeofenceManager(this)
        offlineMapManager = OfflineMapManager(this)
        offlineDataCache = OfflineDataCache(this)
        
        // Initialize network monitoring
        setupNetworkMonitoring()
        
        // Create root layout
        val rootLayout = FrameLayout(this)
        
        // Create and add MapView
        mapView = MapView(this)
        rootLayout.addView(mapView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        // C1: Create offline status bar at top
        createOfflineStatusBar(rootLayout)
        
        // Create and add Geofence Monitor UI
        createGeofenceMonitorUI(rootLayout)
        
        setContentView(rootLayout)

        // Set camera to show Arizona Route 66 corridor
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(-111.5, 35.1)) // Central Arizona
                .zoom(7.0) // See full Arizona Route 66
                .build()
        )

        // Load Mapbox style and initialize features
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
            setupAnnotationManagers()
            addAllLandmarkMarkers()
            drawAllGeofenceCircles()
            requestLocationPermissions()
        }
        
        // Register broadcast receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                geofenceReceiver,
                IntentFilter(GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                geofenceReceiver,
                IntentFilter(GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT)
            )
        }
        
        // C1: Initialize offline cache on startup
        initializeOfflineCache()
    }
    
    /**
     * C1: Setup network connectivity monitoring
     */
    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    isOnline = true
                    updateOfflineStatusUI()
                    Log.d(TAG, "Network available - Online mode")
                }
            }
            
            override fun onLost(network: Network) {
                runOnUiThread {
                    isOnline = false
                    updateOfflineStatusUI()
                    Log.d(TAG, "Network lost - Offline mode")
                    
                    // Show toast about offline mode
                    if (offlineDataCache.isCacheValid()) {
                        Toast.makeText(
                            this@MainActivity,
                            "📴 Offline Mode - Using cached data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Check initial state
        isOnline = isNetworkAvailable()
    }
    
    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    
    /**
     * C1: Create offline status bar UI
     */
    private fun createOfflineStatusBar(rootLayout: FrameLayout) {
        offlineStatusBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#4CAF50")) // Green for online
            setPadding(16, 12, 16, 12)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        // Status icon
        offlineStatusIcon = TextView(this).apply {
            text = "🟢"
            textSize = 16f
            setPadding(0, 0, 12, 0)
        }
        offlineStatusBar.addView(offlineStatusIcon)
        
        // Status text
        offlineStatusText = TextView(this).apply {
            text = "Online"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        offlineStatusBar.addView(offlineStatusText)
        
        // Progress bar (hidden by default)
        downloadProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 12
            }
            visibility = View.GONE
            max = 100
        }
        offlineStatusBar.addView(downloadProgress)
        
        // Download button
        downloadButton = Button(this).apply {
            text = "📥 Cache"
            setBackgroundColor(Color.parseColor("#1976D2"))
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding(16, 8, 16, 8)
            setOnClickListener { handleOfflineDownload() }
        }
        offlineStatusBar.addView(downloadButton)
        
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
        }
        rootLayout.addView(offlineStatusBar, params)
        
        // Initial UI update
        updateOfflineStatusUI()
    }
    
    /**
     * C1: Update offline status UI based on connectivity and cache state
     */
    private fun updateOfflineStatusUI() {
        val cacheInfo = offlineDataCache.getCacheInfo()
        
        if (isOnline) {
            offlineStatusBar.setBackgroundColor(Color.parseColor("#4CAF50"))
            offlineStatusIcon.text = "🟢"
            offlineStatusText.text = if (cacheInfo.isValid) {
                "Online | Cache: ${cacheInfo.itemCount} items"
            } else {
                "Online | No cache"
            }
            downloadButton.text = if (cacheInfo.isValid) "🔄 Update" else "📥 Cache"
            downloadButton.isEnabled = true
        } else {
            offlineStatusBar.setBackgroundColor(Color.parseColor("#FF9800"))
            offlineStatusIcon.text = "📴"
            offlineStatusText.text = if (cacheInfo.isValid) {
                "Offline | Using cache (${cacheInfo.itemCount} items)"
            } else {
                "Offline | No cache available!"
            }
            downloadButton.text = "Offline"
            downloadButton.isEnabled = false
            
            if (!cacheInfo.isValid) {
                offlineStatusBar.setBackgroundColor(Color.parseColor("#F44336"))
            }
        }
    }
    
    /**
     * C1: Handle offline download button click
     */
    private fun handleOfflineDownload() {
        Toast.makeText(this, "Starting offline cache...", Toast.LENGTH_SHORT).show()
        
        // Show progress
        downloadProgress.visibility = View.VISIBLE
        downloadProgress.progress = 0
        downloadButton.isEnabled = false
        downloadButton.text = "⏳"
        
        // Cache landmark metadata first
        val metadataCached = offlineDataCache.cacheLandmarksData()
        
        if (metadataCached) {
            downloadProgress.progress = 30
            
            // Check if map tiles already downloaded
            offlineMapManager.checkOfflineRegionExists { exists, size ->
                runOnUiThread {
                    if (exists) {
                        // Already have offline maps
                        downloadProgress.progress = 100
                        completeOfflineDownload(true, "Cache updated! Maps: ${size/1024/1024}MB")
                    } else {
                        // Download map tiles
                        downloadProgress.progress = 40
                        offlineMapManager.downloadArizonaRoute66Region(
                            onProgress = { progress ->
                                runOnUiThread {
                                    downloadProgress.progress = (40 + progress * 0.6).toInt()
                                }
                            },
                            onComplete = { success, message ->
                                runOnUiThread {
                                    completeOfflineDownload(success, message)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            completeOfflineDownload(false, "Failed to cache metadata")
        }
    }
    
    /**
     * C1: Complete offline download process
     */
    private fun completeOfflineDownload(success: Boolean, message: String) {
        downloadProgress.visibility = View.GONE
        downloadButton.isEnabled = true
        updateOfflineStatusUI()
        
        if (success) {
            Toast.makeText(this, "✅ $message", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Offline cache complete: $message")
        } else {
            Toast.makeText(this, "❌ $message", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Offline cache failed: $message")
        }
    }
    
    /**
     * C1: Initialize offline cache on startup
     */
    private fun initializeOfflineCache() {
        if (!offlineDataCache.isCacheValid()) {
            // Auto-cache on first launch if online
            if (isOnline) {
                offlineDataCache.cacheLandmarksData()
                Log.d(TAG, "Initial offline cache created")
            }
        } else {
            val cacheInfo = offlineDataCache.getCacheInfo()
            Log.d(TAG, "Offline cache loaded: ${cacheInfo.itemCount} items, last updated: ${cacheInfo.lastUpdated}")
        }
    }
    
    /**
     * Create the Geofence Monitor UI overlay
     */
    private fun createGeofenceMonitorUI(rootLayout: FrameLayout) {
        // Toggle button at top right (adjusted for status bar)
        toggleButton = Button(this).apply {
            text = "📍 Monitor"
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            setPadding(24, 16, 24, 16)
            textSize = 11f
            setOnClickListener { toggleMonitorPanel() }
        }
        
        val toggleParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 100 // Below status bar
            rightMargin = 16
        }
        rootLayout.addView(toggleButton, toggleParams)
        
        // Monitor panel (initially hidden)
        monitorPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            setPadding(16, 16, 16, 16)
            elevation = 8f
            visibility = View.GONE
        }
        
        // Title
        val titleView = TextView(this).apply {
            text = "🗺️ GEOFENCE MONITOR"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1976D2"))
            setPadding(0, 0, 0, 16)
        }
        monitorPanel.addView(titleView)
        
        // Registered Geofences Section
        val geofenceHeader = TextView(this).apply {
            text = "📋 REGISTERED GEOFENCES (${ArizonaLandmarks.landmarks.size})"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#424242"))
            setPadding(0, 8, 0, 8)
        }
        monitorPanel.addView(geofenceHeader)
        
        // Scrollable geofence list
        val geofenceScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                200
            )
        }
        
        geofenceListTextView = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.parseColor("#616161"))
            setBackgroundColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
        }
        geofenceScrollView.addView(geofenceListTextView)
        monitorPanel.addView(geofenceScrollView)
        
        // Divider
        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#BDBDBD"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply { 
                topMargin = 16
                bottomMargin = 16
            }
        }
        monitorPanel.addView(divider)
        
        // Event Log Section
        val eventHeader = TextView(this).apply {
            text = "📜 EVENT LOG"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#424242"))
            setPadding(0, 0, 0, 8)
        }
        monitorPanel.addView(eventHeader)
        
        // Scrollable event log
        val eventScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                150
            )
        }
        
        eventLogTextView = TextView(this).apply {
            text = "Waiting for geofence events...\n(Use mock location to trigger ENTER/EXIT)"
            textSize = 11f
            setTextColor(Color.parseColor("#757575"))
            setBackgroundColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
        }
        eventScrollView.addView(eventLogTextView)
        monitorPanel.addView(eventScrollView)
        
        // Add panel to root layout
        val panelParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            leftMargin = 16
            rightMargin = 16
            bottomMargin = 32
        }
        rootLayout.addView(monitorPanel, panelParams)
        
        // Populate geofence list
        updateGeofenceListDisplay()
    }
    
    /**
     * Toggle the monitor panel visibility
     */
    private fun toggleMonitorPanel() {
        isMonitorVisible = !isMonitorVisible
        if (isMonitorVisible) {
            monitorPanel.visibility = View.VISIBLE
            toggleButton.text = "✕ Hide"
            toggleButton.setBackgroundColor(Color.parseColor("#F44336"))
        } else {
            monitorPanel.visibility = View.GONE
            toggleButton.text = "📍 Monitor"
            toggleButton.setBackgroundColor(Color.parseColor("#2196F3"))
        }
    }
    
    /**
     * Update the geofence list display with all landmarks
     */
    private fun updateGeofenceListDisplay() {
        val sb = StringBuilder()
        ArizonaLandmarks.landmarks.forEachIndexed { index, landmark ->
            val status = if (activeLandmarks.contains(landmark.id)) "🟢 INSIDE" else "⚪ Outside"
            sb.append("${index + 1}. ${landmark.name}\n")
            sb.append("   ID: ${landmark.id}\n")
            sb.append("   📍 (${landmark.latitude}, ${landmark.longitude})\n")
            sb.append("   📏 Radius: ${landmark.radiusMeters.toInt()}m\n")
            sb.append("   Status: $status\n")
            if (index < ArizonaLandmarks.landmarks.size - 1) {
                sb.append("   ─────────────────────\n")
            }
        }
        geofenceListTextView.text = sb.toString()
    }
    
    /**
     * Update the event log display
     */
    private fun updateEventLogDisplay() {
        if (geofenceEventLog.isEmpty()) {
            eventLogTextView.text = "Waiting for geofence events...\n(Use mock location to trigger ENTER/EXIT)"
            return
        }
        
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        
        // Show most recent events first (last 20)
        geofenceEventLog.takeLast(20).reversed().forEach { event ->
            val timeStr = timeFormat.format(Date(event.timestamp))
            val emoji = when (event.transitionType) {
                "ENTER" -> "🟢 ENTER"
                "EXIT" -> "🔴 EXIT"
                "DWELL" -> "🟡 DWELL"
                else -> "⚪ ${event.transitionType}"
            }
            sb.append("[$timeStr] $emoji\n")
            sb.append("  → ${event.landmarkName}\n")
            sb.append("  ID: ${event.landmarkId}\n\n")
        }
        
        eventLogTextView.text = sb.toString()
        eventLogTextView.setTextColor(Color.parseColor("#212121"))
    }
    
    private fun setupAnnotationManagers() {
        val annotationPlugin = mapView.annotations
        pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
        circleAnnotationManager = annotationPlugin.createCircleAnnotationManager()
    }

    /**
     * Add markers for all Arizona Route 66 landmarks
     */
    private fun addAllLandmarkMarkers() {
        // Convert vector drawable to bitmap
        val drawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.red_marker)
        val markerBitmap = if (drawable != null) {
            val bitmap = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth.takeIf { it > 0 } ?: 48,
                drawable.intrinsicHeight.takeIf { it > 0 } ?: 72,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } else {
            // Fallback: create a simple colored bitmap
            android.graphics.Bitmap.createBitmap(24, 36, android.graphics.Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.RED)
            }
        }
        
        ArizonaLandmarks.landmarks.forEach { landmark ->
            val markerOptions = PointAnnotationOptions()
                .withPoint(landmark.toPoint())
                .withIconImage(markerBitmap)
                .withTextField(landmark.name)
                .withTextOffset(listOf(0.0, 2.0))
                .withTextSize(10.0)
            
            pointAnnotationManager?.create(markerOptions)
        }
        
        Log.d(TAG, "Added ${ArizonaLandmarks.landmarks.size} landmark markers")
    }

    /**
     * Draw geofence radius circles around all landmarks
     */
    private fun drawAllGeofenceCircles() {
        ArizonaLandmarks.landmarks.forEach { landmark ->
            val circleOptions = CircleAnnotationOptions()
                .withPoint(landmark.toPoint())
                .withCircleRadius((landmark.radiusMeters / 10).toDouble()) // Visual scaling
                .withCircleColor("#4A90D9") // Blue
                .withCircleOpacity(0.25)
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#2E5A8B")
            
            circleAnnotationManager?.create(circleOptions)
        }
    }

    /**
     * Request location permissions step by step
     */
    private fun requestLocationPermissions() {
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                checkBackgroundLocationPermission()
            }
            else -> {
                requestFineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
    
    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    initializeGeofencing()
                }
                else -> {
                    // Show rationale first
                    Toast.makeText(
                        this,
                        "Background location allows geofence detection while traveling",
                        Toast.LENGTH_LONG
                    ).show()
                    requestBackgroundLocationLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }
            }
        } else {
            initializeGeofencing()
        }
    }
    
    /**
     * Initialize geofencing after permissions granted
     */
    private fun initializeGeofencing() {
        enableUserLocation()
        
        geofenceManager.registerAllGeofences(
            onSuccess = {
                Toast.makeText(
                    this,
                    "✓ Monitoring ${ArizonaLandmarks.landmarks.size} Route 66 landmarks",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "Geofences registered successfully")
                printGeofenceList()
            },
            onFailure = { e ->
                Toast.makeText(
                    this,
                    "Geofence registration failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Geofence registration failed", e)
            }
        )
    }
    
    /**
     * Print registered geofences for demo/debug
     */
    private fun printGeofenceList() {
        Log.d(TAG, "=== REGISTERED GEOFENCES ===")
        ArizonaLandmarks.landmarks.forEach { landmark ->
            Log.d(TAG, """
                Landmark: ${landmark.name}
                ID: ${landmark.id}
                Coordinates: (${landmark.latitude}, ${landmark.longitude})
                Radius: ${landmark.radiusMeters}m
                ---
            """.trimIndent())
        }
    }

    /**
     * Enable user location display on map
     */
    private fun enableUserLocation() {
        val locationComponent = mapView.location
        locationComponent.updateSettings {
            enabled = true
            locationPuck = LocationPuck2D()
        }
    }

    /**
     * Handle geofence ENTER/EXIT events
     */
    private fun handleGeofenceEvent(
        landmarkId: String,
        landmarkName: String,
        transitionType: String,
        timestamp: Long
    ) {
        val event = GeofenceEvent(landmarkId, landmarkName, transitionType, timestamp)
        geofenceEventLog.add(event)
        
        when (transitionType) {
            "ENTER" -> {
                activeLandmarks.add(landmarkId)
                highlightLandmark(landmarkId, true)
                showEntryNotification(landmarkName)
            }
            "EXIT" -> {
                activeLandmarks.remove(landmarkId)
                highlightLandmark(landmarkId, false)
                showExitNotification(landmarkName)
            }
            "DWELL" -> {
                showDwellNotification(landmarkName)
            }
        }
        
        // Update UI displays
        updateGeofenceListDisplay()
        updateEventLogDisplay()
        
        logGeofenceEvent(event)
    }
    
    /**
     * Highlight or unhighlight a landmark's geofence circle
     */
    private fun highlightLandmark(landmarkId: String, highlight: Boolean) {
        // Remove existing circles and redraw with new colors
        circleAnnotationManager?.deleteAll()
        
        ArizonaLandmarks.landmarks.forEach { lm ->
            val isActive = activeLandmarks.contains(lm.id)
            val color = if (isActive) "#4CAF50" else "#4A90D9" // Green if active, blue otherwise
            val opacity = if (isActive) 0.4 else 0.25
            
            val circleOptions = CircleAnnotationOptions()
                .withPoint(lm.toPoint())
                .withCircleRadius((lm.radiusMeters / 10).toDouble())
                .withCircleColor(color)
                .withCircleOpacity(opacity)
                .withCircleStrokeWidth(if (isActive) 3.0 else 2.0)
                .withCircleStrokeColor(if (isActive) "#2E7D32" else "#2E5A8B")
            
            circleAnnotationManager?.create(circleOptions)
        }
    }
    
    private fun showEntryNotification(landmarkName: String) {
        Toast.makeText(
            this,
            "📍 ENTERED: $landmarkName",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun showExitNotification(landmarkName: String) {
        Toast.makeText(
            this,
            "👋 EXITED: $landmarkName",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun showDwellNotification(landmarkName: String) {
        Toast.makeText(
            this,
            "⏱️ DWELLING at: $landmarkName",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * Log geofence events for demo inspection
     */
    private fun logGeofenceEvent(event: GeofenceEvent) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = timeFormat.format(Date(event.timestamp))
        
        Log.d(TAG, """
            ╔══════════════════════════════════════
            ║ GEOFENCE EVENT
            ╠══════════════════════════════════════
            ║ Type: ${event.transitionType}
            ║ Landmark: ${event.landmarkName}
            ║ ID: ${event.landmarkId}
            ║ Time: $timeStr
            ╚══════════════════════════════════════
        """.trimIndent())
    }
    
    /**
     * Get event log for demo purposes
     */
    fun getGeofenceEventLog(): List<GeofenceEvent> = geofenceEventLog.toList()
    
    /**
     * Get currently active landmarks
     */
    fun getActiveLandmarks(): Set<String> = activeLandmarks.toSet()

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
        unregisterReceiver(geofenceReceiver)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        geofenceManager.removeAllGeofences()
        if (::mapView.isInitialized) mapView.onDestroy()
    }
}
