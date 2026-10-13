package com.eliteguardian.app.domain.filtering

import java.net.IDN
import java.util.Locale

object DomainValidator {

    private val DOMAIN_REGEX = Regex(
        "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$"
    )

    /**
     * Sanitizes and normalizes an input domain string.
     * Strips URL schemes, ports, path components, trailing dots, and trims whitespace.
     * Converts to punycode / ASCII lowercase.
     */
    fun normalize(input: String?): String? {
        if (input.isNullOrBlank()) return null
        var cleaned = input.trim().lowercase(Locale.ROOT)

        // Strip scheme (http://, https://, etc.)
        val schemeIndex = cleaned.indexOf("://")
        if (schemeIndex != -1) {
            cleaned = cleaned.substring(schemeIndex + 3)
        }

        // Strip path or query
        val slashIndex = cleaned.indexOf('/')
        if (slashIndex != -1) {
            cleaned = cleaned.substring(0, slashIndex)
        }
        val queryIndex = cleaned.indexOf('?')
        if (queryIndex != -1) {
            cleaned = cleaned.substring(0, queryIndex)
        }

        // Strip port
        val colonIndex = cleaned.indexOf(':')
        if (colonIndex != -1) {
            cleaned = cleaned.substring(0, colonIndex)
        }

        // Strip trailing dots
        while (cleaned.endsWith('.')) {
            cleaned = cleaned.substring(0, cleaned.length - 1)
        }

        if (cleaned.isBlank() || cleaned.length > 253) return null

        return try {
            val ascii = IDN.toASCII(cleaned, IDN.ALLOW_UNASSIGNED).lowercase(Locale.ROOT)
            if (isValidNormalizedDomain(ascii)) ascii else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a domain string is structurally valid.
     */
    fun isValid(domain: String?): Boolean {
        return normalize(domain) != null
    }

    private fun isValidNormalizedDomain(domain: String): Boolean {
        if (domain.isBlank() || domain.length > 253) return false
        // Allow local diagnostic testing domain
        if (domain.endsWith(".local") && domain.split('.').size >= 2) return true
        return DOMAIN_REGEX.matches(domain)
    }
}

