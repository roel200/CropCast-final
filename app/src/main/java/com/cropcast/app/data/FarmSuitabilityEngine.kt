package com.cropcast.app.data

import com.cropcast.app.data.model.SeedRecommendation
import com.cropcast.app.data.model.SensorReading
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** One scored input for one crop. `score` is 0-100. */
data class FactorScore(
    val name: String,
    val score: Int,
    val detail: String,
    val source: String
)

data class CropSuitability(
    val crop: SeedRecommendation,
    val score: Int,
    val factors: List<FactorScore>
) {
    val limitingFactor: FactorScore? get() = factors.minByOrNull { it.score }
}

enum class NutrientStatus { LOW, OK, HIGH }

data class NutrientAdvice(
    val nutrient: String,
    val measured: Double,
    val targetLow: Double,
    val targetHigh: Double,
    val status: NutrientStatus,
    val action: String
)

data class FarmRecommendation(
    val ranked: List<CropSuitability>,
    val nutrientPlan: List<NutrientAdvice>,
    val warnings: List<String>,
    val sources: List<String>
) {
    val top: CropSuitability? get() = ranked.firstOrNull()
}

/**
 * Ranks CropCast's 12 crops for a farm by combining the soil probe with free
 * location data, using the FAO ECOCROP method: each factor is scored on a
 * trapezoid (0 outside the absolute range, 100 inside the optimal range).
 *
 * Temperature over the growing season and soil pH are hard factors: the farmer
 * cannot easily change them, so the weakest one caps the score (0 means the crop
 * cannot grow). Growing-season rain and soil texture are soft factors that can
 * lower the score by up to 30 %, because irrigation and drainage can manage them. N/P/K never rank crops; fertilizer can fix them, so they drive
 * the nutrient plan instead.
 */
object FarmSuitabilityEngine {

    /** FAO ECOCROP ranges; source rows in data/processed/ecocrop_selected.csv. */
    internal data class EcocropRange(
        val temperature: List<Double>,  // absolute min, optimal min, optimal max, absolute max (°C)
        val ph: List<Double>,
        val rainfall: List<Double>,     // mm over the crop cycle
        val textures: String            // L light, M medium, H heavy, O organic, W wide
    )

    internal val ECOCROP = mapOf(
        "Tomato" to EcocropRange(listOf(7.0, 20.0, 27.0, 35.0), listOf(5.0, 5.5, 6.8, 7.5), listOf(400.0, 600.0, 1300.0, 1800.0), "MO"),
        "Okra" to EcocropRange(listOf(12.0, 20.0, 30.0, 35.0), listOf(4.5, 5.5, 7.0, 8.7), listOf(300.0, 600.0, 1200.0, 2500.0), "WO"),
        "Alugbati" to EcocropRange(listOf(10.0, 23.0, 27.0, 35.0), listOf(4.3, 5.5, 7.0, 7.5), listOf(700.0, 2000.0, 2500.0, 4200.0), "MO"),
        "Potato" to EcocropRange(listOf(7.0, 15.0, 25.0, 30.0), listOf(4.2, 5.0, 6.2, 8.5), listOf(250.0, 500.0, 800.0, 2000.0), "MO"),
        "Rice" to EcocropRange(listOf(16.0, 25.0, 35.0, 38.0), listOf(3.5, 5.0, 8.0, 9.0), listOf(1000.0, 1500.0, 2000.0, 4000.0), "W"),
        "Corn" to EcocropRange(listOf(10.0, 18.0, 33.0, 47.0), listOf(4.5, 5.0, 7.0, 8.5), listOf(400.0, 600.0, 1200.0, 1800.0), "MO"),
        "Eggplant" to EcocropRange(listOf(9.0, 20.0, 35.0, 40.0), listOf(4.3, 5.5, 6.8, 8.5), listOf(800.0, 1200.0, 1600.0, 4000.0), "MO"),
        "Cucumber" to EcocropRange(listOf(6.0, 18.0, 32.0, 38.0), listOf(4.5, 6.0, 7.5, 8.7), listOf(400.0, 1000.0, 1200.0, 4300.0), "MO"),
        "Cabbage" to EcocropRange(listOf(7.0, 15.0, 24.0, 32.0), listOf(5.0, 6.0, 7.5, 8.3), listOf(300.0, 500.0, 1000.0, 2500.0), "MO"),
        "Sweet Potato" to EcocropRange(listOf(10.0, 18.0, 28.0, 38.0), listOf(4.0, 5.0, 7.0, 8.0), listOf(500.0, 750.0, 1250.0, 5000.0), "M"),
        "Lettuce" to EcocropRange(listOf(5.0, 12.0, 21.0, 30.0), listOf(4.2, 6.0, 7.0, 7.5), listOf(900.0, 1100.0, 1400.0, 4100.0), "LM"),
        "Spinach" to EcocropRange(listOf(2.0, 13.0, 20.0, 27.0), listOf(5.3, 6.0, 7.5, 8.3), listOf(300.0, 800.0, 1200.0, 1700.0), "LM")
    )

