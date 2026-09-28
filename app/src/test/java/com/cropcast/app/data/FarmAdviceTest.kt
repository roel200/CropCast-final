package com.cropcast.app.data

import com.cropcast.app.data.model.SensorReading
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** FAO classes, salinity, fertilizer rates, season risks and PSA economics. */
class FarmAdviceTest {
    // The reading the user's probe showed: N 16, P 24, K 55 mg/kg, pH 6.1, EC 344 µS/cm.
    private val probe = SensorReading(
        temperature = 27.0, humidity = 80.0, soilMoisture = 62.5, soilPh = 6.1,
        nitrogen = 16.0, phosphorus = 24.0, potassium = 55.0, timestamp = 1L, electricalConductivity = 344.0
    )

    private fun site(temperature: Double, monthlyRain: Double = 200.0) = FarmSiteData(
        normals = ClimateNormals(List(12) { temperature }, List(12) { 80.0 }, List(12) { monthlyRain })
    )

    private fun crop(name: String) = FarmSuitabilityEngine.CROPS.first { it.name == name }

    @Test
    fun mapsScoresToFaoSuitabilityClasses() {
        assertEquals(SuitabilityClass.S1, SuitabilityClass.of(80))
        assertEquals(SuitabilityClass.S2, SuitabilityClass.of(79))
        assertEquals(SuitabilityClass.S3, SuitabilityClass.of(40))
        assertEquals(SuitabilityClass.N, SuitabilityClass.of(39))
    }

    @Test
    fun catalogCoversSeventeenCropsWithEcocropData() {
        assertEquals(17, FarmSuitabilityEngine.CROPS.size)
        FarmSuitabilityEngine.CROPS.forEach { assertNotNull(it.name, FarmSuitabilityEngine.ECOCROP[it.name]) }
        assertTrue(FarmSuitabilityEngine.CROPS.map { it.name }.containsAll(listOf("Pechay", "Kangkong", "Ampalaya", "Sitaw", "Kalabasa")))
    }

    @Test
    fun salinityFollowsFaoRelativeYield() {
        // 8,000 µS/cm bulk EC -> ECe 8 dS/m. Tomato: 100 - 9.9 x (8 - 2.5) = 45.55 %.
        val saline = probe.copy(electricalConductivity = 8000.0)
        val result = FarmSuitabilityEngine.recommend(saline, site(24.0), plantingMonth = 1)
        val tomato = result.ranked.first { it.crop.name == "Tomato" }
        assertEquals(46, tomato.factors.first { it.name == "Soil salinity" }.score)
        assertTrue(result.warnings.any { it.contains("saline") })
        // Sitaw (cowpea, threshold 4.9) tolerates salt better than tomato.
        val sitaw = result.ranked.first { it.crop.name == "Sitaw" }
        assertTrue(sitaw.factors.first { it.name == "Soil salinity" }.score > 46)
        // FAO has no salt data for kangkong, so it gets no salinity factor.
        assertTrue(result.ranked.first { it.crop.name == "Kangkong" }.factors.none { it.name == "Soil salinity" })
    }

    @Test
    fun normalSoilEcCostsNothing() {
        val result = FarmSuitabilityEngine.recommend(probe, site(24.0), plantingMonth = 1)
        result.ranked.forEach { crop ->
            crop.factors.firstOrNull { it.name == "Soil salinity" }?.let { assertEquals(crop.crop.name, 100, it.score) }
        }
        assertTrue(result.warnings.none { it.contains("saline") })
    }

    @Test
    fun fertilizerRatesFollowSoilTestLevels() {
        // Tomato needs N 80-150, P2O5 50-70, K2O 100-150 kg/ha.
        val plan = FarmSuitabilityEngine.nutrientPlan(crop("Tomato"), probe).associateBy { it.nutrient }
        val nitrogen = plan.getValue("Nitrogen")
        assertEquals(NutrientStatus.LOW, nitrogen.status)                 // 16 mg/kg < 125
        assertEquals(150.0, nitrogen.applyKgPerHa!!, 1e-9)                // low soil -> top of the need
        assertTrue(nitrogen.action.contains("326 kg Urea"))               // 150 / 0.46
        val phosphorus = plan.getValue("Phosphorus")
        assertEquals(NutrientStatus.OK, phosphorus.status)                // 24 mg/kg is medium (10-25)
        assertEquals(60.0, phosphorus.applyKgPerHa!!, 1e-9)               // middle of 50-70
        val potassium = plan.getValue("Potassium")
        assertEquals(NutrientStatus.LOW, potassium.status)                // 55 mg/kg < 78
        assertTrue(potassium.action.contains("250 kg Muriate of potash")) // 150 / 0.60
        assertTrue(potassium.action.contains("5.0 bags/ha"))
    }

