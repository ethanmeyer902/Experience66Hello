package com.example.experience66hello

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Parses the Route 66 archive CSV file containing CONTENTdm metadata
 * This file links POIs to their archive items in the CONTENTdm database
 */
object CsvParser {
    private const val TAG = "CsvParser"
    
    /**
     * Reads and parses the archive CSV file from assets
     * Returns a list of archive items with call numbers and reference URLs
     */
    fun parseRoute66Archive(context: Context, fileName: String = "capstone-exp-66.csv"): List<Route66ArchiveItem> {
        val items = mutableListOf<Route66ArchiveItem>()
        
        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            // Skip header line
            reader.readLine()
            
            var line: String?
            var lineNumber = 1
            
            while (reader.readLine().also { line = it } != null) {
                lineNumber++
                line?.let { csvLine ->
                    try {
                        val item = parseCsvLine(csvLine)
                        if (item != null) {
                            items.add(item)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing line $lineNumber: ${e.message}")
                    }
                }
            }
            
            reader.close()
            inputStream.close()
            
            Log.d(TAG, "Successfully parsed ${items.size} archive items from CSV")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading CSV file: ${e.message}", e)
        }
        
        return items
    }
    
    /**
     * Converts a CSV line into a Route66ArchiveItem
     * Expected format: Call number, CONTENTdm number, Item number, Reference URL
     */
    private fun parseCsvLine(line: String): Route66ArchiveItem? {
        if (line.isBlank()) return null
        
        // Split by comma but keep quoted fields intact
        val parts = parseCsvLineWithQuotes(line)
        
        if (parts.size < 4) {
            Log.w(TAG, "Invalid CSV line (expected 4 columns): $line")
            return null
        }
        
        return Route66ArchiveItem(
            callNumber = parts[0].trim(),
            contentDmNumber = parts[1].trim(),
            itemNumber = parts[2].trim(),
            referenceUrl = parts[3].trim()
        )
    }
    
    /**
     * Splits a CSV line into fields, handling quoted values properly
     * This allows fields to contain commas when they're in quotes
     */
    private fun parseCsvLineWithQuotes(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var insideQuotes = false
        
        for (i in line.indices) {
            val char = line[i]
            
            when {
                char == '"' -> {
                    insideQuotes = !insideQuotes
                }
                char == ',' && !insideQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        
        // Add the last field
        result.add(current.toString())
        
        return result
    }
}
