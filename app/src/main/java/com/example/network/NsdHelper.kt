package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

data class DiscoveredPeer(
    val serviceName: String,
    val deviceName: String,
    val ip: String,
    val filePort: Int,
    val chatPort: Int
)

class NsdHelper(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiCharString = "PeerLinkMulticast"
    private val tag = "NsdHelper"

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _discoveryException = MutableStateFlow<com.example.util.PeerLinkException?>(null)
    val discoveryException = _discoveryException.asStateFlow()

    fun clearDiscoveryException() {
        _discoveryException.value = null
    }

    private var isDiscovering = false
    private var isRegistered = false

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val multicastLock = wifiManager?.createMulticastLock(wifiCharString)?.apply {
        setReferenceCounted(true)
    }

    @Synchronized
    fun registerService(filePort: Int, chatPort: Int) {
        val manager = nsdManager ?: return
        if (isRegistered) {
            unregisterService()
        }

        val sharedPrefs = context.getSharedPreferences("peerlink_prefs", Context.MODE_PRIVATE)
        val customName = sharedPrefs.getString("device_name", android.os.Build.MODEL) ?: android.os.Build.MODEL
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "PeerLink - $customName - $filePort"
            serviceType = "_peerlink._tcp"
            port = filePort
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                setAttribute("deviceName", customName)
                setAttribute("chatPort", chatPort.toString())
            }
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.i(tag, "Service registered successfully: ${info.serviceName}")
                isRegistered = true
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "Service registration failed: $errorCode")
                _discoveryException.value = com.example.util.PeerLinkException.DiscoveryException("registering service (code: $errorCode)")
                isRegistered = false
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.i(tag, "Service unregistered: ${info.serviceName}")
                isRegistered = false
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "Service unregistration failed: $errorCode")
            }
        }

        try {
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(tag, "Error registering NSD service", e)
            _discoveryException.value = com.example.util.PeerLinkException.DiscoveryException("registering service", e)
        }
    }

    @Synchronized
    fun unregisterService() {
        val manager = nsdManager ?: return
        if (!isRegistered || registrationListener == null) return
        try {
            manager.unregisterService(registrationListener)
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering NSD service", e)
        } finally {
            registrationListener = null
            isRegistered = false
        }
    }

    @Synchronized
    fun startDiscovery() {
        val manager = nsdManager ?: return
        if (isDiscovering) return
        _discoveredPeers.value = emptyList()

        try {
            multicastLock?.acquire()
        } catch (e: Exception) {
            Log.e(tag, "Failed to acquire multicast lock", e)
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Start discovery failed: $errorCode")
                _discoveryException.value = com.example.util.PeerLinkException.DiscoveryException("starting discovery (code: $errorCode)")
                try {
                    nsdManager?.stopServiceDiscovery(this)
                } catch (e: Exception) {
                    // Safe ignore
                }
                isDiscovering = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Stop discovery failed: $errorCode")
                _discoveryException.value = com.example.util.PeerLinkException.DiscoveryException("stopping discovery (code: $errorCode)")
                isDiscovering = false
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.i(tag, "Discovery started for $serviceType")
                isDiscovering = true
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(tag, "Discovery stopped")
                isDiscovering = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(tag, "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType.contains("peerlink")) {
                    try {
                        nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                Log.e(tag, "Resolve failed: $errorCode")
                            }

                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                                Log.v(tag, "Service resolved: ${resolvedInfo.serviceName}")
                                val hostVal = resolvedInfo.host ?: return
                                val ip = hostVal.hostAddress ?: return

                                // Filter out connection to own device IP if possible
                                val myIp = NetworkUtils.getLocalIpv4Address()
                                if (ip == myIp) return

                                val devName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    resolvedInfo.attributes?.get("deviceName")?.let { String(it, Charsets.UTF_8) }
                                } else {
                                    null
                                } ?: resolvedInfo.serviceName.removePrefix("PeerLink - ")

                                val chatPort = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    resolvedInfo.attributes?.get("chatPort")?.let { String(it, Charsets.UTF_8).toIntOrNull() } ?: -1
                                } else {
                                    -1
                                }

                                val peer = DiscoveredPeer(
                                    serviceName = resolvedInfo.serviceName,
                                    deviceName = devName,
                                    ip = ip,
                                    filePort = resolvedInfo.port,
                                    chatPort = chatPort
                                )

                                synchronized(this@NsdHelper) {
                                    val currentList = _discoveredPeers.value
                                    if (currentList.none { it.ip == peer.ip && it.filePort == peer.filePort }) {
                                        _discoveredPeers.value = currentList + peer
                                        Log.i(tag, "Peer added: ${peer.deviceName} @ ${peer.ip}:${peer.filePort}")
                                    }
                                }
                            }
                        })
                    } catch (e: Exception) {
                        Log.e(tag, "Concurrent resolve requests failed gracefully", e)
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(tag, "Service lost: ${serviceInfo.serviceName}")
                synchronized(this@NsdHelper) {
                    _discoveredPeers.value = _discoveredPeers.value.filter {
                        it.serviceName != serviceInfo.serviceName
                    }
                }
            }
        }

        try {
            manager.discoverServices("_peerlink._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(tag, "Error starting discovery", e)
            _discoveryException.value = com.example.util.PeerLinkException.DiscoveryException("initiating discovery", e)
            isDiscovering = false
        }
    }

    @Synchronized
    fun stopDiscovery() {
        val manager = nsdManager ?: return
        if (!isDiscovering || discoveryListener == null) return
        try {
            manager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            Log.e(tag, "Error stopping discovery", e)
        } finally {
            discoveryListener = null
            isDiscovering = false
            _discoveredPeers.value = emptyList()
        }

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock.release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to release multicast lock", e)
        }
    }
}
