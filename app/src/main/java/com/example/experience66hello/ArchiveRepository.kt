package com.example.experience66hello

import android.content.Context
import android.util.Log

/**
 * Manages the Route 66 archive data loaded from CSV
 * Provides search functionality to find archive items related to POIs
 */
class ArchiveRepository(private val context: Context) {
    
    private var archiveItems: List<Route66ArchiveItem> = emptyList()
    var isLoaded = false
        private set
    
    companion object {
        private const val TAG = "ArchiveRepository"
    }
    
    /**
     * Loads archive data from the CSV file in assets
     * This data links POIs to their CONTENTdm archive entries
     */
    fun loadArchiveData() {
        if (isLoaded && archiveItems.isNotEmpty()) {
            Log.d(TAG, "Archive data already loaded: ${archiveItems.size} items")
            return
        }
        
        try {
            Log.d(TAG, "Loading archive data from CSV...")
            archiveItems = CsvParser.parseRoute66Archive(context)
            isLoaded = true
            if (archiveItems.isEmpty()) {
                Log.w(TAG, "WARNING: CSV parsing returned 0 items!")
            } else {
                Log.d(TAG, "Successfully loaded ${archiveItems.size} archive items")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load archive data: ${e.message}", e)
            e.printStackTrace()
            // Reset loaded flag so we can try again
            isLoaded = false
            archiveItems = emptyList()
        }
    }
    
    /**
     * Searches for archive items related to a POI
     * Matches by landmark name, ID, or call number
     * Returns items that might be related to the searched landmark
     */
    fun searchByPoi(poiName: String): List<Route66ArchiveItem> {
        // Force load if not loaded
        if (!isLoaded) {
            loadArchiveData()
        }
        
        if (poiName.isBlank()) {
            return emptyList()
        }
        
        val results = mutableListOf<Route66ArchiveItem>()
        val searchTerm = poiName.lowercase().trim()
        
        Log.d(TAG, "Searching for: '$poiName' (term: '$searchTerm')")
        Log.d(TAG, "Archive items loaded: ${archiveItems.size}")
        
        // Strategy 1: If search term looks like a call number (starts with NAU), search by call number
        if (searchTerm.startsWith("nau") || searchTerm.contains("ph.") || searchTerm.contains("oh.")) {
            val callResults = searchByCallNumber(poiName)
            Log.d(TAG, "Call number search returned ${callResults.size} results")
            return callResults
        }
        
        // Strategy 2: Try to find matching landmark first
        // Note: This will work with both hardcoded and CSV-loaded landmarks
        val matchingLandmarks = ArizonaLandmarks.landmarks.filter { landmark ->
            landmark.name.lowercase().contains(searchTerm) ||
            landmark.id.lowercase().contains(searchTerm) ||
            searchTerm.contains(landmark.name.lowercase()) ||
            searchTerm.contains(landmark.id.lowercase()) ||
            landmark.description.lowercase().contains(searchTerm)
        }
        
        Log.d(TAG, "Found ${matchingLandmarks.size} matching landmark(s)")
        
        // Strategy 3: If we found matching landmarks, return relevant archive items
        // Since call numbers don't contain landmark names, we return a representative
        // sample of Route 66 archive items when a landmark is found
        if (matchingLandmarks.isNotEmpty()) {
            // Make sure we have archive items loaded
            if (archiveItems.isEmpty()) {
                Log.w(TAG, "Archive items list is empty! Attempting to reload...")
                loadArchiveData()
            }
            
            if (archiveItems.isNotEmpty()) {
                // Return a diverse sample of archive items (all are Route 66 related)
                // This gives users access to Route 66 historical archive items
                val sampleSize = minOf(25, archiveItems.size) // Return up to 25 items
                val step = if (archiveItems.size > sampleSize) {
                    maxOf(1, archiveItems.size / sampleSize)
                } else {
                    1
                }
                
                for (i in archiveItems.indices step step) {
                    if (results.size >= sampleSize) break
                    results.add(archiveItems[i])
                }
                
                // Also try to find any items that might have the landmark name/ID in call number
                matchingLandmarks.forEach { landmark ->
                    val landmarkWords = landmark.name.lowercase().split(" ", "-", "_", "'")
                    archiveItems.forEach { item ->
                        val callLower = item.callNumber.lowercase()
                        val matches = landmarkWords.any { word ->
                            word.length > 3 && callLower.contains(word)
                        } || callLower.contains(landmark.id.lowercase())
                        
                        if (matches && !results.contains(item)) {
                            results.add(item)
                        }
                    }
                }
                
                Log.d(TAG, "Found ${matchingLandmarks.size} matching landmark(s), returning ${results.size} archive items")
            } else {
                Log.e(TAG, "Archive items list is still empty after reload attempt!")
            }
        }
        
        // Strategy 4: Direct search in call numbers for the search term
        archiveItems.forEach { item ->
            if (item.callNumber.lowercase().contains(searchTerm) && !results.contains(item)) {
                results.add(item)
            }
        }
        
        // Strategy 5: If no results yet, try partial matching
        if (results.isEmpty() && searchTerm.length >= 3) {
            archiveItems.forEach { item ->
                val callLower = item.callNumber.lowercase()
                // Try matching parts of the search term
                val searchParts = searchTerm.split(" ", "-", "_")
                val matches = searchParts.any { part ->
                    part.length >= 3 && callLower.contains(part)
                }
                
                if (matches && !results.contains(item)) {
                    results.add(item)
                }
            }
        }
        
        Log.d(TAG, "Search for '$poiName' returned ${results.size} results")
        return results.distinct()
    }
    
    /**
     * Search archive items by call number
     */
    fun searchByCallNumber(callNumber: String): List<Route66ArchiveItem> {
        if (!isLoaded) {
            loadArchiveData()
        }
        
        if (callNumber.isBlank()) {
            return emptyList()
        }
        
        val searchTerm = callNumber.lowercase()
        return archiveItems.filter { 
            it.callNumber.lowercase().contains(searchTerm)
        }
    }
    
    /**
     * Get all archive items for a specific landmark
     */
    fun getItemsForLandmark(landmarkId: String): List<Route66ArchiveItem> {
        if (!isLoaded) {
            loadArchiveData()
        }
        
        val landmark = ArizonaLandmarks.findById(landmarkId)
        if (landmark == null) {
            return emptyList()
        }
        
        // Search for items related to this landmark
        return searchByPoi(landmark.name)
    }
    
    /**
     * Get all archive items
     */
    fun getAllItems(): List<Route66ArchiveItem> {
        if (!isLoaded) {
            loadArchiveData()
        }
        return archiveItems
    }
    
    /**
     * Get total count of archive items
     */
    fun getItemCount(): Int {
        if (!isLoaded) {
            loadArchiveData()
        }
        return archiveItems.size
    }
}
