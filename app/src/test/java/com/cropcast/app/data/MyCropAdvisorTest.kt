package com.cropcast.app.data

import com.cropcast.app.data.model.SensorReading
import com.cropcast.app.ui.localization.translateFilipino
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MyCropAdvisorTest {
    private val today = LocalDate.of(2026, 10, 29)

    private fun reading(temp: Double = 29.4, hum: Double = 84.0, moist: Double = 34.0, ph: Double = 7.3, n: Double = 62.0, p: Double = 6.0, k: Double = 50.0) =
        SensorReading(temp, hum, moist, ph, n, p, k, 38500.0, timestamp = 1L)

    @Test
    fun tomatoAtDay45IsFloweringWithLowNutrients() {
        val advice = MyCropAdvisor.advise("Tomato", today.minusDays(45), today, reading())!!
        assertEquals(45, advice.day)
        assertEquals("Flowering and fruiting", advice.stage)
        assertEquals(listOf("Nitrogen", "Phosphorus", "Potassium"), advice.lowNutrients)
        assertTrue(advice.phHigh)
        assertTrue(advice.actions.any { it.title == "Add fertilizer" })
        // Common mix: Complete sized to the P need, topped up with ammonium sulfate (high pH) and potash.
        assertEquals(listOf("Complete", "Ammonium sulfate", "Muriate of potash"), advice.mix.map { it.name })
        assertEquals(listOf("Ammonium sulfate", "Solophos", "Muriate of potash"), advice.single.map { it.name })
    }

    @Test
    fun nearHarvestSkipsFertilizerAction() {
        val advice = MyCropAdvisor.advise("Tomato", today.minusDays(78), today, reading())!!
        assertTrue(advice.nearHarvest)
        assertTrue(advice.actions.any { it.title == "No more fertilizer" })
        assertTrue(advice.actions.none { it.title == "Add fertilizer" })
    }

    @Test
    fun riceNearHarvestIsDrainedNotFlagged() {
        val advice = MyCropAdvisor.advise("Rice", today.minusDays(110), today, reading(moist = 30.0))!!
        assertEquals(CheckLevel.OK, advice.checks.first { it.name == "Soil moisture" }.level)
        assertTrue(advice.actions.none { it.title == "Flood the field" })
    }

    @Test
    fun cropWithoutFertilizerRateHasNoPlan() {
        val advice = MyCropAdvisor.advise("Chili", today.minusDays(30), today, reading())!!
        assertTrue(advice.mix.isEmpty() && advice.single.isEmpty())
        assertEquals(4, advice.checks.size)
    }

    @Test
    fun unknownCropAndFutureDate() {
        assertNull(MyCropAdvisor.advise("", today, today, reading()))
        assertEquals(0, MyCropAdvisor.advise("Tomato", today.plusDays(5), today, reading())!!.day)
    }

    @Test
    fun everyCropAndStageHasFilipinoText() {
        val untranslated = linkedSetOf<String>()
        val readings = listOf(reading(), reading(temp = 15.0, hum = 50.0, moist = 90.0, ph = 4.0, n = 300.0, p = 40.0, k = 200.0), reading(temp = 24.0, hum = 70.0, moist = 55.0, ph = 6.2, n = 150.0, p = 15.0, k = 100.0))
        for (crop in FarmSuitabilityEngine.CROPS) for (day in listOf(2, 30, 55, 100)) for (r in readings) {
            val advice = MyCropAdvisor.advise(crop.name, today.minusDays(day.toLong()), today, r) ?: continue
            assertNotNull(advice)
            buildList {
                add(advice.stage)
                advice.actions.forEach { addAll(listOf(it.title, it.why, it.how)) }
                (advice.mix + advice.single).forEach { add(it.whenText) }
                advice.checks.forEach { it.note?.let(::add) }
            }.filter { it != "Normal" && translateFilipino(it) == it }.forEach(untranslated::add)
        }
        assertTrue("No Filipino text for: $untranslated", untranslated.isEmpty())
    }
}
