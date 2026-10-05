package com.cropcast.app.data

import java.security.SecureRandom

/**
 * The 4-digit PIN shown while pairing an ESP32. It is also the device id: the firmware publishes
 * to /devices/<pin>. Easy to type, but only 10,000 values, so [FirebaseSensorRepository] skips a
 * PIN that already has a device behind it.
 */
object PairingCode {
    private val random = SecureRandom()

    fun generate(): String = "%04d".format(random.nextInt(10_000))
}
