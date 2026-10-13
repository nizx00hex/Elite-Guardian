package com.eliteguardian.app.domain.filtering

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class DomainMatcher {

    private val allowlist = CopyOnWriteArraySet<String>()
    private val blocklist = CopyOnWriteArraySet<String>()
    private val lookupCache = ConcurrentHashMap<String, MatchResult>()

    enum class MatchResult {
        ALLOWED_BY_RULE,
        BLOCKED_BY_RULE,
        NOT_MATCHED
    }

    /**
     * Updates the active allowlist.
     */
    fun setAllowlist(domains: Collection<String>) {
        allowlist.clear()
        domains.mapNotNull { DomainValidator.normalize(it) }.forEach { allowlist.add(it) }
        lookupCache.clear()
    }

    /**
     * Updates the active blocklist (seed + user custom).
     */
    fun setBlocklist(domains: Collection<String>) {
        blocklist.clear()
        domains.mapNotNull { DomainValidator.normalize(it) }.forEach { blocklist.add(it) }
        lookupCache.clear()
    }

    fun addBlockItem(domain: String) {
        DomainValidator.normalize(domain)?.let {
            blocklist.add(it)
            lookupCache.clear()
        }
    }

    fun addAllowItem(domain: String) {
        DomainValidator.normalize(domain)?.let {
            allowlist.add(it)
            lookupCache.clear()
        }
    }

    /**
     * Determines whether a given query domain should be blocked.
     * Returns true if the domain or any of its parent domain suffixes is blocked,
     * unless an explicit allowlist entry overrides it.
     */
    fun isBlocked(rawDomain: String?): Boolean {
        val normalized = DomainValidator.normalize(rawDomain) ?: return false

        // Check local memory cache
        lookupCache[normalized]?.let { cached ->
            return cached == MatchResult.BLOCKED_BY_RULE
        }

        // Diagnostic domain is always blocked for filter verification
        if (normalized == "test.adult.eliteguardian.local") {
            lookupCache[normalized] = MatchResult.BLOCKED_BY_RULE
            return true
        }

        // Split domain into label parts, e.g. "sub.example.com" -> ["sub", "example", "com"]
        val labels = normalized.split('.')
        if (labels.size < 2) {
            lookupCache[normalized] = MatchResult.NOT_MATCHED
            return false
        }

        // Generate suffixes from most specific to least specific:
        // "sub.example.com", "example.com"
        // 1. Allowlist precedence check: if any matching suffix is in allowlist, it is allowed!
        for (i in 0 until labels.size - 1) {
            val suffix = labels.subList(i, labels.size).joinToString(".")
            if (allowlist.contains(suffix)) {
                lookupCache[normalized] = MatchResult.ALLOWED_BY_RULE
                return false
            }
        }

        // 2. Blocklist check: if any matching suffix is in blocklist, it is blocked!
        for (i in 0 until labels.size - 1) {
            val suffix = labels.subList(i, labels.size).joinToString(".")
            if (blocklist.contains(suffix)) {
                lookupCache[normalized] = MatchResult.BLOCKED_BY_RULE
                return true
            }
        }

        lookupCache[normalized] = MatchResult.NOT_MATCHED
        return false
    }

    fun getBlocklistCount(): Int = blocklist.size
    fun getAllowlistCount(): Int = allowlist.size
}

