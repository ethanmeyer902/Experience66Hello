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
 * C1: Offline Data Cache for CONTENTdm/Landmark metadata
 * Stores landmark data locally for offline access
 */
class OfflineDataCache(context: Context) {

    companion object {
        const val TAG = "OfflineDataCache"
        private const val PREFS_NAME = "offline_data_cache"
        private const val KEY_LANDMARKS_DATA = "landmarks_data"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val KEY_CACHE_VERSION = "cache_version"
        private const val CURRENT_VERSION = 1
    }

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Cache all landmark data for offline use
     */
    fun cacheLandmarksData(): Boolean {
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
                    // Simulated CONTENTdm metadata
                    put("historical_notes", getHistoricalNotes(landmark.id))
                    put("year_established", getYearEstablished(landmark.id))
                    put("content_dm_id", "RT66-AZ-${landmark.id.uppercase()}")
                    put("cached_at", System.currentTimeMillis())
                }
                jsonArray.put(landmarkJson)
            }

            prefs.edit()
                .putString(KEY_LANDMARKS_DATA, jsonArray.toString())
                .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
                .putInt(KEY_CACHE_VERSION, CURRENT_VERSION)
                .apply()

            Log.d(TAG, "Cached ${jsonArray.length()} landmarks for offline use")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache data: ${e.message}")
            false
        }
    }

    /**
     * Get cached landmark data (works offline)
     */
    fun getCachedLandmarks(): List<CachedLandmarkData> {
        val jsonString = prefs.getString(KEY_LANDMARKS_DATA, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val landmarks = mutableListOf<CachedLandmarkData>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                landmarks.add(
                    CachedLandmarkData(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.getString("description"),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        radiusMeters = obj.getDouble("radius_meters").toFloat(),
                        historicalNotes = obj.optString("historical_notes", ""),
                        yearEstablished = obj.optString("year_established", ""),
                        contentDmId = obj.optString("content_dm_id", "")
                    )
                )
            }
            landmarks
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read cache: ${e.message}")
            emptyList()
        }
    }

    /**
     * Check if cache exists and is valid
     */
    fun isCacheValid(): Boolean {
        val version = prefs.getInt(KEY_CACHE_VERSION, 0)
        val hasData = prefs.contains(KEY_LANDMARKS_DATA)
        return hasData && version == CURRENT_VERSION
    }

    /**
     * Get cache info for UI display
     */
    fun getCacheInfo(): CacheInfo {
        val landmarks = getCachedLandmarks()
        val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0)
        val lastUpdatedStr = if (lastUpdated > 0) {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastUpdated))
        } else {
            "Never"
        }
        
        return CacheInfo(
            itemCount = landmarks.size,
            lastUpdated = lastUpdatedStr,
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

    // Simulated CONTENTdm historical notes
    private fun getHistoricalNotes(landmarkId: String): String {
        return when (landmarkId) {
            "oatman" -> "Gold discovered 1915. Clark Gable & Carole Lombard honeymooned at Oatman Hotel, 1939."
            "kingman" -> "Named after Lewis Kingman, railroad surveyor. Powerhouse built 1907."
            "hackberry" -> "Mining town turned Route 66 icon. Restored 1992 by artist Bob Waldmire."
            "seligman" -> "Angel Delgadillo founded Historic Route 66 Association here in 1987."
            "williams" -> "Last town bypassed by I-40 on October 13, 1984. Gateway to Grand Canyon."
            "flagstaff" -> "Named for July 4, 1876 flag-raising. Historic downtown preserved."
            "meteor_crater" -> "50,000-year-old impact site. Used for Apollo astronaut training."
            "winslow" -> "Eagles' 'Take It Easy' (1972) tribute. Corner park at 2nd & Kinsley."
            "jackrabbit" -> "'HERE IT IS' billboard landmark since 1949."
            "holbrook" -> "Wigwam Motel built 1950. One of 3 remaining Wigwam motels."
            "petrified_forest" -> "World's largest petrified wood. Route 66 runs through park."
            else -> "Historic Route 66 landmark."
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
            "meteor_crater" -> "1903"
            "winslow" -> "1882"
            "jackrabbit" -> "1949"
            "holbrook" -> "1950"
            "petrified_forest" -> "1906"
            else -> "Unknown"
        }
    }

    data class CachedLandmarkData(
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
        val itemCount: Int,
        val lastUpdated: String,
        val isValid: Boolean
    )
}