    const val SOURCE_PROBE = "7-in-1 soil probe"
    const val SOURCE_AIR_SENSOR = "DHT11 air sensor"
    const val SOURCE_ECOCROP = "FAO ECOCROP crop ranges"
    const val SOURCE_RECENT_RAIN = "Open-Meteo rainfall history"

    // ponytail: table N/P/K ranges have no stated unit; set this from a lab soil
    // test (lab value / probe value) instead of trusting the probe's mg/kg as-is.
    const val NPK_TABLE_SCALE = 1.0

    fun recommend(
        soil: SensorReading?,
        site: FarmSiteData?,
        plantingMonth: Int,
        recentRainMm: Double? = null
    ): FarmRecommendation {
        val probe = soil?.takeIf { it.soilPh > 0.0 }
        val sources = linkedSetOf(SOURCE_ECOCROP)
        if (probe != null) sources += SOURCE_PROBE
        site?.status?.filterValues { it != SourceStatus.UNAVAILABLE }?.forEach { (name, status) ->
            sources += if (status == SourceStatus.CACHED) "$name (saved copy)" else name
        }
        if (recentRainMm != null) sources += SOURCE_RECENT_RAIN

        val ranked = SeedRecommendationEngine.crops.map { profile ->
            val range = ECOCROP.getValue(profile.name)
            val months = ceil(profile.days / 30.0).toInt().coerceIn(1, 12)
            val hard = listOfNotNull(
                temperatureFactor(range, months, plantingMonth, site, soil)?.also {
                    if (it.source == SOURCE_AIR_SENSOR) sources += SOURCE_AIR_SENSOR
                },
                phFactor(range, probe, site?.soil)
            )
            val soft = listOfNotNull(
                rainfallFactor(range, months, plantingMonth, site?.normals),
                textureFactor(range, site?.soil)
            )
            val all = hard + soft
            // The weakest hard factor caps the score; soft factors can remove up to 30 %.
            val cap = (hard.ifEmpty { soft }).minOfOrNull { it.score } ?: 0
            val softMean = soft.map { it.score }.average().takeIf { soft.isNotEmpty() } ?: 100.0
            val score = (cap * (0.7 + 0.3 * softMean / 100.0)).roundToInt()
            CropSuitability(
                crop = SeedRecommendation(profile.name, profile.variety, profile.icon, profile.days, profile.soil, score),
                score = score,
                factors = all
            )
        }
            .filter { it.factors.isNotEmpty() }
            .sortedByDescending { it.score }  // stable: ties keep the existing profile order

        return FarmRecommendation(
            ranked = ranked,
            nutrientPlan = ranked.firstOrNull()?.let { nutrientPlan(it.crop.name, probe) }.orEmpty(),
            warnings = warnings(probe, site, recentRainMm),
            sources = sources.toList()
        )
    }

    /** ECOCROP trapezoid: 0 outside [min, max], 100 inside [optMin, optMax], linear between. */
    internal fun trapezoid(value: Double, limits: List<Double>): Int {
        val (min, optMin, optMax, max) = limits
        val score = when {
            !value.isFinite() || value <= min || value >= max -> 0.0
            value < optMin -> (value - min) / (optMin - min) * 100.0
            value > optMax -> (max - value) / (max - optMax) * 100.0
            else -> 100.0
        }
        return score.roundToInt()
    }

    private fun seasonMonths(plantingMonth: Int, months: Int): List<Int> =
        (0 until months).map { (plantingMonth - 1 + it) % 12 }

    private fun temperatureFactor(
        range: EcocropRange,
        months: Int,
        plantingMonth: Int,
        site: FarmSiteData?,
        soil: SensorReading?
    ): FactorScore? {
        val normals = site?.normals
        val outlook = site?.outlook
        val (temperatures, source) = when {
            normals != null -> seasonMonths(plantingMonth, months).mapIndexed { index, month ->
                // The 16-day forecast replaces the normal for the planting month.
                if (index == 0 && outlook != null) outlook.meanTemperatureC else normals.temperatureC[month]
            } to FarmSiteDataRepository.NASA_POWER
            outlook != null -> listOf(outlook.meanTemperatureC) to FarmSiteDataRepository.OPEN_METEO_FORECAST
            soil != null && soil.temperature != 0.0 -> listOf(soil.temperature) to SOURCE_AIR_SENSOR
            else -> return null
        }
        val worst = temperatures.minBy { trapezoid(it, range.temperature) }
        return FactorScore(
            name = "Temperature",
            score = trapezoid(worst, range.temperature),
            detail = "Season %.1f–%.1f °C (best %.0f–%.0f °C)".format(
                temperatures.min(), temperatures.max(), range.temperature[1], range.temperature[2]
            ),
            source = source
        )
    }

    private fun phFactor(range: EcocropRange, probe: SensorReading?, map: SoilMap?): FactorScore? {
        val (ph, source) = when {
            probe != null -> probe.soilPh to SOURCE_PROBE
            map?.ph != null -> map.ph to FarmSiteDataRepository.SOILGRIDS
            else -> return null
        }
        return FactorScore(
            name = "Soil pH",
            score = trapezoid(ph, range.ph),
            detail = "pH %.1f (best %.1f–%.1f)".format(ph, range.ph[1], range.ph[2]),
            source = source
        )
    }

