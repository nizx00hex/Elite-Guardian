package com.eliteguardian.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArraySet

class KeywordFilteringTest {

    private val keywordSet = CopyOnWriteArraySet<String>()

    @Before
    fun setUp() {
        keywordSet.clear()
        listOf("porn", "xxx", "hentai", "sex", "nude", "erotic", "nsfw").forEach {
            keywordSet.add(it.lowercase())
        }
    }

    private fun checkKeyword(text: String): String? {
        val lower = text.lowercase()
        for (kw in keywordSet) {
            if (lower.contains(kw)) {
                return kw
            }
        }
        return null
    }

    @Test
    fun testExactKeywordMatch() {
        assertEquals("porn", checkKeyword("porn"))
        assertEquals("xxx", checkKeyword("XXX"))
        assertEquals("hentai", checkKeyword("Hentai"))
    }

    @Test
    fun testKeywordWithinSentence() {
        assertEquals("porn", checkKeyword("how to watch porn online"))
        assertEquals("nude", checkKeyword("celebrity nude photos"))
        assertEquals("sex", checkKeyword("search sexy models"))
    }

    @Test
    fun testCleanTextNotMatched() {
        assertNull(checkKeyword("android jetpack compose tutorial"))
        assertNull(checkKeyword("quantum mechanics study notes"))
        assertNull(checkKeyword("google maps navigation"))
    }
}

