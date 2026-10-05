package com.cropcast.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

class PairingCodeTest {
    @Test
    fun `code is four digits, which the firmware rule accepts`() {
        // Firmware validPairingCode(): 4-40 of [A-Za-z0-9_-].
        repeat(500) {
            val code = PairingCode.generate()
            assertTrue(code, Regex("\\d{4}").matches(code))
        }
    }
}
