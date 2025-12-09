package com.example.experience66hello

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local cache for landmark/POI metadata (simulates CONTENTdm data caching)
 * Demonstrates C1: Offline Caching for CONTENTdm Data
 */
class LandmarkDataCache(context: Context) {

    companion object {
        const val TAG = "LandmarkDataCache"
        private const val PREFS_NAME = "landmark_cache"
        private const val KEY_LANDMARKS_JSON = "landmarks_json"
        private const val KEY_LAST_SYNC = "last_sync_time"
        private const val KEY_CACHE_VERSION = "cache_version"
        private const val CURRENT_CACHE_VERSION = 1
    }

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Cache all Arizona Route 66 landmark data locally
     */
    fun cacheLandmarkData(): Boolean {
        return try {
            val jsonArray = JSONArray()
            
            ArizonaLandmarks.landmarks.forEach { landmark ->
                val landmarkJson = JSONObject().apply {
                    put("id", landmark.id)
                    put("name", landmark.name)
                    put("description", landmark.description)
                    put("latitude", landmark.latitude)
                    put("longitude", landmark.longitude)
                    put("radius_meters", landmark.radiusMeters)
                    // Simulate additional CONTENTdm metadata
                    put("historical_notes", getHistoricalNotes(landmark.id))
                    put("year_established", getYearEstablished(landmark.id))
                    put("content_dm_id", "RT66-AZ-${landmark.id.uppercase()}")
                }
                jsonArray.put(landmarkJson)
            }

            prefs.edit()
                .putString(KEY_LANDMARKS_JSON, jsonArray.toString())
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .putInt(KEY_CACHE_VERSION, CURRENT_CACHE_VERSION)
                .apply()

            Log.d(TAG, "Cached ${jsonArray.length()} landmarks to local storage")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache landmark data: ${e.message}")
            false
        }
    }

    /**
     * Get cached landmark data (works offline)
     */
    fun getCachedLandmarks(): List<CachedLandmark> {
        val jsonString = prefs.getString(KEY_LANDMARKS_JSON, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val landmarks = mutableListOf<CachedLandmark>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                landmarks.add(
                    CachedLandmark(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.getString("description"),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        radiusMeters = obj.getDouble("radius_meters").toFloat(),
                        historicalNotes = obj.optString("historical_notes", ""),
                        yearEstablished = obj.optString("year_established", "Unknown"),
                        contentDmId = obj.optString("content_dm_id", "")
                    )
                )
            }
            
            Log.d(TAG, "Retrieved ${landmarks.size} landmarks from cache")
            landmarks
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read cached landmarks: ${e.message}")
            emptyList()
        }
    }

    /**
     * Check if cache exists and is valid
     */
    fun isCacheValid(): Boolean {
        val version = prefs.getInt(KEY_CACHE_VERSION, 0)
        val hasData = prefs.contains(KEY_LANDMARKS_JSON)
        return hasData && version == CURRENT_CACHE_VERSION
    }

    /**
     * Get last sync time as formatted string
     */
    fun getLastSyncTime(): String {
        val timestamp = prefs.getLong(KEY_LAST_SYNC, 0)
        if (timestamp == 0L) return "Never"
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Get cache size info
     */
    fun getCacheInfo(): CacheInfo {
        val landmarks = getCachedLandmarks()
        val jsonString = prefs.getString(KEY_LANDMARKS_JSON, "") ?: ""
        val sizeBytes = jsonString.toByteArray().size.toLong()
        
        return CacheInfo(
            landmarkCount = landmarks.size,
            sizeBytes = sizeBytes,
            lastSync = getLastSyncTime(),
            isValid = isCacheValid()
        )
    }

    /**
     * Clear all cached data
     */
    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cache cleared")
    }

    // Simulated CONTENTdm historical data
    private fun getHistoricalNotes(landmarkId: String): String {
        return when (landmarkId) {
            "oatman" -> "Gold was discovered here in 1915, leading to a boom town. Clark Gable and Carole Lombard honeymooned at the Oatman Hotel in 1939."
            "kingman" -> "Named after Lewis Kingman, a railroad surveyor. The Powerhouse Visitor Center was built in 1907 as a power plant."
            "hackberry" -> "Originally a mining town, the general store became a Route 66 icon after being restored in 1992 by artist Bob Waldmire."
            "seligman" -> "Angel Delgadillo's barber shop became the birthplace of the Historic Route 66 Association in 1987, saving the road from obscurity."
            "williams" -> "The last town on Route 66 to be bypassed by Interstate 40 on October 13, 1984. Gateway to Grand Canyon since 1901."
            "flagstaff" -> "Named for a flag-raising ceremony on July 4, 1876. Historic downtown features many original Route 66 era buildings."
            "meteor_crater" -> "Created 50,000 years ago by a meteorite impact. First proven impact crater, used for Apollo astronaut training."
            "winslow" -> "Made famous by the Eagles song 'Take It Easy' (1972). The corner at 2nd Street and Kinsley Avenue is now a park."
            "jackrabbit" -> "The iconic 'HERE IT IS' billboard with a giant jackrabbit has been a Route 66 landmark since 1949."
            "holbrook" -> "The Wigwam Motel, built in 1950, features 15 concrete teepees. One of only three remaining Wigwam motels."
            "petrified_forest" -> "Contains some of the world's largest petrified wood. Route 66 alignment runs through the park."
            else -> "Historic Route 66 landmark in Arizona."
        }
    }

    private fun getYearEstablished(landmarkId: String): String {
        return when (landmarkId) {
            "oatman" -> "1915"
            "kingman" -> "1882"
            "hackberry" -> "1874"
            "seligman" -> "1886"
            "williams" -> "1881"
            "flagstaff" -> "1876"
            "meteor_crater" -> "1903 (visitor site)"
            "winslow" -> "1882"
            "jackrabbit" -> "1949"
            "holbrook" -> "1950"
            "petrified_forest" -> "1906 (National Monument)"
            else -> "Unknown"
        }
    }

    data class CachedLandmark(
        val id: String,
        val name: String,
        val description: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float,
        val historicalNotes: String,
        val yearEstablished: String,
        val contentDmId: String
    )

    data class CacheInfo(
        val landmarkCount: Int,
        val sizeBytes: Long,
        val lastSync: String,
        val isValid: Boolean
    ) {
        fun getSizeKB(): String = "%.1f KB".format(sizeBytes / 1024.0)
    }
}

