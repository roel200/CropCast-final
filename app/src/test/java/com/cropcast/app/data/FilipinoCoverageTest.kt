package com.cropcast.app.data

import com.cropcast.app.data.model.SensorReading
import com.cropcast.app.ui.localization.translateFilipino
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Every sentence the engines can show must have a Filipino text, so the farmer never sees English mixed in. */
class FilipinoCoverageTest {
    private val today = LocalDate.now()
    private val days = (0 until 16).map { today.plusDays(it.toLong()).toString() }
    private val season = (0 until 6).map {
        val month = YearMonth.now().plusMonths(it.toLong())
        // Alternating dry and wet months with a warm anomaly hit every seasonal warning.
        SeasonalMonth(month.year, month.monthValue, if (it % 2 == 0) 40.0 else 600.0, if (it % 2 == 0) -60.0 else 250.0, 1.4)
    }
    private val outlook = WeatherOutlook(
        meanTemperatureC = 27.0, meanHumidityPct = 90.0, rainfallMm = 300.0, days = 16, elevationM = 450.0,
        dailyDates = days,
        dailyRainMm = days.indices.map { if (it == 3) 130.0 else 2.0 },
        dailyGustKmh = days.indices.map { if (it == 5) 75.0 else 20.0 }
    )

    private fun scenario(
        probe: SensorReading?,
        rainMonth: Double,
        month: Int,
        site: FarmSiteData? = null,
        growing: String? = null,
        previous: String? = null,
        latitude: Double? = 10.7
    ): FarmRecommendation {
        val data = site ?: FarmSiteData(
            normals = ClimateNormals(List(12) { 28.0 }, List(12) { 80.0 }, List(12) { rainMonth }, cellElevationM = 100.0),
            outlook = outlook,
            soil = SoilMap(ph = 5.0, clayPct = 40.0, sandPct = 20.0, nitrogenGPerKg = null),
            seasonal = season,
            status = mapOf(
                FarmSiteDataRepository.NASA_POWER to SourceStatus.LIVE,
                FarmSiteDataRepository.SOILGRIDS to SourceStatus.CACHED
            )
        )
        return FarmSuitabilityEngine.recommend(
            probe, data, plantingMonth = month, recentRainMm = 5.0, latitude = latitude,
            previousCrop = previous, growingCrop = growing
        )
    }

    private fun texts(recommendation: FarmRecommendation): List<String> = buildList {
        addAll(recommendation.warnings)
        addAll(recommendation.sources)
        recommendation.ranked.forEach { crop ->
            add(crop.crop.name); add(crop.crop.variety); add(crop.crop.soil)
            addAll(crop.risks)
            crop.factors.forEach { add(it.name); add(it.detail); add(it.source) }
        }
        recommendation.nutrientPlan.forEach { add(it.nutrient); add(it.detail); add(it.action) }
    }

    @Test
    fun noEngineSentenceStaysInEnglish() {
        val probes = listOf(
            SensorReading(27.0, 80.0, 25.0, 4.6, 16.0, 5.0, 30.0, timestamp = 1L, electricalConductivity = 6000.0),
            SensorReading(27.0, 80.0, 60.0, 8.2, 300.0, 40.0, 200.0, timestamp = 1L, electricalConductivity = 300.0),
            SensorReading(27.0, 80.0, 60.0, 6.2, 150.0, 15.0, 100.0, timestamp = 1L, electricalConductivity = 300.0)
        )
        val untranslated = linkedSetOf<String>()
        for (probe in probes) for (month in listOf(1, 7, 10)) for (growing in listOf(null, "Tomato", "Chili", "Rice")) {
            for (rain in listOf(40.0, 400.0)) {
                val recommendation = scenario(probe, rain, month, growing = growing, previous = growing)
                texts(recommendation).filter { translateFilipino(it) == it }.forEach(untranslated::add)
            }
        }
        scenario(null, 100.0, 1, site = FarmSiteData(), latitude = null).warnings
            .filter { translateFilipino(it) == it }.forEach(untranslated::add)
        // Proper names that read the same in Filipino (crop varieties, "Okra") are fine.
        val allowed = setOf("Okra", "Alugbati", "Spinach", "Pechay", "Kangkong", "Ampalaya", "Sitaw", "Kalabasa", "Gabi", "Patola",
            "Native", "Hybrid", "Pole", "Roma", "Ilocos", "Upland", "Lowland", "Siling labuyo", "Temperature", "Nitrogen", "Phosphorus", "Potassium")
        val missing = untranslated - allowed
        assertTrue("Not translated:\n" + missing.joinToString("\n"), missing.isEmpty())
    }
}
