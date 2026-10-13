package com.eliteguardian.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.eliteguardian.app.MainActivity
import com.eliteguardian.app.R
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.data.repository.FilterRepository
import com.eliteguardian.app.domain.filtering.SafeSearchRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.nio.ByteBuffer

class GuardianVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private var upstreamSocket: DatagramSocket? = null

    private lateinit var filterRepo: FilterRepository
    private lateinit var appPreferences: AppPreferences
    private lateinit var packetHandler: DnsPacketHandler

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        const val ACTION_START = "com.eliteguardian.app.START_VPN"
        const val ACTION_STOP = "com.eliteguardian.app.STOP_VPN"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "elite_guardian_vpn_channel"

        private val _vpnStatus = MutableStateFlow<VpnStatus>(VpnStatus.Disconnected)
        val vpnStatus: StateFlow<VpnStatus> = _vpnStatus.asStateFlow()

        private val _stats = MutableStateFlow(VpnFilterStats())
        val stats: StateFlow<VpnFilterStats> = _stats.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, GuardianVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, GuardianVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        filterRepo = FilterRepository.getInstance(applicationContext)
        appPreferences = AppPreferences(applicationContext)
        packetHandler = DnsPacketHandler(filterRepo.matcher, isSafeSearchEnabled = true)
        createNotificationChannel()
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startFiltering()
            ACTION_STOP -> stopFiltering()
        }
        return START_STICKY
    }

    private fun startFiltering() {
        if (isRunning) return
        _vpnStatus.value = VpnStatus.Connecting

        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        serviceScope.launch {
            try {
                val isSafeSearch = appPreferences.isSafeSearchEnabled.first()
                packetHandler = DnsPacketHandler(filterRepo.matcher, isSafeSearchEnabled = isSafeSearch)

                val builder = Builder()
                    .setSession("EliteGuardian")
                    .setMtu(1500)
                    .addAddress("10.254.0.1", 30)
                    .addDnsServer("10.254.0.1")
                    .addRoute("10.254.0.0", 30)

                // Optional: Intercept SafeSearch search IPs
                builder.addRoute("216.239.38.120", 32) // Google SafeSearch VIP
                builder.addRoute("204.79.197.220", 32) // Bing SafeSearch VIP

                // Allow app itself to bypass VPN to prevent traffic loops
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    // Ignored if unsupported on some OEM ROMs
                }

                vpnInterface = builder.establish()
                if (vpnInterface == null) {
                    _vpnStatus.value = VpnStatus.Error("Failed to establish VPN interface")
                    stopSelf()
                    return@launch
                }

                upstreamSocket = DatagramSocket().apply {
                    protect(this) // Crucial: protect upstream socket from VPN routing
                }

                isRunning = true
                _vpnStatus.value = VpnStatus.Connected
                appPreferences.setProtectionEnabled(true)
                appPreferences.setLastFilterInitTime(System.currentTimeMillis())

                runPacketProcessingLoop()
            } catch (e: Exception) {
                e.printStackTrace()
                _vpnStatus.value = VpnStatus.Error(e.message ?: "VPN initialization error")
                stopFiltering()
            }
        }
    }

    private fun runPacketProcessingLoop() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val packetBuffer = ByteArray(32767)

        while (isRunning) {
            try {
                val length = inputStream.read(packetBuffer)
                if (length <= 0) continue

                // Check for IPv4 UDP packet
                val version = (packetBuffer[0].toInt() shr 4) and 0x0F
                if (version != 4) continue // Only IPv4 for now

                val ihl = (packetBuffer[0].toInt() and 0x0F) * 4
                val protocol = packetBuffer[9].toInt() and 0xFF
                if (protocol != 17) continue // UDP only

                val srcIp = packetBuffer.copyOfRange(12, 16)
                val dstIp = packetBuffer.copyOfRange(16, 20)

                val udpOffset = ihl
                if (length < udpOffset + 8) continue

                val srcPort = ((packetBuffer[udpOffset].toInt() and 0xFF) shl 8) or
                        (packetBuffer[udpOffset + 1].toInt() and 0xFF)
                val dstPort = ((packetBuffer[udpOffset + 2].toInt() and 0xFF) shl 8) or
                        (packetBuffer[udpOffset + 3].toInt() and 0xFF)

                // Check if destination is DNS port 53
                if (dstPort == 53) {
                    val dnsOffset = udpOffset + 8
                    val dnsLength = length - dnsOffset
                    val dnsQuery = packetHandler.parseDnsQuery(packetBuffer.copyOfRange(dnsOffset, length), dnsLength)

                    if (dnsQuery != null) {
                        _stats.value = _stats.value.copy(
                            totalQueries = _stats.value.totalQueries + 1
                        )

                        when (packetHandler.evaluate(dnsQuery.qname)) {
                            DnsPacketHandler.Decision.BLOCK -> {
                                // Synthesize sinkhole response
                                val sinkholeIp = byteArrayOf(0, 0, 0, 0)
                                val dnsResponse = packetHandler.synthesizeDnsResponse(dnsQuery, sinkholeIp)
                                val responsePacket = packetHandler.buildIpv4UdpPacket(
                                    srcIp = dstIp,
                                    dstIp = srcIp,
                                    srcPort = dstPort,
                                    dstPort = srcPort,
                                    payload = dnsResponse
                                )
                                outputStream.write(responsePacket)
                                outputStream.flush()

                                _stats.value = _stats.value.copy(
                                    blockedQueries = _stats.value.blockedQueries + 1,
                                    lastBlockedDomain = dnsQuery.qname
                                )
                                serviceScope.launch { appPreferences.incrementBlockedCount() }
                            }
                            DnsPacketHandler.Decision.SAFESEARCH -> {
                                val safeSearchIp = SafeSearchRules.getSafeSearchIp(dnsQuery.qname)
                                val dnsResponse = packetHandler.synthesizeDnsResponse(dnsQuery, safeSearchIp)
                                val responsePacket = packetHandler.buildIpv4UdpPacket(
                                    srcIp = dstIp,
                                    dstIp = srcIp,
                                    srcPort = dstPort,
                                    dstPort = srcPort,
                                    payload = dnsResponse
                                )
                                outputStream.write(responsePacket)
                                outputStream.flush()

                                _stats.value = _stats.value.copy(
                                    safeSearchQueries = _stats.value.safeSearchQueries + 1
                                )
                            }
                            DnsPacketHandler.Decision.FORWARD -> {
                                // Forward to upstream protected resolver
                                upstreamSocket?.let { sock ->
                                    val upstreamResponse = packetHandler.forwardUpstream(sock, dnsQuery)
                                    if (upstreamResponse != null) {
                                        val responsePacket = packetHandler.buildIpv4UdpPacket(
                                            srcIp = dstIp,
                                            dstIp = srcIp,
                                            srcPort = dstPort,
                                            dstPort = srcPort,
                                            payload = upstreamResponse
                                        )
                                        outputStream.write(responsePacket)
                                        outputStream.flush()
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (!isRunning) break
            }
        }
    }

    private fun stopFiltering() {
        isRunning = false
        _vpnStatus.value = VpnStatus.Disconnected
        try {
            upstreamSocket?.close()
            upstreamSocket = null
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serviceScope.launch {
            appPreferences.setProtectionEnabled(false)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopFiltering()
        super.onRevoke()
    }

    override fun onDestroy() {
        isRunning = false
        unregisterNetworkCallback()
        serviceScope.cancel()
        try {
            upstreamSocket?.close()
            vpnInterface?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _vpnStatus.value = VpnStatus.Disconnected
        super.onDestroy()
    }

    private fun registerNetworkCallback() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                upstreamSocket?.let { sock ->
                    try {
                        protect(sock)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager?.registerNetworkCallback(request, networkCallback!!)
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let { callback ->
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.vpn_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_running_title))
            .setContentText(getString(R.string.vpn_running_desc))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

