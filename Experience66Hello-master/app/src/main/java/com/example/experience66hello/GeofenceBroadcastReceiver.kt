package com.example.experience66hello

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/**
 * Broadcast receiver for geofence transition events
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        const val TAG = "GeofenceReceiver"
        const val ACTION_GEOFENCE_EVENT = "com.example.experience66hello.ACTION_GEOFENCE_EVENT"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }
        
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }
        
        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
        
        val transitionType = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
            else -> "UNKNOWN"
        }
        
        for (geofence in triggeringGeofences) {
            val landmarkId = geofence.requestId
            val landmark = ArizonaLandmarks.findById(landmarkId)
            
            Log.d(TAG, "Geofence $transitionType: ${landmark?.name ?: landmarkId}")
            
            // Broadcast the event to MainActivity with explicit package
            val broadcastIntent = Intent(ACTION_GEOFENCE_EVENT).apply {
                setPackage(context.packageName) // Explicit package for Android 14+
                putExtra("landmark_id", landmarkId)
                putExtra("landmark_name", landmark?.name ?: "Unknown")
                putExtra("transition_type", transitionType)
                putExtra("timestamp", System.currentTimeMillis())
            }
            context.sendBroadcast(broadcastIntent)
        }
    }
}
