package com.eliteguardian.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinManagerTest {

    @Test
    fun testSaltGeneration() {
        val salt1 = PinManager.generateSalt()
        val salt2 = PinManager.generateSalt()
        assertNotEquals(salt1, salt2)
        assertTrue(salt1.length >= 32)
    }

    @Test
    fun testPinVerificationSuccess() {
        val pin = "1234"
        val salt = PinManager.generateSalt()
        val hash = PinManager.hashPin(pin, salt)

        assertTrue(PinManager.verifyPin("1234", salt, hash))
    }

    @Test
    fun testPinVerificationFailure() {
        val pin = "1234"
        val salt = PinManager.generateSalt()
        val hash = PinManager.hashPin(pin, salt)

        assertFalse(PinManager.verifyPin("0000", salt, hash))
        assertFalse(PinManager.verifyPin("12345", salt, hash))
        assertFalse(PinManager.verifyPin("", salt, hash))
    }
}

