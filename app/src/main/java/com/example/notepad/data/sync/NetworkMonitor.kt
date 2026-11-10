package com.example.notepad.data.sync

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 網路連線狀態觀察介面（便於單元測試隔離 Android ConnectivityManager）
 */
interface NetworkConnectivityObserver {
    val isOnlineFlow: StateFlow<Boolean>
    fun isCurrentlyOnline(): Boolean
}

/**
 * 監測裝置網路連線狀態與恢復事件（資料同步規則 2、3、4）
 */
class AndroidNetworkMonitor(context: Context) : NetworkConnectivityObserver {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnlineFlow = MutableStateFlow(checkCurrentConnectivity())
    override val isOnlineFlow: StateFlow<Boolean> = _isOnlineFlow.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnlineFlow.value = true
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnlineFlow.value = hasInternet
        }

        override fun onLost(network: Network) {
            _isOnlineFlow.value = checkCurrentConnectivity()
        }

        override fun onUnavailable() {
            _isOnlineFlow.value = false
        }
    }

    init {
        registerCallbackSafely()
    }

    @SuppressLint("MissingPermission")
    private fun registerCallbackSafely() {
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (_: Exception) {
            try {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, networkCallback)
            } catch (_: Exception) {
                _isOnlineFlow.value = checkCurrentConnectivity()
            }
        }
    }

    override fun isCurrentlyOnline(): Boolean {
        val online = checkCurrentConnectivity()
        _isOnlineFlow.value = online
        return online
    }

    @SuppressLint("MissingPermission")
    private fun checkCurrentConnectivity(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: Exception) {
            false
        }
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }
}
