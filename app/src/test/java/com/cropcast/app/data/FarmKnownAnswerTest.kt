package com.cropcast.app.data

import com.cropcast.app.data.model.SensorReading
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Generates random farms whose correct answer is known in advance, then checks
 * the engine agrees. Seeded, so every run tests the same thousands of farms.
 */
class FarmKnownAnswerTest {
    private val random = Random(20260927)
    private val crops = FarmSuitabilityEngine.ECOCROP.keys.toList()

    private fun uniform(low: Double, high: Double) = low + random.nextDouble() * (high - low)

    /** Clay and sand percentages that fall in one ECOCROP texture group. */
    private fun textureFor(group: Char): Pair<Double, Double> = when (group) {
        'L' -> uniform(5.0, 15.0) to uniform(70.0, 85.0)
        'H' -> uniform(40.0, 60.0) to uniform(10.0, 30.0)
        else -> uniform(20.0, 30.0) to uniform(30.0, 50.0)
    }

    private fun probe(ph: Double) = SensorReading(
        temperature = 25.0, humidity = 80.0, soilMoisture = 50.0, soilPh = ph,
        nitrogen = 50.0, phosphorus = 30.0, potassium = 60.0, timestamp = 1L
    )

    /** A farm where every condition sits inside `crop`'s ECOCROP optimum all year. */
    private fun idealFarm(crop: String): Pair<FarmSiteData, SensorReading> {
        val range = FarmSuitabilityEngine.ECOCROP.getValue(crop)
        val rain = range.monthlyRainfall
        val ph = uniform(range.ph[1], range.ph[2])
        val group = range.textures.firstOrNull { it in "LMH" } ?: "LMH".random(random)
        val (clay, sand) = textureFor(group)
        val site = FarmSiteData(
            normals = ClimateNormals(
                temperatureC = List(12) { uniform(range.temperature[1], range.temperature[2]) },
                humidityPct = List(12) { 80.0 },
                rainfallMm = List(12) { uniform(rain[1], rain[2]) }
            ),
            soil = SoilMap(ph = ph, clayPct = clay, sandPct = sand, nitrogenGPerKg = null)
        )
        return site to probe(ph)
    }

    @Test
    fun everyCropGetsFullMarksWhereItsOptimumHolds() {
        val report = StringBuilder("Known-answer farms (crop optimum holds all year):\n")
        var total = 0
        var correct = 0
        for (crop in crops) {
            var hits = 0
            repeat(200) {
                val (site, soil) = idealFarm(crop)
                val result = FarmSuitabilityEngine.recommend(soil, site, plantingMonth = random.nextInt(1, 13))
                val scored = result.ranked.first { it.crop.name == crop }
                // The crop must score 100 and be in the top tier (ties are allowed).
                if (scored.score == 100 && scored.score == result.top!!.score) hits++
            }
            report.append("  %-12s %3d/200\n".format(crop, hits))
            total += 200
            correct += hits
        }
        println(report.append("  total %d/%d".format(correct, total)))
        assertEquals(total, correct)
    }

    @Test
    fun aCropCannotGrowWhereItIsAlwaysTooHotOrTooCold() {
        for (crop in crops) {
            val range = FarmSuitabilityEngine.ECOCROP.getValue(crop)
            repeat(100) {
                val tooHot = random.nextBoolean()
                val temperature = if (tooHot) range.temperature[3] + uniform(0.1, 6.0) else range.temperature[0] - uniform(0.1, 6.0)
                val (site, soil) = idealFarm(crop)
                val hostile = site.copy(normals = site.normals!!.copy(temperatureC = List(12) { temperature }))
                val scored = FarmSuitabilityEngine.recommend(soil, hostile, plantingMonth = random.nextInt(1, 13))
                    .ranked.first { it.crop.name == crop }
                assertEquals("$crop at %.1f °C".format(temperature), 0, scored.score)
            }
        }
    }

    @Test
    fun handlesEveryCombinationOfMissingData() {
        val (full, soil) = idealFarm("Tomato")
        val outlook = WeatherOutlook(26.0, 85.0, 40.0, 16, elevationM = 300.0)
        for (mask in 0 until 16) {
            val site = FarmSiteData(
                normals = full.normals.takeIf { mask and 1 != 0 },
                outlook = outlook.takeIf { mask and 2 != 0 },
                soil = full.soil.takeIf { mask and 4 != 0 }
            )
            for (probe in listOf(soil, null)) {
                val result = FarmSuitabilityEngine.recommend(probe, site.takeIf { mask != 0 }, plantingMonth = 1 + mask % 12)
                assertTrue("mask $mask", result.ranked.all { it.score in 0..100 && it.factors.isNotEmpty() })
                assertTrue("mask $mask", result.ranked.isEmpty() || result.ranked.size == 12)
            }
        }
    }

