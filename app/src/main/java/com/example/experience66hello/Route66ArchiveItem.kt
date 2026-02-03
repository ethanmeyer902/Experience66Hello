package com.example.experience66hello

/**
 * Represents an archive item from the CONTENTdm database
 * Links POIs to their archive entries via call numbers and URLs
 */
data class Route66ArchiveItem(
    val callNumber: String,
    val contentDmNumber: String,
    val itemNumber: String,
    val referenceUrl: String
) {
    /**
     * Extracts potential location keywords from the call number
     * Call numbers like "NAU.PH.2004.11.2.1515" might contain location hints
     */
    fun extractLocationKeywords(): List<String> {
        val keywords = mutableListOf<String>()
        
        // Try to extract meaningful parts from call number
        val parts = callNumber.split(".")
        if (parts.size > 2) {
            // Add parts that might be location-related
            parts.forEach { part ->
                if (part.length > 2 && part.any { it.isLetter() }) {
                    keywords.add(part.lowercase())
                }
            }
        }
        
        return keywords
    }
    
    /**
     * Checks if this archive item might be related to a landmark
     * Matches by checking if the landmark name appears in the call number
     */
    fun matchesLandmark(landmarkName: String): Boolean {
        val nameLower = landmarkName.lowercase()
        val callLower = callNumber.lowercase()
        
        // Check if call number contains the landmark name or parts of it
        return callLower.contains(nameLower) || 
               landmarkName.split(" ").any { word ->
                   callLower.contains(word.lowercase())
               }
    }
}
