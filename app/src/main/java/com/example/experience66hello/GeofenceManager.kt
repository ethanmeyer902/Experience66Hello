package com.example.experience66hello

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Manages geofence registration and monitoring for Route 66 landmarks
 */
class GeofenceManager(private val context: Context) {
    
    companion object {
        const val TAG = "GeofenceManager"
        private const val GEOFENCE_EXPIRATION_MS = Geofence.NEVER_EXPIRE
        private const val LOITERING_DELAY_MS = 30000 // 30 seconds for DWELL
    }
    
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
    
    /**
     * Register all Arizona Route 66 landmarks as geofences
     */
    fun registerAllGeofences(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            onFailure(SecurityException("Location permission required"))
            return
        }
        
        val geofenceList = ArizonaLandmarks.landmarks.map { landmark ->
            createGeofence(landmark)
        }
        
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()
        
        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully registered ${geofenceList.size} geofences")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register geofences: ${e.message}")
                    onFailure(e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            onFailure(e)
        }
    }
    
    /**
     * Create a Geofence object from a Route66Landmark
     */
    private fun createGeofence(landmark: Route66Landmark): Geofence {
        return Geofence.Builder()
            .setRequestId(landmark.id)
            .setCircularRegion(
                landmark.latitude,
                landmark.longitude,
                landmark.radiusMeters
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_MS)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                Geofence.GEOFENCE_TRANSITION_EXIT or
                Geofence.GEOFENCE_TRANSITION_DWELL
            )
            .setLoiteringDelay(LOITERING_DELAY_MS)
            .build()
    }
    
    /**
     * Remove all registered geofences
     */
    fun removeAllGeofences(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "All geofences removed")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofences: ${e.message}")
                onFailure(e)
            }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
