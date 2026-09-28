package com.cropcast.app.data

import com.cropcast.app.data.model.SensorReading
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Seasonal outlook, heavy rain at planting, gale warnings and typhoon timing. */
class FarmForecastTest {
    private val probe = SensorReading(
        temperature = 27.0, humidity = 80.0, soilMoisture = 50.0, soilPh = 6.2,
        nitrogen = 40.0, phosphorus = 20.0, potassium = 100.0, timestamp = 1L
    )
    private val now = YearMonth.now()

    /** Six forecast months from now, each with the same rain ratio against ECMWF's average. */
    private fun seasonal(ratio: Double, temperatureAnomaly: Double = 0.0) = (0 until 6).map {
        val month = now.plusMonths(it.toLong())
        SeasonalMonth(month.year, month.monthValue, rainfallMm = 200.0 * ratio, rainfallAnomalyMm = 200.0 * ratio - 200.0,
            temperatureAnomalyC = temperatureAnomaly)
    }

    private fun site(monthlyRain: Double, seasonal: List<SeasonalMonth>? = null, outlook: WeatherOutlook? = null) = FarmSiteData(
        normals = ClimateNormals(List(12) { 26.0 }, List(12) { 80.0 }, List(12) { monthlyRain }),
        seasonal = seasonal,
        outlook = outlook
    )

    private fun score(result: FarmRecommendation, crop: String) = result.ranked.first { it.crop.name == crop }.score

    /** 16 days from today with rain and gust values given for chosen days. */
    private fun outlook(rain: Map<Int, Double> = emptyMap(), gust: Map<Int, Double> = emptyMap()): WeatherOutlook {
        val dates = (0 until 16).map { LocalDate.now().plusDays(it.toLong()).toString() }
        val daily = (0 until 16).map { rain[it] ?: 2.0 }
        return WeatherOutlook(
            meanTemperatureC = 26.0, meanHumidityPct = 80.0, rainfallMm = daily.sum(), days = 16,
            dailyDates = dates, dailyRainMm = daily, dailyGustKmh = (0 until 16).map { gust[it] ?: 20.0 }
        )
    }

    @Test
    fun parsesTheOpenMeteoSeasonalForecast() {
        val body = """{"elevation":1315.0,"monthly":{"time":["2026-10-01","2026-11-01","2026-12-01"],
            "precipitation_mean":[177.0,80.7,null],"precipitation_anomaly":[-76.8,-65.9,-77.9],
            "temperature_2m_anomaly":[1.0,1.2,1.5]}}"""
        val forecast = FarmSiteDataRepository.parseSeasonalForecast(body)
        assertEquals(2, forecast.months.size)  // December has no rain value
        assertEquals(SeasonalMonth(2026, 11, 80.7, -65.9, 1.2), forecast.months[1])
        assertEquals(1315.0, forecast.elevationM!!, 1e-9)
    }

    @Test
    fun elevationWithoutTheForecastStillCorrectsHighlandTemperature() {
        // La Trinidad: NASA's grid cell averages ~700 m, the farm is at 1,315 m.
        val site = FarmSiteData(
            normals = ClimateNormals(List(12) { 23.0 }, List(12) { 85.0 }, List(12) { 200.0 }, cellElevationM = 715.0),
            elevationM = 1315.0
        )
        assertEquals(23.0 - 0.0065 * 600.0, FarmSuitabilityEngine.siteClimate(site)!!.temperatureC[0], 1e-9)
    }

    @Test
    fun parsesDailyRainAndGustsFromTheForecast() {
        val body = """{"elevation":12,"daily":{"time":["2026-10-01","2026-10-02","2026-10-03"],
            "temperature_2m_mean":[27,27,26],"relative_humidity_2m_mean":[80,85,90],
            "precipitation_sum":[1.0,null,120.5],"wind_gusts_10m_max":[30,40,75]}}"""
        val outlook = FarmSiteDataRepository.parseOpenMeteoForecast(body)
        assertEquals(listOf("2026-10-01", "2026-10-03"), outlook.dailyDates)  // the null-rain day is dropped
        assertEquals(listOf(1.0, 120.5), outlook.dailyRainMm)
        assertEquals(listOf(30.0, 75.0), outlook.dailyGustKmh)
    }

