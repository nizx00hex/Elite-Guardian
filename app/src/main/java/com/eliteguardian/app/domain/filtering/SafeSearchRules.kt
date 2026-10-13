package com.eliteguardian.app.domain.filtering

import java.net.InetAddress

object SafeSearchRules {

    // Official SafeSearch VIP IPv4 addresses
    val GOOGLE_SAFESEARCH_IPV4: ByteArray = byteArrayOf(216.toByte(), 239.toByte(), 38.toByte(), 120.toByte())
    val BING_SAFESEARCH_IPV4: ByteArray = byteArrayOf(204.toByte(), 79.toByte(), 197.toByte(), 220.toByte())
    val YOUTUBE_RESTRICTED_IPV4: ByteArray = byteArrayOf(216.toByte(), 239.toByte(), 38.toByte(), 119.toByte())
    val DUCKDUCKGO_SAFESEARCH_IPV4: ByteArray = byteArrayOf(52.toByte(), 142.toByte(), 124.toByte(), 215.toByte())

    /**
     * Returns true if SafeSearch redirect rule applies to the normalized domain.
     */
    fun getSafeSearchIp(normalizedDomain: String): ByteArray? {
        val domain = normalizedDomain.lowercase()

        // Google SafeSearch
        if (domain == "google.com" || domain.endsWith(".google.com") ||
            domain.startsWith("google.") || domain.contains(".google.")
        ) {
            // Exclude non-search services like accounts or apis
            if (!domain.startsWith("accounts.") && !domain.startsWith("play.") && !domain.startsWith("apis.")) {
                return GOOGLE_SAFESEARCH_IPV4
            }
        }

        // Bing SafeSearch
        if (domain == "bing.com" || domain.endsWith(".bing.com")) {
            return BING_SAFESEARCH_IPV4
        }

        // YouTube Restricted Mode
        if (domain == "youtube.com" || domain.endsWith(".youtube.com") ||
            domain == "m.youtube.com" || domain == "youtubei.googleapis.com"
        ) {
            return YOUTUBE_RESTRICTED_IPV4
        }

        // DuckDuckGo SafeSearch
        if (domain == "duckduckgo.com" || domain.endsWith(".duckduckgo.com")) {
            return DUCKDUCKGO_SAFESEARCH_IPV4
        }

        return null
    }
}