    @Test
    fun highSoilNutrientGetsOnlyAMaintenanceDose() {
        val rich = probe.copy(nitrogen = 300.0)
        val nitrogen = FarmSuitabilityEngine.nutrientPlan(crop("Tomato"), rich).first { it.nutrient == "Nitrogen" }
        assertEquals(NutrientStatus.HIGH, nitrogen.status)
        assertEquals(40.0, nitrogen.applyKgPerHa!!, 1e-9)  // half of the 80 kg/ha minimum
    }

    @Test
    fun cropsWithoutVerifiedRatesSayWhereToLook() {
        val plan = FarmSuitabilityEngine.nutrientPlan(crop("Kangkong"), probe)
        assertEquals(listOf("Soil pH", "Fertilizer"), plan.map { it.nutrient })
        assertNull(plan.last().applyKgPerHa)
        assertTrue(plan.last().action.contains("DA production guide"))
    }

    @Test
    fun flagsTyphoonSeasonForTallCropsInLuzonOnly() {
        fun risks(latitude: Double, crop: String, month: Int) =
            FarmSuitabilityEngine.recommend(probe, site(27.0), plantingMonth = month, latitude = latitude)
                .ranked.first { it.crop.name == crop }.risks
        assertTrue(risks(15.5, "Corn", 8).any { it.contains("Typhoon") })        // Nueva Ecija, August
        assertTrue(risks(15.5, "Corn", 1).none { it.contains("Typhoon") })       // dry season
        assertTrue(risks(15.5, "Sweet Potato", 8).none { it.contains("Typhoon") }) // low-growing
        assertTrue(risks(7.1, "Corn", 8).none { it.contains("Typhoon") })        // Davao, south of the belt
    }

    @Test
    fun flagsWetSeasonDiseaseByCropFamily() {
        val result = FarmSuitabilityEngine.recommend(probe, site(27.0, monthlyRain = 350.0), plantingMonth = 7)
        fun risks(crop: String) = result.ranked.first { it.crop.name == crop }.risks
        assertTrue(risks("Tomato").any { it.contains("bacterial wilt") })
        assertTrue(risks("Cucumber").any { it.contains("downy mildew") })
        assertTrue(risks("Pechay").any { it.contains("black rot") })
        assertTrue(risks("Rice").none { it.contains("Wet, warm") })
    }

    @Test
    fun flagsPlantingTheSameFamilyTwice() {
        val result = FarmSuitabilityEngine.recommend(probe, site(24.0), plantingMonth = 1, previousCrop = "eggplant")
        assertTrue(result.ranked.first { it.crop.name == "Tomato" }.risks.any { it.contains("Follows Eggplant") })
        assertTrue(result.ranked.first { it.crop.name == "Corn" }.risks.none { it.contains("Follows") })
        // Rice after rice is normal paddy practice, so no rotation warning.
        val paddy = FarmSuitabilityEngine.recommend(probe, site(27.0), plantingMonth = 6, previousCrop = "Rice")
        assertTrue(paddy.ranked.first { it.crop.name == "Rice" }.risks.none { it.contains("Follows") })
    }

    @Test
    fun averagesProbeSpotsIntoOneFieldSample() {
        val spots = listOf(5.8, 6.2, 6.0, 6.6, 5.9).mapIndexed { i, ph ->
            probe.copy(soilPh = ph, nitrogen = 10.0 + i * 5, timestamp = 1_000L + i)
        }
        val sample = MonthlySensorAggregator.fieldSample(spots, takenAt = 9_999L)!!
        assertEquals(5, sample.spots)
        assertEquals(6.1, sample.average.soilPh, 1e-9)
        assertEquals(20.0, sample.average.nitrogen, 1e-9)
        assertEquals(0.8, sample.phSpread, 1e-9)
        assertEquals(9_999L, sample.takenAt)
        assertNull(MonthlySensorAggregator.fieldSample(emptyList(), takenAt = 1L))
    }

    @Test
    fun readsBundledPsaEconomicsAndPicksTheBestValueCrop() {
        val asset = listOf(File("src/main/assets/crop_economics.json"), File("app/src/main/assets/crop_economics.json"))
            .first(File::isFile)
        val economics = FarmSuitabilityEngine.parseEconomics(asset.readText())
        assertEquals(17, economics.size)
        val rice = economics.getValue("Rice")
        assertTrue(rice.yieldTonnesPerHa!! in 3.0..6.0)       // national palay yield is about 4 t/ha
        assertTrue(rice.farmgatePricePhpPerKg!! in 12.0..35.0)
        assertNull(economics.getValue("Spinach").grossPhpPerHa) // PSA has no spinach farmgate price

        val result = FarmSuitabilityEngine.recommend(probe, site(24.0), plantingMonth = 1, economics = economics)
        val best = result.bestValue!!
        assertTrue(best.score >= 60)
        assertTrue(result.ranked.filter { it.score >= 60 }.all {
            (it.economics?.grossPhpPerHa ?: 0.0) <= best.economics!!.grossPhpPerHa!!
        })
        assertTrue(FarmSuitabilityEngine.SOURCE_PSA in result.sources)
    }
}
