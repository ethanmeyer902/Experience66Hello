package com.example.experience66hello

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

/**
 * Network connectivity utilities for offline mode demonstration
 * Demonstrates C1: Correct behavior when device loses network access
 */
class NetworkUtils(private val context: Context) {

    companion object {
        const val TAG = "NetworkUtils"
    }

    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var onNetworkChanged: ((Boolean) -> Unit)? = null

    /**
     * Check if device currently has network connectivity
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Get detailed network status
     */
    fun getNetworkStatus(): NetworkStatus {
        val network = connectivityManager.activeNetwork
        if (network == null) {
            return NetworkStatus(
                isConnected = false,
                type = "None",
                description = "No network connection"
            )
        }

        val capabilities = connectivityManager.getNetworkCapabilities(network)
        if (capabilities == null) {
            return NetworkStatus(
                isConnected = false,
                type = "Unknown",
                description = "Network capabilities unavailable"
            )
        }

        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }

        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        return NetworkStatus(
            isConnected = isValidated,
            type = type,
            description = if (isValidated) "Connected via $type" else "$type (no internet)"
        )
    }

    /**
     * Register for network connectivity changes
     */
    fun registerNetworkCallback(onChanged: (Boolean) -> Unit) {
        onNetworkChanged = onChanged

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                onNetworkChanged?.invoke(true)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                onNetworkChanged?.invoke(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasInternet = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                Log.d(TAG, "Network capabilities changed, internet: $hasInternet")
                onNetworkChanged?.invoke(hasInternet)
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
            Log.d(TAG, "Network callback registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    /**
     * Unregister network callback
     */
    fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
                Log.d(TAG, "Network callback unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback: ${e.message}")
            }
        }
        networkCallback = null
        onNetworkChanged = null
    }

    data class NetworkStatus(
        val isConnected: Boolean,
        val type: String,
        val description: String
    )
}

