package com.cropcast.app.data

import com.cropcast.app.data.model.SeedRecommendation
import com.cropcast.app.data.model.SensorReading
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
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
    val factors: List<FactorScore>,
    /** Score if planted in each month, January first; empty without climate normals. */
    val monthlyScores: List<Int> = emptyList()
) {
    val limitingFactor: FactorScore? get() = factors.minByOrNull { it.score }

    /** Months (1-12) within 5 points of the best planting month. */
    val bestPlantingMonths: List<Int>
        get() {
            val best = monthlyScores.maxOrNull()?.takeIf { it > 0 } ?: return emptyList()
            return monthlyScores.indices.filter { monthlyScores[it] >= best - 5 }.map { it + 1 }
        }
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
 * location data. Temperature and rain follow FAO's ECOCROP model exactly as the
 * reference implementation (R package Recocrop) computes it: monthly climate is
 * interpolated to half-months, each half-month is scored on the crop's
 * trapezoid (0 outside the absolute range, 100 inside the optimal range), and
 * the worst half-month over the growing season counts. ECOCROP rain limits are
 * totals over the crop cycle, so they are converted to monthly limits first.
 *
 * Temperature is the hard factor: a farmer cannot change it, so it caps the
 * score (0 means the crop cannot grow there). Soil pH, rain and texture can be
 * managed with lime or organic matter, irrigation and drainage, so together
 * they scale the score between 60 % and 100 % (weights 0.5, 0.3, 0.2). This
 * departs from ECOCROP, which treats pH as absolute: validation against PSA
 * production and SoilsSync farms showed tomato thriving on Ilocos soils above
 * pH 7.5 and corn on limed Bukidnon soils below pH 4.5, both of which ECOCROP
 * zeroes. N/P/K never rank crops; fertilizer fixes them, so they and pH drive
 * the soil plan instead.
 */
object FarmSuitabilityEngine {

    /** FAO ECOCROP parameters; source rows in data/processed/ecocrop_selected.csv. */
    internal data class EcocropRange(
        val temperature: List<Double>,  // absolute min, optimal min, optimal max, absolute max (°C)
        val ph: List<Double>,
        val rainfall: List<Double>,     // mm over the whole crop cycle
        val textures: String,           // L light, M medium, H heavy, O organic, W wide
        val cycleDays: List<Double>     // shortest and longest growing cycle (GMIN, GMAX)
    ) {
        /** Recocrop: duration = GMIN + up to 30 days, rounded to 15 days; counted in half-months. */
        val seasonHalfMonths: Int
            get() {
                val (shortest, longest) = cycleDays
                val duration = 15.0 * ((shortest + min(30.0, longest - shortest)) / 15.0).roundToInt()
                return ceil(duration / 15.0).toInt().coerceIn(1, 24)
            }

        /** Recocrop: cycle rain totals divided by the cycle length in months (±30 days at the extremes). */
        val monthlyRainfall: List<Double>
            get() {
                val (shortest, longest) = cycleDays
                val average = (longest + shortest) / 2.0
                val (long, short) = if (longest > shortest) (average + 30.0) to (average - 30.0) else average to average
                val divisors = listOf(long, (average + long) / 2.0, (average + short) / 2.0, short).map { it / 30.0 }
                return rainfall.zip(divisors) { total, months -> (total / months).roundToInt().toDouble() }
            }
    }

    internal val ECOCROP = mapOf(
        "Tomato" to EcocropRange(listOf(7.0, 20.0, 27.0, 35.0), listOf(5.0, 5.5, 6.8, 7.5), listOf(400.0, 600.0, 1300.0, 1800.0), "MO", listOf(70.0, 150.0)),
        "Okra" to EcocropRange(listOf(12.0, 20.0, 30.0, 35.0), listOf(4.5, 5.5, 7.0, 8.7), listOf(300.0, 600.0, 1200.0, 2500.0), "WO", listOf(50.0, 180.0)),
        "Alugbati" to EcocropRange(listOf(10.0, 23.0, 27.0, 35.0), listOf(4.3, 5.5, 7.0, 7.5), listOf(700.0, 2000.0, 2500.0, 4200.0), "MO", listOf(55.0, 180.0)),
        "Potato" to EcocropRange(listOf(7.0, 15.0, 25.0, 30.0), listOf(4.2, 5.0, 6.2, 8.5), listOf(250.0, 500.0, 800.0, 2000.0), "MO", listOf(90.0, 160.0)),
        "Rice" to EcocropRange(listOf(16.0, 25.0, 35.0, 38.0), listOf(3.5, 5.0, 8.0, 9.0), listOf(1000.0, 1500.0, 2000.0, 4000.0), "W", listOf(80.0, 200.0)),
        // Flint maize, not generic "Maize": its 90-140 day cycle matches Philippine yellow and white corn.
        "Corn" to EcocropRange(listOf(10.0, 18.0, 30.0, 47.0), listOf(4.5, 5.0, 7.0, 8.5), listOf(450.0, 600.0, 1200.0, 1800.0), "MO", listOf(90.0, 140.0)),
        "Eggplant" to EcocropRange(listOf(9.0, 20.0, 35.0, 40.0), listOf(4.3, 5.5, 6.8, 8.5), listOf(800.0, 1200.0, 1600.0, 4000.0), "MO", listOf(70.0, 120.0)),
        "Cucumber" to EcocropRange(listOf(6.0, 18.0, 32.0, 38.0), listOf(4.5, 6.0, 7.5, 8.7), listOf(400.0, 1000.0, 1200.0, 4300.0), "MO", listOf(40.0, 180.0)),
        "Cabbage" to EcocropRange(listOf(7.0, 15.0, 24.0, 32.0), listOf(5.0, 6.0, 7.5, 8.3), listOf(300.0, 500.0, 1000.0, 2500.0), "MO", listOf(60.0, 200.0)),
        "Sweet Potato" to EcocropRange(listOf(10.0, 18.0, 28.0, 38.0), listOf(4.0, 5.0, 7.0, 8.0), listOf(500.0, 750.0, 1250.0, 5000.0), "M", listOf(80.0, 170.0)),
        "Lettuce" to EcocropRange(listOf(5.0, 12.0, 21.0, 30.0), listOf(4.2, 6.0, 7.0, 7.5), listOf(900.0, 1100.0, 1400.0, 4100.0), "LM", listOf(35.0, 85.0)),
        "Spinach" to EcocropRange(listOf(2.0, 13.0, 20.0, 27.0), listOf(5.3, 6.0, 7.5, 8.3), listOf(300.0, 800.0, 1200.0, 1700.0), "LM", listOf(40.0, 120.0))
    )

    const val SOURCE_PROBE = "7-in-1 soil probe"
    const val SOURCE_AIR_SENSOR = "DHT11 air sensor"
    const val SOURCE_ECOCROP = "FAO ECOCROP crop ranges"
    const val SOURCE_RECENT_RAIN = "Open-Meteo rainfall history"

    /** Standard environmental lapse rate, °C per metre of height. */
    internal const val LAPSE_RATE = 0.0065

    // ponytail: table N/P/K ranges have no stated unit; set this from a lab soil
    // test (lab value / probe value) instead of trusting the probe's mg/kg as-is.
    const val NPK_TABLE_SCALE = 1.0

    fun recommend(
        soil: SensorReading?,
        site: FarmSiteData?,
        plantingMonth: Int,
        recentRainMm: Double? = null,
        irrigated: Boolean = false
    ): FarmRecommendation {
        val probe = soil?.takeIf { it.soilPh > 0.0 }
        val sources = linkedSetOf(SOURCE_ECOCROP)
        if (probe != null) sources += SOURCE_PROBE
        site?.status?.filterValues { it != SourceStatus.UNAVAILABLE }?.forEach { (name, status) ->
            sources += if (status == SourceStatus.CACHED) "$name (saved copy)" else name
        }
        if (recentRainMm != null) sources += SOURCE_RECENT_RAIN
        val climate = siteClimate(site)

        val ranked = SeedRecommendationEngine.crops.map { profile ->
            val range = ECOCROP.getValue(profile.name)
            val factors = factors(range, plantingMonth, climate, site?.outlook, soil, probe, site?.soil, irrigated)
            factors.firstOrNull { it.source == SOURCE_AIR_SENSOR }?.let { sources += SOURCE_AIR_SENSOR }
            val score = combine(factors)
            CropSuitability(
                crop = SeedRecommendation(profile.name, profile.variety, profile.icon, profile.days, profile.soil, score),
                score = score,
                factors = factors,
                monthlyScores = if (climate == null) emptyList() else (1..12).map { month ->
                    if (month == plantingMonth) score
                    else combine(factors(range, month, climate, outlook = null, soil, probe, site?.soil, irrigated))
                }
            )
        }
            .filter { it.factors.isNotEmpty() }
            .sortedByDescending { it.score }  // stable: ties keep the existing profile order

        return FarmRecommendation(
            ranked = ranked,
            nutrientPlan = ranked.firstOrNull()?.let { nutrientPlan(it.crop.name, probe) }.orEmpty(),
            warnings = warnings(probe, site, climate, recentRainMm),
            sources = sources.toList()
        )
    }

    /** Scores one crop for a site, planting month by month; used by the validation tests. */
    internal fun monthlyScores(
        cropName: String,
        site: FarmSiteData,
        soil: SensorReading? = null,
        irrigated: Boolean = false
    ): List<Int> {
        val climate = siteClimate(site) ?: return emptyList()
        val range = ECOCROP.getValue(cropName)
        val probe = soil?.takeIf { it.soilPh > 0.0 }
        return (1..12).map { month -> combine(factors(range, month, climate, null, soil, probe, site.soil, irrigated)) }
    }

    /** Temperature caps the score; manageable factors scale it between 60 % and 100 %. */
    private fun combine(factors: List<FactorScore>): Int {
        if (factors.isEmpty()) return 0
        val cap = factors.firstOrNull { it.name == "Temperature" }?.score ?: factors.minOf { it.score }
        val manageable = factors.filter { it.name in MANAGEABLE_WEIGHTS }
        val weight = manageable.sumOf { MANAGEABLE_WEIGHTS.getValue(it.name) }
        val quality = if (manageable.isEmpty()) 100.0
        else manageable.sumOf { it.score * MANAGEABLE_WEIGHTS.getValue(it.name) } / weight
        return (cap * (0.6 + 0.4 * quality / 100.0)).roundToInt()
    }

    private val MANAGEABLE_WEIGHTS = mapOf("Soil pH" to 0.5, "Season rainfall" to 0.3, "Soil texture" to 0.2)

    private fun factors(
        range: EcocropRange,
        plantingMonth: Int,
        climate: ClimateNormals?,
        outlook: WeatherOutlook?,
        soil: SensorReading?,
        probe: SensorReading?,
        map: SoilMap?,
        irrigated: Boolean = false
    ): List<FactorScore> = listOfNotNull(
        temperatureFactor(range, plantingMonth, climate, outlook, soil),
        phFactor(range, probe, map),
        rainfallFactor(range, plantingMonth, climate, irrigated),
        textureFactor(range, map)
    )

    /**
     * NASA POWER normals describe a ~50 km grid cell, whose average height can be
     * hundreds of metres off the farm's (Benguet sits far above its cell). The
     * forecast reports the farm's real elevation, so shift the normals by the
     * standard lapse rate when both heights are known.
     */
    internal fun siteClimate(site: FarmSiteData?): ClimateNormals? {
        val normals = site?.normals ?: return null
        val farm = site.outlook?.elevationM ?: site.elevationM
        val cell = normals.cellElevationM
        if (farm == null || cell == null) return normals
        val shift = -LAPSE_RATE * (farm - cell)
        return normals.copy(temperatureC = normals.temperatureC.map { it + shift }, cellElevationM = farm)
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

    /** Recocrop `halfmonths`: the 1st of each month is the mean of it and the previous month. */
    internal fun halfMonths(monthly: List<Double>): List<Double> =
        monthly.indices.flatMap { month ->
            val previous = monthly[(month + 11) % 12]
            listOf((monthly[month] + previous) / 2.0, monthly[month])
        }

    /** Half-month values from the planting month's 1st over the growing season, wrapping into next year. */
    private fun season(monthly: List<Double>, plantingMonth: Int, halfMonths: Int): List<Double> {
        val values = halfMonths(monthly)
        return (0 until halfMonths).map { values[(2 * (plantingMonth - 1) + it) % 24] }
    }

    private fun temperatureFactor(
        range: EcocropRange,
        plantingMonth: Int,
        climate: ClimateNormals?,
        outlook: WeatherOutlook?,
        soil: SensorReading?
    ): FactorScore? {
        val (temperatures, source) = when {
            climate != null -> {
                // The 16-day forecast replaces the normal for the planting month.
                val monthly = climate.temperatureC.toMutableList()
                if (outlook != null) monthly[plantingMonth - 1] = outlook.meanTemperatureC
                season(monthly, plantingMonth, range.seasonHalfMonths) to FarmSiteDataRepository.NASA_POWER
            }
            outlook != null -> listOf(outlook.meanTemperatureC) to FarmSiteDataRepository.OPEN_METEO_FORECAST
            soil != null && soil.temperature != 0.0 -> listOf(soil.temperature) to SOURCE_AIR_SENSOR
            else -> return null
        }
        val score = temperatures.minOf { trapezoid(it, range.temperature) }
        return FactorScore(
            name = "Temperature",
            score = score,
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

    private fun rainfallFactor(
        range: EcocropRange,
        plantingMonth: Int,
        climate: ClimateNormals?,
        irrigated: Boolean
    ): FactorScore? {
        climate ?: return null
        // With irrigation, too little rain is fixable; only too much rain still hurts.
        val limits = if (irrigated) range.monthlyRainfall.let { listOf(-2.0, -1.0, it[2], it[3]) } else range.monthlyRainfall
        val rain = season(climate.rainfallMm, plantingMonth, range.seasonHalfMonths)
        val worst = rain.minBy { trapezoid(it, limits) }
        val hint = when {
            irrigated && worst < range.monthlyRainfall[1] -> " – irrigated, so dry months are fine"
            worst < limits[1] -> " – plan irrigation"
            worst > limits[2] -> " – needs good drainage"
            else -> ""
        }
        return FactorScore(
            name = "Season rainfall",
            score = trapezoid(worst, limits),
            detail = "%.0f–%.0f mm/month expected (best %.0f–%.0f)$hint".format(
                rain.min(), rain.max(), range.monthlyRainfall[1], range.monthlyRainfall[2]
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
        val ph = ECOCROP.getValue(cropName).ph
        val phAdvice = NutrientAdvice(
            nutrient = "Soil pH",
            measured = probe.soilPh,
            targetLow = ph[1],
            targetHigh = ph[2],
            status = when {
                probe.soilPh < ph[1] -> NutrientStatus.LOW
                probe.soilPh > ph[2] -> NutrientStatus.HIGH
                else -> NutrientStatus.OK
            },
            action = when {
                probe.soilPh < ph[1] -> "Acidic: apply agricultural lime (dolomite) 2-3 weeks before planting"
                probe.soilPh > ph[2] -> "Alkaline: add compost and use ammonium sulfate as the nitrogen source"
                else -> "No pH correction needed"
            }
        )
        return listOf(phAdvice) + listOf(
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

    private fun warnings(
        probe: SensorReading?,
        site: FarmSiteData?,
        climate: ClimateNormals?,
        recentRainMm: Double?
    ): List<String> = buildList {
        val mappedPh = site?.soil?.ph
        if (probe != null && mappedPh != null && abs(probe.soilPh - mappedPh) > 1.0) {
            add("Probe pH %.1f differs from the SoilGrids map (%.1f) by more than 1.0. Check the probe with a pH buffer or a lab test.".format(probe.soilPh, mappedPh))
        }
        val normals = site?.normals
        if (normals != null && climate != null && climate.temperatureC != normals.temperatureC) {
            val shift = climate.temperatureC[0] - normals.temperatureC[0]
            if (abs(shift) >= 1.0) {
                add("Climate normals were adjusted by %+.1f °C for the farm's elevation.".format(shift))
            }
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
