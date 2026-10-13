package com.eliteguardian.app.domain.filtering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DomainFilteringTest {

    private lateinit var matcher: DomainMatcher

    @Before
    fun setUp() {
        matcher = DomainMatcher()
        matcher.setBlocklist(
            listOf(
                "adultsite.com",
                "bad-hub.xxx",
                "tracker.net"
            )
        )
    }

    @Test
    fun testExactDomainMatching() {
        assertTrue(matcher.isBlocked("adultsite.com"))
        assertTrue(matcher.isBlocked("bad-hub.xxx"))
        assertTrue(matcher.isBlocked("tracker.net"))
    }

    @Test
    fun testSubdomainMatching() {
        assertTrue(matcher.isBlocked("sub.adultsite.com"))
        assertTrue(matcher.isBlocked("api.v2.sub.adultsite.com"))
        assertTrue(matcher.isBlocked("cdn.bad-hub.xxx"))
    }

    @Test
    fun testUnrelatedDomainsNotBlocked() {
        assertFalse(matcher.isBlocked("notadultsite.com"))
        assertFalse(matcher.isBlocked("adultsite.com.org"))
        assertFalse(matcher.isBlocked("adultsite.org"))
        assertFalse(matcher.isBlocked("google.com"))
        assertFalse(matcher.isBlocked("github.com"))
    }

    @Test
    fun testAllowlistPrecedenceOverBlocklist() {
        // Block whole domain
        matcher.setBlocklist(listOf("example.com"))
        assertTrue(matcher.isBlocked("example.com"))
        assertTrue(matcher.isBlocked("api.example.com"))

        // Add specific subdomain to allowlist
        matcher.setAllowlist(listOf("api.example.com"))
        assertFalse("Allowlist must take precedence on exact match", matcher.isBlocked("api.example.com"))
        assertTrue("Parent domain should still be blocked", matcher.isBlocked("example.com"))
        assertTrue("Other subdomains should still be blocked", matcher.isBlocked("other.example.com"))
    }

    @Test
    fun testDiagnosticDomain() {
        assertTrue("Diagnostic test domain must always be blocked", matcher.isBlocked("test.adult.eliteguardian.local"))
    }

    @Test
    fun testDomainValidatorNormalization() {
        assertEquals("example.com", DomainValidator.normalize("http://example.com/some/path?query=1"))
        assertEquals("example.com", DomainValidator.normalize("HTTPS://EXAMPLE.COM:8080/"))
        assertEquals("example.com", DomainValidator.normalize("  example.com.  "))
        assertEquals("test.local", DomainValidator.normalize("test.local"))
    }

    @Test
    fun testMalformedDomains() {
        assertNull(DomainValidator.normalize(""))
        assertNull(DomainValidator.normalize("   "))
        assertNull(DomainValidator.normalize("invalid_domain_without_tld"))
        assertNull(DomainValidator.normalize("-invalid.com"))
        assertNull(DomainValidator.normalize("http://"))
    }

    @Test
    fun testInternationalizedDomainNames() {
        // German umlaut: münchen.de -> xn--mnchen-3ya.de
        val normalized = DomainValidator.normalize("münchen.de")
        assertNotNull(normalized)
        assertEquals("xn--mnchen-3ya.de", normalized)

        matcher.setBlocklist(listOf("münchen.de"))
        assertTrue(matcher.isBlocked("münchen.de"))
        assertTrue(matcher.isBlocked("xn--mnchen-3ya.de"))
    }

    @Test
    fun testSafeSearchRules() {
        val googleIp = SafeSearchRules.getSafeSearchIp("google.com")
        assertNotNull(googleIp)
        assertEquals(216.toByte(), googleIp!![0])

        val bingIp = SafeSearchRules.getSafeSearchIp("www.bing.com")
        assertNotNull(bingIp)

        val ddgIp = SafeSearchRules.getSafeSearchIp("duckduckgo.com")
        assertNotNull(ddgIp)

        val cleanIp = SafeSearchRules.getSafeSearchIp("wikipedia.org")
        assertNull(cleanIp)
    }
}

