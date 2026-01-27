package com.yaxer.timetrack.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors network connectivity state using ConnectivityManager.
 * Provides reactive updates via StateFlow for observing online/offline status.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var onNetworkAvailable: (() -> Unit)? = null

    /**
     * Set a listener to be called when network becomes available.
     * Use this to trigger sync when coming back online.
     */
    fun setOnNetworkAvailableListener(listener: () -> Unit) {
        onNetworkAvailable = listener
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            onNetworkAvailable?.invoke()
        }

        override fun onLost(network: Network) {
            // Check if there's still any active network
            _isOnline.value = checkCurrentConnectivity()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet && isValidated
        }
    }

    /**
     * Register the network callback to start monitoring connectivity changes.
     * Call this in Application.onCreate()
     */
    fun register() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    /**
     * Unregister the network callback to stop monitoring.
     * Call this when no longer needed to avoid memory leaks.
     */
    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            // Callback was not registered or already unregistered
        }
    }

    /**
     * Check current connectivity status synchronously.
     * @return true if device has active internet connection, false otherwise
     */
    fun isOnlineNow(): Boolean {
        return checkCurrentConnectivity()
    }

    private fun checkCurrentConnectivity(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
