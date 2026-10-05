package com.cropcast.app.data

import com.cropcast.app.data.model.SensorReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmAdviceRepositoryTest {
    private val probe = SensorReading(
        temperature = 27.0, humidity = 80.0, soilMoisture = 62.5, soilPh = 6.1,
        nitrogen = 16.0, phosphorus = 24.0, potassium = 55.0, timestamp = 1L, electricalConductivity = 344.0
    )
    private val site = FarmSiteData(normals = ClimateNormals(List(12) { 24.0 }, List(12) { 80.0 }, List(12) { 200.0 }))

    @Test
    fun factsHoldTopThreeCropsAndSoilPlanOnly() {
        val recommendation = FarmSuitabilityEngine.recommend(probe, site, plantingMonth = 1, latitude = 10.7)
        val facts = FarmAdviceRepository.buildFacts(recommendation)

        recommendation.ranked.take(3).forEach { assertTrue(it.crop.name, facts.contains(it.crop.name)) }
        assertTrue(facts.contains("Soil "))
        assertEquals(3, facts.lines().count { it.matches(Regex("""^\d\. .*""")) })
        // The latitude passed to the engine must never reach the prompt.
        assertTrue(!facts.contains("10.7"))
    }
}