    @Test
    fun seasonalRainRatioCancelsModelBiasAndIgnoresDryMonths() {
        // ECMWF: 80.7 mm forecast, 65.9 mm below its own 146.6 mm average -> 55 % of normal.
        assertEquals(0.55, FarmSuitabilityEngine.seasonalRainRatio(SeasonalMonth(2026, 11, 80.7, -65.9, 0.0)), 0.01)
        assertEquals(0.25, FarmSuitabilityEngine.seasonalRainRatio(SeasonalMonth(2027, 1, 5.0, -80.0, 0.0)), 1e-9)  // clamped
        assertEquals(1.0, FarmSuitabilityEngine.seasonalRainRatio(SeasonalMonth(2027, 3, 10.0, -5.0, 0.0)), 1e-9)   // 15 mm average
    }

    @Test
    fun seasonalForecastReplacesNormalsForTheMonthsItCovers() {
        val climate = FarmSuitabilityEngine.siteClimate(site(200.0, seasonal(ratio = 1.5, temperatureAnomaly = 1.0)))!!
        val covered = (0 until 6).map { now.plusMonths(it.toLong()).monthValue }
        for (month in 1..12) {
            if (month in covered) {
                assertEquals(300.0, climate.rainfallMm[month - 1], 1e-9)
                assertEquals(27.0, climate.temperatureC[month - 1], 1e-9)
            } else {
                assertEquals(200.0, climate.rainfallMm[month - 1], 1e-9)
            }
        }
        assertEquals(covered.toSet(), climate.forecastMonths)
    }

    @Test
    fun staleSeasonalForecastIsIgnored() {
        val old = seasonal(ratio = 3.0).map { it.copy(year = it.year - 2) }
        val climate = FarmSuitabilityEngine.siteClimate(site(200.0, old))!!
        assertTrue(climate.rainfallMm.all { it == 200.0 })
        assertTrue(climate.forecastMonths.isEmpty())
    }

    @Test
    fun aWetterSeasonFavoursWaterLovingCropsOverDryLandCrops() {
        // 300 mm/month is a normal year; the forecast says 2.5x that.
        val normal = FarmSuitabilityEngine.recommend(probe, site(300.0), now.monthValue)
        val wet = FarmSuitabilityEngine.recommend(probe, site(300.0, seasonal(ratio = 2.5)), now.monthValue)
        // Potato needs little water (best 107-218 mm/month) and loses; kangkong needs a lot (best 923+) and gains.
        assertTrue("potato ${score(normal, "Potato")} -> ${score(wet, "Potato")}", score(wet, "Potato") < score(normal, "Potato"))
        assertTrue("kangkong ${score(normal, "Kangkong")} -> ${score(wet, "Kangkong")}", score(wet, "Kangkong") > score(normal, "Kangkong"))
        assertTrue(wet.warnings.toString(), wet.warnings.any { it.contains("wetter than normal") })
        val rain = wet.ranked.first { it.crop.name == "Potato" }.factors.first { it.name == "Season rainfall" }
        assertTrue(rain.source, rain.source.contains(FarmSiteDataRepository.ECMWF_SEASONAL))
    }

    @Test
    fun aDrierSeasonHurtsRainFedRiceButNotIrrigatedRice() {
        val dry = site(250.0, seasonal(ratio = 0.3))
        val normal = FarmSuitabilityEngine.recommend(probe, site(250.0), now.monthValue)
        val rainfed = FarmSuitabilityEngine.recommend(probe, dry, now.monthValue)
        val irrigated = FarmSuitabilityEngine.recommend(probe, dry, now.monthValue, irrigated = true)
        assertTrue(score(rainfed, "Rice") < score(normal, "Rice"))
        assertTrue(score(irrigated, "Rice") > score(rainfed, "Rice"))
        assertTrue(rainfed.warnings.any { it.contains("drier than normal") })
    }

    @Test
    fun heavyRainAtPlantingPenalizesCropsThatNeedDrainageOnly() {
        val calm = FarmSuitabilityEngine.recommend(probe, site(200.0, outlook = outlook()), now.monthValue)
        val storm = FarmSuitabilityEngine.recommend(probe, site(200.0, outlook = outlook(rain = mapOf(3 to 130.0, 4 to 60.0))), now.monthValue)

        // 130 mm is PAGASA "heavy to intense": 30 % loss for drainage-needing crops.
        val tomato = storm.ranked.first { it.crop.name == "Tomato" }
        val factor = tomato.factors.first { it.name == "Heavy rain at planting" }
        assertEquals(70, factor.score)
        val plantFrom = LocalDate.now().plusDays(5)  // day after the last heavy-rain day
        assertTrue(factor.detail, factor.detail.contains("plant from"))
        assertTrue(storm.warnings.any { it.contains("Heavy to intense rain forecast") })
        assertTrue(storm.warnings.any { it.contains(plantFrom.dayOfMonth.toString()) })
        assertTrue(score(storm, "Tomato") < score(calm, "Tomato"))

        // Rice, kangkong and sitaw tolerate waterlogging (ECOCROP drainage "poorly drained").
        for (crop in FarmSuitabilityEngine.FLOOD_TOLERANT) assertEquals(crop, score(calm, crop), score(storm, crop))
    }

