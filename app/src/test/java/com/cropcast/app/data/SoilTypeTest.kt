package com.cropcast.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SoilTypeTest {
    @Test
    fun `soil type class centres land in the intended texture class`() {
        val expected = mapOf("Clay" to 'H', "Clay loam" to 'M', "Loam" to 'M', "Sandy loam" to 'M', "Sand" to 'L')
        FarmSuitabilityEngine.SOIL_TYPES.forEach { (name, centre) ->
            assertEquals(name, expected[name], FarmSuitabilityEngine.textureClass(centre.first, centre.second))
        }
    }

    @Test
    fun `chosen soil type replaces the mapped texture but keeps the mapped pH`() {
        val mapped = FarmSiteData(soil = SoilMap(ph = 5.7, clayPct = 42.0, sandPct = 20.0, nitrogenGPerKg = null))
        val soil = FarmSuitabilityEngine.withSoilType(mapped, "Sand")!!.soil!!
        assertEquals(85.0, soil.sandPct!!, 0.0)
        assertEquals(5.7, soil.ph!!, 0.0)
        assertEquals(FarmSuitabilityEngine.USER_SOIL_SOURCE, soil.textureSource)
    }

    @Test
    fun `empty or unknown soil type leaves the map alone`() {
        val mapped = FarmSiteData(soil = SoilMap(ph = 5.7, clayPct = 42.0, sandPct = 20.0, nitrogenGPerKg = null))
        assertSame(mapped, FarmSuitabilityEngine.withSoilType(mapped, ""))
        assertNull(FarmSuitabilityEngine.withSoilType(null, ""))
    }

    @Test
    fun `soil type works before any map has loaded`() {
        val soil = FarmSuitabilityEngine.withSoilType(null, "Clay")!!.soil!!
        assertEquals(45.0, soil.clayPct!!, 0.0)
    }
}
