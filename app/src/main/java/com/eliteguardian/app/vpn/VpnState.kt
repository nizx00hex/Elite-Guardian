package com.eliteguardian.app.vpn

sealed interface VpnStatus {
    object Disconnected : VpnStatus
    object Connecting : VpnStatus
    object Connected : VpnStatus
    data class Error(val message: String) : VpnStatus
}

data class VpnFilterStats(
    val totalQueries: Long = 0,
    val blockedQueries: Long = 0,
    val safeSearchQueries: Long = 0,
    val lastBlockedDomain: String? = null
)

