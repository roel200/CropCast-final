package com.cropcast.app.data

import com.cropcast.app.data.model.SensorReading
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sixteen Panay and non-Panay farms run through the real engine with the BSWM survey merged in.
 * Asserts how pH and texture are sourced, and writes each farm's advice facts to
 * app/build/advice-cases/ so scripts/run_advice_cases.cmd can send them to the live AI worker.
 */
class AdviceCasesTest {
    private fun asset(name: String) = listOf(File("src/main/assets/$name"), File("app/src/main/assets/$name")).first { it.exists() }

    private val survey = SoilSurvey.parse(asset("iloilo_soil_series.json").readText())
    private val economics = FarmSuitabilityEngine.parseEconomics(asset("crop_economics.json").readText())

    // Typical Panay: warm all year, wet June to October.
    private val normals = ClimateNormals(
        temperatureC = listOf(26.0, 26.3, 27.2, 28.4, 28.6, 28.0, 27.5, 27.4, 27.4, 27.4, 27.0, 26.4),
        humidityPct = List(12) { 80.0 },
        rainfallMm = listOf(80.0, 50.0, 50.0, 60.0, 150.0, 270.0, 330.0, 340.0, 330.0, 300.0, 200.0, 120.0)
    )

    private class Case(
        val name: String, val lat: Double, val lon: Double, val month: Int,
        val irrigated: Boolean = false, val probePh: Double? = null,
        val expectPh: Double?, val expectSource: String?
    )

    private val cases = listOf(
        Case("alimodian_town", 10.82, 122.43, 6, expectPh = 6.0, expectSource = SoilSurvey.SOURCE),
        Case("iloilo_city", 10.72, 122.56, 7, expectPh = 6.5, expectSource = SoilSurvey.SOURCE),
        Case("sara_acid_soil", 11.448, 123.124, 6, expectPh = 4.5, expectSource = SoilSurvey.SOURCE),
        Case("alimodian_silt_dry_season", 10.943, 122.472, 11, expectPh = 5.0, expectSource = SoilSurvey.SOURCE),
        Case("umingan_clay_no_ph", 11.023, 122.657, 8, expectPh = null, expectSource = null),
        Case("manila_outside_survey", 14.6, 121.0, 6, expectPh = null, expectSource = null),
        Case("bantog_irrigated_january", 11.128, 122.606, 1, irrigated = true, expectPh = 5.0, expectSource = SoilSurvey.SOURCE),
        Case("sara_with_probe_ph", 11.448, 123.124, 6, probePh = 6.8, expectPh = 6.8, expectSource = FarmSuitabilityEngine.SOURCE_PROBE),
        // Outside the Iloilo survey (no pH from maps) and extreme probe readings; Panay normals reused for every site.
        Case("benguet_highland_dec", 16.40, 120.60, 12, expectPh = null, expectSource = null),
        Case("davao_lowland_april", 7.07, 125.61, 4, expectPh = null, expectSource = null),
        Case("palawan_irrigated_march", 9.74, 118.74, 3, irrigated = true, expectPh = null, expectSource = null),
        Case("cebu_dry_season_feb", 10.32, 123.89, 2, expectPh = null, expectSource = null),
        Case("iloilo_very_acid_probe", 10.82, 122.43, 6, probePh = 3.9, expectPh = 3.9, expectSource = FarmSuitabilityEngine.SOURCE_PROBE),
        Case("iloilo_alkaline_probe", 10.72, 122.56, 7, probePh = 8.6, expectPh = 8.6, expectSource = FarmSuitabilityEngine.SOURCE_PROBE),
        Case("sara_irrigated_dry_season", 11.448, 123.124, 2, irrigated = true, expectPh = 4.5, expectSource = SoilSurvey.SOURCE),
        Case("bantog_probe_neutral_wet_season", 11.128, 122.606, 8, probePh = 6.6, expectPh = 6.6, expectSource = FarmSuitabilityEngine.SOURCE_PROBE)
    )

    private fun probe(ph: Double) = SensorReading(
        temperature = 28.0, humidity = 80.0, soilMoisture = 60.0, soilPh = ph,
        nitrogen = 16.0, phosphorus = 24.0, potassium = 55.0, timestamp = 1L
    )

    @Test
    fun surveyFeedsTheEngineAndAdviceFactsAreSafe() {
        val out = File("build/advice-cases").apply { mkdirs() }
        for (case in cases) {
            val soil = survey.lookup(case.lat, case.lon)
            val site = FarmSiteData(
                normals = normals,
                outlook = WeatherOutlook(27.5, 80.0, 60.0, 16),
                soil = soil,
                status = FarmSiteDataRepository.SOURCES.associateWith { SourceStatus.CACHED },
                elevationM = 20.0
            )
            val recommendation = FarmSuitabilityEngine.recommend(
                soil = case.probePh?.let(::probe), site = site, plantingMonth = case.month,
                irrigated = case.irrigated, latitude = case.lat, economics = economics
            )
            assertTrue("${case.name}: no crops ranked", recommendation.ranked.isNotEmpty())
            assertTrue("${case.name}: scores out of range", recommendation.ranked.all { it.score in 0..100 })

            val ph = recommendation.ranked.first().factors.firstOrNull { it.name == "Soil pH" }
            if (case.expectPh == null) {
                assertNull("${case.name}: unexpected pH factor", ph)
            } else {
                assertNotNull("${case.name}: missing pH factor", ph)
                assertTrue("${case.name}: ${ph!!.detail}", ph.detail.startsWith("pH %.1f".format(case.expectPh)))
                assertEquals(case.name, case.expectSource, ph.source)
            }
            // The survey only gives a texture class, so it must never print a made-up clay %.
            recommendation.ranked.flatMap { it.factors }.filter { it.name == "Soil texture" && it.source == SoilSurvey.SOURCE }
                .forEach { assertFalse("${case.name}: ${it.detail}", it.detail.contains("% clay")) }

            val facts = FarmAdviceRepository.buildFacts(recommendation)
            assertTrue("${case.name}: facts empty or over the worker's 4000-char limit", facts.length in 1..4000)
            assertFalse("${case.name}: coordinates leaked", facts.contains(case.lat.toString()) || facts.contains(case.lon.toString()))
            File(out, "${case.name}.txt").writeText(facts)
        }
    }

    @Test
    fun acidSaraSoilLowersTheScoreOfAcidSensitiveCropsComparedWithAlimodian() {
        fun best(lat: Double, lon: Double, crop: String): Int {
            val site = FarmSiteData(normals = normals, soil = survey.lookup(lat, lon), elevationM = 20.0)
            return FarmSuitabilityEngine.recommend(null, site, 6, economics = economics)
                .ranked.first { it.crop.name == crop }.score
        }
        // Tomato likes pH 6-7: Alimodian clay loam (pH 6.0) must not score below Sara silt loam (pH 4.5).
        assertTrue(best(10.82, 122.43, "Tomato") >= best(11.448, 123.124, "Tomato"))
    }
}
