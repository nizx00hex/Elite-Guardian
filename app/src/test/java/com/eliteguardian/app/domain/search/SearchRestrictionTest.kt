package com.eliteguardian.app.domain.search

import com.eliteguardian.app.data.database.CustomSearchDomainEntity
import com.eliteguardian.app.domain.filtering.DomainMatcher
import com.eliteguardian.app.domain.filtering.DomainValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchRestrictionTest {

    private lateinit var matcher: DomainMatcher

    @Before
    fun setUp() {
        matcher = DomainMatcher()
        // Adult seed list simulation
        matcher.setBlocklist(
            listOf(
                "adultsite.xxx",
                "porn-example.com"
            )
        )
    }

    @Test
    fun testMasterSwitchDisabledBlocksNothingFromSearchEngines() {
        val disabledEngines = setOf("google", "bing")
        val customDomains = listOf(
            CustomSearchDomainEntity(domain = "search.brave.com", isEnabled = true)
        )

        // Effective blocked search domains should be empty when master switch is false and not in focus mode
        val effective = computeEffective(
            masterEnabled = false,
            isFocus = false,
            focusRestricted = true,
            disabledEngines = disabledEngines,
            customList = customDomains
        )
        assertTrue(effective.isEmpty())

        // Adult content must remain blocked regardless
        assertTrue("Adult content must remain blocked when search restrictions disabled", matcher.isBlocked("adultsite.xxx"))
        assertFalse("Google should be allowed when search restrictions are off", matcher.isBlocked("google.com"))
    }

    @Test
    fun testMasterSwitchEnabledBlocksSelectedEngines() {
        val disabledEngines = setOf("google", "bing")
        val customDomains = listOf(
            CustomSearchDomainEntity(domain = "search.brave.com", isEnabled = true)
        )

        val effective = computeEffective(
            masterEnabled = true,
            isFocus = false,
            focusRestricted = true,
            disabledEngines = disabledEngines,
            customList = customDomains
        )

        // Feed effective search domains into matcher (simulating FilterRepository)
        matcher.setBlocklist(listOf("adultsite.xxx") + effective)

        // Verify Google domains are blocked
        assertTrue(matcher.isBlocked("google.com"))
        assertTrue(matcher.isBlocked("www.google.com"))
        assertTrue(matcher.isBlocked("google.co.uk"))

        // Verify Bing domains are blocked
        assertTrue(matcher.isBlocked("bing.com"))
        assertTrue(matcher.isBlocked("www.bing.com"))

        // Verify Custom domain is blocked
        assertTrue(matcher.isBlocked("search.brave.com"))

        // Verify DuckDuckGo is NOT blocked since it was not selected
        assertFalse(matcher.isBlocked("duckduckgo.com"))

        // Verify adult content remains blocked
        assertTrue(matcher.isBlocked("adultsite.xxx"))
    }

    @Test
    fun testFocusModeSearchOverride() {
        // Master switch is OFF, but Focus Mode is ACTIVE and Focus Search Restrictions is ON
        val disabledEngines = setOf("duckduckgo", "yahoo")
        val effective = computeEffective(
            masterEnabled = false,
            isFocus = true,
            focusRestricted = true,
            disabledEngines = disabledEngines,
            customList = emptyList()
        )

        matcher.setBlocklist(listOf("adultsite.xxx") + effective)

        assertTrue("DuckDuckGo must be blocked during focus mode override", matcher.isBlocked("duckduckgo.com"))
        assertTrue("Yahoo must be blocked during focus mode override", matcher.isBlocked("search.yahoo.com"))

        // When Focus mode ends, restrictions automatically revert
        val effectiveAfterFocus = computeEffective(
            masterEnabled = false,
            isFocus = false,
            focusRestricted = true,
            disabledEngines = disabledEngines,
            customList = emptyList()
        )
        matcher.setBlocklist(listOf("adultsite.xxx") + effectiveAfterFocus)

        assertFalse("DuckDuckGo should be unblocked once focus session terminates", matcher.isBlocked("duckduckgo.com"))
    }

    @Test
    fun testCustomSearchDomainValidation() {
        assertNotNull(DomainValidator.normalize("search.brave.com"))
        assertNotNull(DomainValidator.normalize("HTTPS://SEARCH.BRAVE.COM/path?q=1"))
        assertEquals("search.brave.com", DomainValidator.normalize("HTTPS://SEARCH.BRAVE.COM/path?q=1"))

        assertNull(DomainValidator.normalize("invalid_domain_name"))
        assertNull(DomainValidator.normalize(""))
        assertNull(DomainValidator.normalize("   "))
    }

    @Test
    fun testCustomDomainDisabledToggle() {
        val customDomains = listOf(
            CustomSearchDomainEntity(domain = "search.brave.com", isEnabled = false),
            CustomSearchDomainEntity(domain = "ecosia.org", isEnabled = true)
        )

        val effective = computeEffective(
            masterEnabled = true,
            isFocus = false,
            focusRestricted = false,
            disabledEngines = emptySet(),
            customList = customDomains
        )

        assertFalse(effective.contains("search.brave.com"))
        assertTrue(effective.contains("ecosia.org"))
    }

    @Test
    fun testAllowlistPrecedenceOverSearchRestrictions() {
        // Even if google is blocked by search restriction, explicit allowlist entry overrides it
        val effective = computeEffective(
            masterEnabled = true,
            isFocus = false,
            focusRestricted = false,
            disabledEngines = setOf("google"),
            customList = emptyList()
        )

        matcher.setBlocklist(effective)
        matcher.setAllowlist(listOf("google.com"))

        assertFalse("Explicit allowlist must take precedence over search block", matcher.isBlocked("google.com"))
    }

    private fun computeEffective(
        masterEnabled: Boolean,
        isFocus: Boolean,
        focusRestricted: Boolean,
        disabledEngines: Set<String>,
        customList: List<CustomSearchDomainEntity>
    ): Set<String> {
        val shouldApply = masterEnabled || (isFocus && focusRestricted)
        if (!shouldApply) return emptySet()

        val domains = mutableSetOf<String>()
        disabledEngines.forEach { id ->
            val engine = SupportedSearchEngine.fromId(id)
            if (engine != null) {
                domains.addAll(engine.domains)
            }
        }
        customList.filter { it.isEnabled }.forEach { custom ->
            domains.add(custom.domain)
        }
        return domains
    }
}