    @Test
    fun scoreNeverDropsAsTemperatureMovesTowardTheOptimum() {
        for (crop in crops) {
            val range = FarmSuitabilityEngine.ECOCROP.getValue(crop)
            val (site, soil) = idealFarm(crop)
            fun scoreAt(temperature: Double) = FarmSuitabilityEngine.recommend(
                soil, site.copy(normals = site.normals!!.copy(temperatureC = List(12) { temperature })), plantingMonth = 3
            ).ranked.first { it.crop.name == crop }.score
            // Walk up from below the absolute minimum to the middle of the optimum.
            val middle = (range.temperature[1] + range.temperature[2]) / 2.0
            val steps = (0..40).map { range.temperature[0] - 2.0 + it * (middle - range.temperature[0] + 2.0) / 40.0 }
            steps.zipWithNext().forEach { (colder, warmer) ->
                assertTrue("$crop $colder -> $warmer", scoreAt(warmer) >= scoreAt(colder))
            }
        }
    }

    /**
     * Random Philippine-like farms checked against an independent oracle that
     * reads the raw ECOCROP ranges month by month, without the engine's code.
     */
    @Test
    fun randomFarmsAgreeWithAnIndependentOracle() {
        var farms = 0
        var withIdealCrop = 0
        var topPickIdeal = 0
        repeat(3000) {
            val base = uniform(13.0, 30.0)
            val swing = uniform(0.0, 4.0)
            val temperatures = List(12) { month -> base + swing * kotlin.math.cos((month - 6) * Math.PI / 6.0) }
            val baseRain = uniform(40.0, 500.0)
            val rains = List(12) { baseRain * uniform(0.8, 1.2) }
            val ph = uniform(4.5, 7.8)
            val (clay, sand) = textureFor("LMH".random(random))
            val site = FarmSiteData(
                normals = ClimateNormals(temperatures, List(12) { 80.0 }, rains),
                soil = SoilMap(ph, clay, sand, null)
            )
            val month = random.nextInt(1, 13)
            val result = FarmSuitabilityEngine.recommend(probe(ph), site, month)
            farms++

            val ideal = crops.filter { crop ->
                val r = FarmSuitabilityEngine.ECOCROP.getValue(crop)
                val rain = r.monthlyRainfall
                val texture = FarmSuitabilityEngine.textureClass(clay, sand)
                temperatures.all { it in r.temperature[1]..r.temperature[2] } &&
                    rains.all { it in rain[1]..rain[2] } &&
                    ph in r.ph[1]..r.ph[2] &&
                    (texture in r.textures || 'W' in r.textures)
            }
            val tooHotOrCold = crops.filter { crop ->
                val r = FarmSuitabilityEngine.ECOCROP.getValue(crop)
                temperatures.all { it >= r.temperature[3] || it <= r.temperature[0] }
            }
            val wrongPh = crops.filter { crop ->
                val r = FarmSuitabilityEngine.ECOCROP.getValue(crop)
                ph <= r.ph[0] || ph >= r.ph[3]
            }
            val scores = result.ranked.associate { it.crop.name to it.score }
            for (crop in ideal) assertEquals("ideal $crop", 100, scores.getValue(crop))
            for (crop in tooHotOrCold) assertEquals("too hot or cold $crop", 0, scores.getValue(crop))
            // pH can be corrected with lime or compost, so it costs at most 20 points, never the whole crop.
            for (crop in wrongPh) assertTrue("wrong pH $crop", scores.getValue(crop) <= 80)
            if (ideal.isNotEmpty()) {
                withIdealCrop++
                if (result.top!!.crop.name in ideal || result.top!!.score == 100) topPickIdeal++
            }
        }
        println("Random farms: $farms, with an ideal crop: $withIdealCrop, top pick ideal: $topPickIdeal")
        assertEquals(withIdealCrop, topPickIdeal)
        assertTrue("generator should produce ideal cases", withIdealCrop > 50)
    }
}
