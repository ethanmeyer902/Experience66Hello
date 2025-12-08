package com.example.experience66hello

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * C1: Simplified Offline Map Manager
 * Tracks offline map download status for demo purposes
 * (Full Mapbox offline tile caching requires additional SDK setup)
 */
class OfflineMapManager(context: Context) {

    companion object {
        const val TAG = "OfflineMapManager"
        private const val PREFS_NAME = "offline_maps_prefs"
        private const val KEY_MAPS_DOWNLOADED = "maps_downloaded"
        private const val KEY_DOWNLOAD_SIZE = "download_size"
        private const val KEY_DOWNLOAD_TIME = "download_time"
    }

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if offline maps are "downloaded" (simulated for demo)
     */
    fun checkOfflineRegionExists(callback: (exists: Boolean, sizeBytes: Long) -> Unit) {
        val exists = prefs.getBoolean(KEY_MAPS_DOWNLOADED, false)
        val size = prefs.getLong(KEY_DOWNLOAD_SIZE, 0)
        Log.d(TAG, "Offline maps status: exists=$exists, size=$size")
        callback(exists, size)
    }

    /**
     * Simulate downloading Arizona Route 66 map tiles
     * In a real app, this would use Mapbox OfflineManager API
     */
    fun downloadArizonaRoute66Region(
        onProgress: (Double) -> Unit,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        Log.d(TAG, "Starting simulated offline map download...")
        
        // Simulate download progress
        Thread {
            try {
                for (i in 0..100 step 10) {
                    Thread.sleep(200) // Simulate download time
                    onProgress(i.toDouble())
                }
                
                // Mark as downloaded
                val simulatedSize = 75L * 1024 * 1024 // ~75 MB simulated
                prefs.edit()
                    .putBoolean(KEY_MAPS_DOWNLOADED, true)
                    .putLong(KEY_DOWNLOAD_SIZE, simulatedSize)
                    .putLong(KEY_DOWNLOAD_TIME, System.currentTimeMillis())
                    .apply()
                
                Log.d(TAG, "Offline map download complete (simulated)")
                onComplete(true, "Arizona Route 66 maps cached (~75 MB)")
                
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}")
                onComplete(false, e.message ?: "Download failed")
            }
        }.start()
    }

    /**
     * Delete offline maps
     */
    fun deleteOfflineRegion(callback: (success: Boolean) -> Unit) {
        prefs.edit()
            .putBoolean(KEY_MAPS_DOWNLOADED, false)
            .putLong(KEY_DOWNLOAD_SIZE, 0)
            .remove(KEY_DOWNLOAD_TIME)
            .apply()
        
        Log.d(TAG, "Offline maps deleted")
        callback(true)
    }

    /**
     * Get offline map info
     */
    fun getOfflineInfo(): OfflineMapInfo {
        return OfflineMapInfo(
            isDownloaded = prefs.getBoolean(KEY_MAPS_DOWNLOADED, false),
            sizeBytes = prefs.getLong(KEY_DOWNLOAD_SIZE, 0),
            downloadTime = prefs.getLong(KEY_DOWNLOAD_TIME, 0),
            coverage = "Arizona Route 66 Corridor",
            zoomLevels = "6-12"
        )
    }

    data class OfflineMapInfo(
        val isDownloaded: Boolean,
        val sizeBytes: Long,
        val downloadTime: Long,
        val coverage: String,
        val zoomLevels: String
    ) {
        fun getSizeMB(): String = "%.1f MB".format(sizeBytes / 1024.0 / 1024.0)
    }
}