    private fun rainfallFactor(range: EcocropRange, months: Int, plantingMonth: Int, normals: ClimateNormals?): FactorScore? {
        normals ?: return null
        val seasonRain = seasonMonths(plantingMonth, months).sumOf { normals.rainfallMm[it] }
        val score = trapezoid(seasonRain, range.rainfall)
        val hint = when {
            seasonRain < range.rainfall[1] -> " – plan irrigation"
            seasonRain > range.rainfall[2] -> " – needs good drainage"
            else -> ""
        }
        return FactorScore(
            name = "Season rainfall",
            score = score,
            detail = "%.0f mm expected over %d month(s) (best %.0f–%.0f)$hint".format(
                seasonRain, months, range.rainfall[1], range.rainfall[2]
            ),
            source = FarmSiteDataRepository.NASA_POWER
        )
    }

    /** Simplified FAO texture groups from clay and sand percentages. */
    internal fun textureClass(clayPct: Double, sandPct: Double): Char = when {
        clayPct >= 35.0 -> 'H'
        sandPct >= 65.0 && clayPct < 18.0 -> 'L'
        else -> 'M'
    }

    private fun textureFactor(range: EcocropRange, map: SoilMap?): FactorScore? {
        val clay = map?.clayPct ?: return null
        val sand = map.sandPct ?: return null
        val texture = textureClass(clay, sand)
        val name = when (texture) { 'H' -> "heavy (clay)"; 'L' -> "light (sandy)"; else -> "medium (loam)" }
        // Every one of the 12 crops tolerates a wide texture range in ECOCROP,
        // so a non-optimal texture lowers the score instead of excluding the crop.
        val optimal = texture in range.textures || 'W' in range.textures
        return FactorScore(
            name = "Soil texture",
            score = if (optimal) 100 else 70,
            detail = "Mapped soil is $name, %.0f%% clay".format(clay) + if (optimal) "" else " – add organic matter",
            source = FarmSiteDataRepository.SOILGRIDS
        )
    }

    private fun nutrientPlan(cropName: String, probe: SensorReading?): List<NutrientAdvice> {
        probe ?: return emptyList()
        val profile = SeedRecommendationEngine.crops.first { it.name == cropName }
        return listOf(
            Triple("Nitrogen", probe.nitrogen, profile.nitrogen) to "Urea (46-0-0) or ammonium sulfate (21-0-0)",
            Triple("Phosphorus", probe.phosphorus, profile.phosphorus) to "Solophos (0-18-0) or complete (14-14-14)",
            Triple("Potassium", probe.potassium, profile.potassium) to "Muriate of potash (0-0-60)"
        ).map { (reading, fertilizer) ->
            val (nutrient, measured, target) = reading
            val low = target.start * NPK_TABLE_SCALE
            val high = target.endInclusive * NPK_TABLE_SCALE
            val status = when {
                measured < low -> NutrientStatus.LOW
                measured > high -> NutrientStatus.HIGH
                else -> NutrientStatus.OK
            }
            NutrientAdvice(
                nutrient = nutrient,
                measured = measured,
                targetLow = low,
                targetHigh = high,
                status = status,
                action = when (status) {
                    NutrientStatus.LOW -> "Apply $fertilizer"
                    NutrientStatus.HIGH -> "Skip ${nutrient.lowercase()} fertilizer this season"
                    NutrientStatus.OK -> "No extra ${nutrient.lowercase()} needed"
                }
            )
        }
    }

    private fun warnings(probe: SensorReading?, site: FarmSiteData?, recentRainMm: Double?): List<String> = buildList {
        val mappedPh = site?.soil?.ph
        if (probe != null && mappedPh != null && abs(probe.soilPh - mappedPh) > 1.0) {
            add("Probe pH %.1f differs from the SoilGrids map (%.1f) by more than 1.0. Check the probe with a pH buffer or a lab test.".format(probe.soilPh, mappedPh))
        }
        site?.outlook?.let { outlook ->
            when {
                outlook.rainfallMm > 150.0 ->
                    add("Heavy rain forecast: %.0f mm in the next %d days. Prepare drainage and delay transplanting.".format(outlook.rainfallMm, outlook.days))
                outlook.rainfallMm < 10.0 ->
                    add("Little rain forecast: %.0f mm in the next %d days. Irrigate after planting.".format(outlook.rainfallMm, outlook.days))
            }
            if (outlook.meanHumidityPct > 85.0) {
                add("High humidity ahead (%.0f%%). Watch tomato, eggplant, potato and cucumber for fungal disease.".format(outlook.meanHumidityPct))
            }
        }
        if (recentRainMm != null && recentRainMm < 20.0 && (probe?.soilMoisture ?: 100.0) < 40.0) {
            add("Only %.0f mm of rain in the last 30 days and the soil is dry. Water before planting.".format(recentRainMm))
        }
        if (site == null || site.status.values.all { it == SourceStatus.UNAVAILABLE }) {
            add("Climate and soil maps are not loaded yet. Set the farm latitude and longitude in Settings and connect to the internet once.")
        }
    }
}
