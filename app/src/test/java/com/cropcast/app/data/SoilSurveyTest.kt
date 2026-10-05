package com.cropcast.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class SoilSurveyTest {

    // A 10x10 square (lon 0-10, lat 0-10) with a 2x2 hole at lon 4-6, lat 4-6.
    private val square = """{"soils":[{"name":"Test clay","ph":5.5,"clay":60,"sand":20,"rings":[
        [0,0,10,0,10,10,0,10,0,0],[4,4,6,4,6,6,4,6,4,4]]}]}"""

    @Test
    fun findsTheSoilAndRespectsHoles() {
        val survey = SoilSurvey.parse(square)
        val soil = survey.lookup(latitude = 2.0, longitude = 2.0)!!
        assertEquals(5.5, soil.ph!!, 0.001)
        assertEquals(60.0, soil.clayPct!!, 0.001)
        assertEquals(SoilSurvey.SOURCE, soil.phSource)
        assertNull(survey.lookup(latitude = 5.0, longitude = 5.0))  // in the hole
        assertNull(survey.lookup(latitude = 20.0, longitude = 2.0))  // outside
    }

    @Test
    fun surveyOnlyFillsWhatSoilGridsLacks() {
        val survey = SoilMap(ph = 6.0, clayPct = 34.0, sandPct = 32.0, nitrogenGPerKg = null,
            phSource = SoilSurvey.SOURCE, textureSource = SoilSurvey.SOURCE)
        val full = SoilMap(ph = 5.0, clayPct = 20.0, sandPct = 40.0, nitrogenGPerKg = 2.0)
        assertEquals(full, SoilSurvey.merge(full, survey))

        val noPh = SoilSurvey.merge(full.copy(ph = null), survey)!!
        assertEquals(6.0, noPh.ph!!, 0.001)
        assertEquals(SoilSurvey.SOURCE, noPh.phSource)
        assertEquals(20.0, noPh.clayPct!!, 0.001)
        assertEquals(FarmSiteDataRepository.SOILGRIDS, noPh.textureSource)

        assertEquals(survey, SoilSurvey.merge(null, survey))  // SoilGrids unavailable
        assertEquals(full, SoilSurvey.merge(full, null))
    }

    @Test
    fun builtAssetKnowsTheSoilAtAlimodianAndIloiloCity() {
        val asset = listOf(File("src/main/assets/iloilo_soil_series.json"), File("app/src/main/assets/iloilo_soil_series.json"))
            .first { it.exists() }
        val survey = SoilSurvey.parse(asset.readText())
        // Alimodian town sits on Alimodian clay loam: pH 6.0 in Table 15, medium texture.
        val alimodian = survey.lookup(10.82, 122.43)
        assertNotNull(alimodian)
        assertEquals(6.0, alimodian!!.ph!!, 0.001)
        assertEquals('M', FarmSuitabilityEngine.textureClass(alimodian.clayPct!!, alimodian.sandPct!!))
        // Iloilo City is on Sta. Rita clay: pH 6.5, heavy texture.
        val city = survey.lookup(10.72, 122.56)!!
        assertEquals(6.5, city.ph!!, 0.001)
        assertEquals('H', FarmSuitabilityEngine.textureClass(city.clayPct!!, city.sandPct!!))
        assertNull(survey.lookup(14.6, 121.0))  // Manila: outside the window
    }
}