    @Test
    fun moderateRainCostsLessThanIntenseRain() {
        val moderate = FarmSuitabilityEngine.recommend(probe, site(200.0, outlook = outlook(rain = mapOf(2 to 70.0))), now.monthValue)
        val factor = moderate.ranked.first { it.crop.name == "Eggplant" }.factors.first { it.name == "Heavy rain at planting" }
        assertEquals(85, factor.score)  // PAGASA "moderate to heavy" (50-100 mm)
        assertTrue(moderate.warnings.any { it.contains("Moderate to heavy rain forecast") })
    }

    @Test
    fun forecastOnlyAffectsPlantingNowNotOtherMonths() {
        val calm = FarmSuitabilityEngine.recommend(probe, site(200.0, outlook = outlook()), now.monthValue)
        val storm = FarmSuitabilityEngine.recommend(probe, site(200.0, outlook = outlook(rain = mapOf(1 to 150.0))), now.monthValue)
        val calmTomato = calm.ranked.first { it.crop.name == "Tomato" }.monthlyScores
        val stormTomato = storm.ranked.first { it.crop.name == "Tomato" }.monthlyScores
        for (month in 1..12) if (month != now.monthValue) assertEquals(calmTomato[month - 1], stormTomato[month - 1])
        assertTrue(stormTomato[now.monthValue - 1] < calmTomato[now.monthValue - 1])
    }

    @Test
    fun noHeavyRainMeansNoPlantingRainFactor() {
        val result = FarmSuitabilityEngine.recommend(probe, site(200.0, outlook = outlook(rain = mapOf(5 to 49.0))), now.monthValue)
        assertTrue(result.ranked.all { crop -> crop.factors.none { it.name == "Heavy rain at planting" } })
        assertTrue(result.warnings.none { it.contains("rain forecast:") })
    }

    @Test
    fun warnsOfGaleForceGustsAtPagasaSignalTwo() {
        val gale = FarmSuitabilityEngine.recommend(probe, site(200.0, outlook = outlook(gust = mapOf(6 to 75.0))), now.monthValue)
        assertTrue(gale.warnings.any { it.contains("Gale-force gusts up to 75 km/h") })
        val breezy = FarmSuitabilityEngine.recommend(probe, site(200.0, outlook = outlook(gust = mapOf(6 to 55.0))), now.monthValue)
        assertTrue(breezy.warnings.none { it.contains("Gale-force") })
    }

    @Test
    fun typhoonRiskFollowsFloweringAndRipeningMonths() {
        fun risks(crop: String, month: Int) = FarmSuitabilityEngine.recommend(probe, site(200.0), month, latitude = 15.5)
            .ranked.first { it.crop.name == crop }.risks
        // Corn planted in March flowers and ripens in May-June, before typhoon season.
        assertTrue(risks("Corn", 3).none { it.contains("Typhoon") })
        // Corn planted in June flowers and ripens in August-September.
        assertTrue(risks("Corn", 6).any { it.contains("Typhoon") })
    }

    @Test
    fun floodTolerantCropsMatchEcocropDrainageData() {
        val csv = listOf(File("../data/processed/ecocrop_selected.csv"), File("data/processed/ecocrop_selected.csv")).first(File::isFile)
        val header = csv.readLines().first().split(",")
        val drar = header.indexOf("DRAR")
        assertTrue("DRAR column", drar >= 0)
        val tolerant = csv.readLines().drop(1)
            .map { it.split(Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) }
            .filter { 'I' in it[drar] }
            .map { it[0] }
            .toSet()
        assertEquals(FarmSuitabilityEngine.FLOOD_TOLERANT, tolerant)
    }

    @Test
    fun noOutlookMeansNoHeavyRainFactor() {
        val result = FarmSuitabilityEngine.recommend(probe, site(200.0), now.monthValue)
        assertNull(result.ranked.first().factors.firstOrNull { it.name == "Heavy rain at planting" })
    }
}
