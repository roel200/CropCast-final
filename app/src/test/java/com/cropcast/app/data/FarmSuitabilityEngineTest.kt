package com.cropcast.app.data

import com.cropcast.app.data.model.SensorReading
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmSuitabilityEngineTest {
    private val probe = SensorReading(
        temperature = 28.0, humidity = 80.0, soilMoisture = 62.5, soilPh = 6.1,
        nitrogen = 16.0, phosphorus = 24.0, potassium = 55.0, timestamp = 1L
    )

    private fun normals(temperature: Double, monthlyRain: Double) = ClimateNormals(
        temperatureC = List(12) { temperature },
        humidityPct = List(12) { 82.0 },
        rainfallMm = List(12) { monthlyRain }
    )

    private fun site(temperature: Double, clay: Double = 42.0, sand: Double = 17.0) = FarmSiteData(
        normals = normals(temperature, monthlyRain = 250.0),
        outlook = WeatherOutlook(temperature, 84.0, 60.0, 16),
        soil = SoilMap(ph = 6.4, clayPct = clay, sandPct = sand, nitrogenGPerKg = 3.8),
        status = mapOf(
            FarmSiteDataRepository.NASA_POWER to SourceStatus.LIVE,
            FarmSiteDataRepository.OPEN_METEO_FORECAST to SourceStatus.LIVE,
            FarmSiteDataRepository.SOILGRIDS to SourceStatus.CACHED
        )
    )

    @Test
    fun trapezoidFollowsEcocrop() {
        val limits = listOf(10.0, 20.0, 30.0, 40.0)
        assertEquals(0, FarmSuitabilityEngine.trapezoid(10.0, limits))
        assertEquals(50, FarmSuitabilityEngine.trapezoid(15.0, limits))
        assertEquals(100, FarmSuitabilityEngine.trapezoid(25.0, limits))
        assertEquals(25, FarmSuitabilityEngine.trapezoid(37.5, limits))
        assertEquals(0, FarmSuitabilityEngine.trapezoid(Double.NaN, limits))
    }

    @Test
    fun kotlinTableMatchesProcessedEcocropCsv() {
        val csv = listOf(
            File("../data/processed/ecocrop_selected.csv"),
            File("data/processed/ecocrop_selected.csv")
        ).first(File::isFile)
        val rows = csv.readLines().drop(1).map { line ->
            // Split on commas outside quotes; ECOCROP names such as "Okra, lady fingers" are quoted.
            line.split(Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
        }
        assertEquals(12, rows.size)
        for (row in rows) {
            val range = FarmSuitabilityEngine.ECOCROP.getValue(row[0])
            val numbers = row.subList(3, 15).map(String::toDouble)
            assertEquals(row[0], numbers.subList(0, 4), range.temperature)
            assertEquals(row[0], numbers.subList(4, 8), range.ph)
            assertEquals(row[0], numbers.subList(8, 12), range.rainfall)
            assertEquals(row[0], row[15].replace(" ", ""), range.textures)
        }
    }

    @Test
    fun hotLowlandFarmFavoursWarmCropsOverHighlandCrops() {
        val result = FarmSuitabilityEngine.recommend(probe, site(temperature = 28.0), plantingMonth = 10)

        val top = result.top!!.crop.name
        assertTrue(top !in setOf("Potato", "Cabbage", "Lettuce", "Spinach"))
        val scores = result.ranked.associate { it.crop.name to it.score }
        assertTrue(scores.getValue("Rice") > scores.getValue("Lettuce"))
        assertEquals(0, scores.getValue("Spinach"))  // 28 °C is past spinach's absolute maximum
        assertEquals(12, result.ranked.size)
    }

    @Test
    fun coolHighlandFarmFavoursCoolCrops() {
        val result = FarmSuitabilityEngine.recommend(probe, site(temperature = 17.0, clay = 20.0, sand = 40.0), plantingMonth = 1)
        val scores = result.ranked.associate { it.crop.name to it.score }

        assertTrue(scores.getValue("Cabbage") > scores.getValue("Rice"))
        assertTrue(scores.getValue("Potato") > scores.getValue("Okra"))
    }

    @Test
    fun nitrogenBelowCropRangeProducesFertilizerAdvice() {
        val result = FarmSuitabilityEngine.recommend(probe, site(temperature = 28.0), plantingMonth = 10)
        val nitrogen = result.nutrientPlan.first { it.nutrient == "Nitrogen" }

        assertEquals(NutrientStatus.LOW, nitrogen.status)
        assertTrue(nitrogen.action.contains("Urea"))
        assertEquals(3, result.nutrientPlan.size)
    }

    @Test
    fun npkNeverChangesTheRanking() {
        val low = FarmSuitabilityEngine.recommend(probe, site(28.0), plantingMonth = 10)
        val high = FarmSuitabilityEngine.recommend(
            probe.copy(nitrogen = 400.0, phosphorus = 300.0, potassium = 500.0), site(28.0), plantingMonth = 10
        )
        assertEquals(low.ranked.map { it.crop.name to it.score }, high.ranked.map { it.crop.name to it.score })
    }

    @Test
    fun worksOfflineFromTheProbeAlone() {
        val result = FarmSuitabilityEngine.recommend(probe, site = null, plantingMonth = 10)

        assertEquals(12, result.ranked.size)
        assertTrue(FarmSuitabilityEngine.SOURCE_PROBE in result.sources)
        assertTrue(FarmSuitabilityEngine.SOURCE_AIR_SENSOR in result.sources)
        assertTrue(result.warnings.any { it.contains("latitude") })
        assertTrue(result.ranked.all { factor -> factor.factors.map { it.name } == listOf("Temperature", "Soil pH") })
    }

    @Test
    fun warnsWhenProbePhDisagreesWithSoilMap() {
        val result = FarmSuitabilityEngine.recommend(probe.copy(soilPh = 4.2), site(28.0), plantingMonth = 10)
        assertTrue(result.warnings.any { it.contains("SoilGrids") })
        assertTrue(result.sources.any { it.contains("saved copy") })
    }

    @Test
    fun parsesNasaPowerClimatology() {
        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        fun block(value: Double) = months.joinToString(",", "{", ",\"ANN\":0}") { "\"$it\":$value" }
        val body = """{"properties":{"parameter":{"T2M":${block(26.0)},"RH2M":${block(85.0)},"PRECTOTCORR":${block(10.0)}}}}"""

        val normals = FarmSiteDataRepository.parseNasaPowerClimatology(body)
        assertEquals(26.0, normals.temperatureC[0], 0.001)
        assertEquals(310.0, normals.rainfallMm[0], 0.001)  // 10 mm/day x 31 days
        assertEquals(3652.5, normals.annualRainfallMm, 0.1)
    }

    @Test
    fun parsesOpenMeteoForecastSkippingNulls() {
        val body = """{"daily":{"temperature_2m_mean":[27.0,null,25.0],"relative_humidity_2m_mean":[80,90,null],"precipitation_sum":[1.5,0,10]}}"""
        val outlook = FarmSiteDataRepository.parseOpenMeteoForecast(body)
        assertEquals(26.0, outlook.meanTemperatureC, 0.001)
        assertEquals(85.0, outlook.meanHumidityPct, 0.001)
        assertEquals(11.5, outlook.rainfallMm, 0.001)
    }

    @Test
    fun parsesSoilGridsAsThicknessWeightedTopsoil() {
        fun layer(name: String, factor: Int, values: List<Int?>) = """
            {"name":"$name","unit_measure":{"d_factor":$factor},"depths":[
              {"range":{"top_depth":0,"bottom_depth":5},"values":{"mean":${values[0]}}},
              {"range":{"top_depth":5,"bottom_depth":15},"values":{"mean":${values[1]}}},
              {"range":{"top_depth":15,"bottom_depth":30},"values":{"mean":${values[2]}}}]}
        """
        val body = """{"properties":{"layers":[
            ${layer("phh2o", 10, listOf(60, 60, 66))},${layer("clay", 10, listOf(400, 400, 400))},
            ${layer("sand", 10, listOf(null, null, null))},${layer("nitrogen", 100, listOf(385, 385, 385))}]}}"""

        val soil = FarmSiteDataRepository.parseSoilGrids(body)
        assertEquals(6.3, soil.ph!!, 0.001)  // (6.0*5 + 6.0*10 + 6.6*15) / 30
        assertEquals(40.0, soil.clayPct!!, 0.001)
        assertNull(soil.sandPct)
        assertEquals(3.85, soil.nitrogenGPerKg!!, 0.001)
    }

    @Test
    fun fallsBackToTheSavedCopyWhenOffline() {
        val cacheDir = Files.createTempDirectory("cropcast-site").toFile()
        val repository = FarmSiteDataRepository(cacheDir)
        val saved = """{"daily":{"temperature_2m_mean":[27],"relative_humidity_2m_mean":[80],"precipitation_sum":[3]}}"""
        File(cacheDir, "site_OpenMeteodayforecast_14.155_121.260.json").writeText(saved)

        val (status, outlook) = repository.fetch(
            FarmSiteDataRepository.OPEN_METEO_FORECAST, "14.155,121.260", FarmSiteDataRepository::parseOpenMeteoForecast
        ) { "http://127.0.0.1:9/unreachable" }

        assertEquals(SourceStatus.CACHED, status)
        assertNotNull(outlook)
        val (missingStatus, _) = repository.fetch(
            FarmSiteDataRepository.SOILGRIDS, "0.000,0.000", FarmSiteDataRepository::parseSoilGrids
        ) { "http://127.0.0.1:9/unreachable" }
        assertEquals(SourceStatus.UNAVAILABLE, missingStatus)
    }
}
