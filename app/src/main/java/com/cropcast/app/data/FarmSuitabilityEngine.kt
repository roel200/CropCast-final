package com.cropcast.app.data

import com.cropcast.app.data.model.SeedRecommendation
import com.cropcast.app.data.model.SensorReading
import org.json.JSONObject
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

/** FAO land suitability classes (FAO Framework for Land Evaluation). */
enum class SuitabilityClass(val code: String, val label: String) {
    S1("S1", "Highly suitable"),
    S2("S2", "Moderately suitable"),
    S3("S3", "Marginally suitable"),
    N("N", "Not suitable");

    companion object {
        fun of(score: Int): SuitabilityClass = when {
            score >= 80 -> S1
            score >= 60 -> S2
            score >= 40 -> S3
            else -> N
        }
    }
}

/** PSA national averages; see scripts/build_crop_economics.py. */
data class CropEconomics(
    val yieldTonnesPerHa: Double? = null,
    val farmgatePricePhpPerKg: Double? = null,
    val grossPhpPerHa: Double? = null
)

data class CropSuitability(
    val crop: SeedRecommendation,
    val score: Int,
    val factors: List<FactorScore>,
    /** Score if planted in each month, January first; empty without climate normals. */
    val monthlyScores: List<Int> = emptyList(),
    /** Season risks for the chosen planting month: typhoons, disease, rotation. Advice only. */
    val risks: List<String> = emptyList(),
    val economics: CropEconomics? = null
) {
    val suitabilityClass: SuitabilityClass get() = SuitabilityClass.of(score)

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
    val status: NutrientStatus,
    val detail: String,
    val action: String,
    val applyKgPerHa: Double? = null
)

data class FarmRecommendation(
    val ranked: List<CropSuitability>,
    val nutrientPlan: List<NutrientAdvice>,
    val warnings: List<String>,
    val sources: List<String>
) {
    val top: CropSuitability? get() = ranked.firstOrNull()

    /** Highest PSA gross income among S1/S2 crops; may differ from the most suitable crop. */
    val bestValue: CropSuitability?
        get() = ranked.filter { it.score >= 60 && it.economics?.grossPhpPerHa != null }
            .maxByOrNull { it.economics!!.grossPhpPerHa!! }
}

/**
 * Ranks CropCast's crops for a farm by combining the soil probe with free
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
 * zeroes. Salinity then multiplies the score by FAO's relative yield (Maas &
 * Hoffman), and a wet, warm season by a disease yield loss for disease-prone
 * vegetable families. N/P/K never rank crops; fertilizer fixes them, so they and pH drive
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
        "Spinach" to EcocropRange(listOf(2.0, 13.0, 20.0, 27.0), listOf(5.3, 6.0, 7.5, 8.3), listOf(300.0, 800.0, 1200.0, 1700.0), "LM", listOf(40.0, 120.0)),
        "Pechay" to EcocropRange(listOf(10.0, 20.0, 25.0, 32.0), listOf(5.0, 5.5, 7.0, 7.5), listOf(300.0, 900.0, 1400.0, 2000.0), "LM", listOf(21.0, 45.0)),
        "Kangkong" to EcocropRange(listOf(10.0, 15.0, 35.0, 40.0), listOf(4.3, 5.0, 7.0, 7.5), listOf(700.0, 2000.0, 2500.0, 4200.0), "HO", listOf(30.0, 70.0)),
        "Ampalaya" to EcocropRange(listOf(15.0, 22.0, 30.0, 38.0), listOf(4.5, 6.0, 6.5, 8.0), listOf(1000.0, 2000.0, 2500.0, 4000.0), "MO", listOf(50.0, 70.0)),
        "Sitaw" to EcocropRange(listOf(14.0, 20.0, 35.0, 38.0), listOf(4.3, 5.5, 7.0, 7.5), listOf(650.0, 1500.0, 2000.0, 4100.0), "MH", listOf(50.0, 150.0)),
        "Kalabasa" to EcocropRange(listOf(10.0, 20.0, 30.0, 40.0), listOf(4.5, 5.5, 7.5, 8.3), listOf(300.0, 600.0, 1600.0, 2800.0), "MO", listOf(80.0, 140.0))
    )

    /** Maas-Hoffman salt tolerance: relative yield = 100 - slope x (ECe - threshold). */
    internal data class SaltTolerance(val thresholdDsM: Double, val slopePctPerDsM: Double, val source: String)

    // FAO Irrigation and Drainage Paper 61, Annex 1 (after Maas & Grattan). Okra and pumpkin are
    // rated "moderately sensitive" without numbers there; they get that class's lower threshold
    // (1.3 dS/m) and the median slope of the moderately sensitive crops below (11 %/dS/m).
    // FAO lists no data for alugbati, kangkong or ampalaya, so they have no salinity factor.
    internal val SALT = mapOf(
        "Tomato" to SaltTolerance(2.5, 9.9, "FAO"),
        "Cabbage" to SaltTolerance(1.8, 9.7, "FAO"),
        "Corn" to SaltTolerance(1.7, 12.0, "FAO"),
        "Potato" to SaltTolerance(1.7, 12.0, "FAO"),
        "Rice" to SaltTolerance(3.0, 12.0, "FAO"),
        "Lettuce" to SaltTolerance(1.3, 13.0, "FAO"),
        "Cucumber" to SaltTolerance(2.5, 13.0, "FAO"),
        "Spinach" to SaltTolerance(2.0, 7.6, "FAO"),
        "Sweet Potato" to SaltTolerance(1.5, 11.0, "FAO"),
        "Eggplant" to SaltTolerance(1.1, 6.9, "FAO"),
        "Sitaw" to SaltTolerance(4.9, 12.0, "FAO (cowpea)"),
        "Pechay" to SaltTolerance(3.3, 4.3, "FAO (turnip greens, B. rapa)"),
        "Okra" to SaltTolerance(1.3, 11.0, "FAO class MS"),
        "Kalabasa" to SaltTolerance(1.3, 11.0, "FAO class MS")
    )

    /**
     * Crop metadata. [needKgHa] is the crop's N, P2O5 and K2O need in kg/ha: the
     * crop-requirement table (data/processed/crop_requirements_selected.json) lists
     * values such as corn 150-200 N, which are per-hectare rates, not soil levels.
     */
    internal data class FarmCrop(
        val profile: SeedRecommendation,
        val family: String,
        val tall: Boolean,
        val needKgHa: List<ClosedFloatingPointRange<Double>>?
    ) {
        val name: String get() = profile.name
    }

    private val FAMILY = mapOf(
        "Tomato" to "Solanaceae", "Potato" to "Solanaceae", "Eggplant" to "Solanaceae",
        "Okra" to "Malvaceae", "Alugbati" to "Basellaceae", "Rice" to "Poaceae", "Corn" to "Poaceae",
        "Cucumber" to "Cucurbitaceae", "Ampalaya" to "Cucurbitaceae", "Kalabasa" to "Cucurbitaceae",
        "Cabbage" to "Brassicaceae", "Pechay" to "Brassicaceae", "Sweet Potato" to "Convolvulaceae",
        "Kangkong" to "Convolvulaceae", "Lettuce" to "Asteraceae", "Spinach" to "Amaranthaceae", "Sitaw" to "Fabaceae"
    )

    /** Bacterial wilt and fusarium (Solanaceae, Cucurbitaceae), clubroot and black rot (Brassicaceae). */
    private val ROTATION_FAMILIES = setOf("Solanaceae", "Cucurbitaceae", "Brassicaceae")

    /** Staked, trellised or tall crops that typhoon winds flatten. */
    private val TALL = setOf("Tomato", "Okra", "Rice", "Corn", "Eggplant", "Cucumber", "Ampalaya", "Sitaw")

    internal val CROPS: List<FarmCrop> = SeedRecommendationEngine.crops.map { p ->
        FarmCrop(
            SeedRecommendation(p.name, p.variety, p.icon, p.days, p.soil),
            FAMILY.getValue(p.name), p.name in TALL, listOf(p.nitrogen, p.phosphorus, p.potassium)
        )
    } + listOf(
        // Pechay uses the table's turnip rates (same species, Brassica rapa); kalabasa uses pumpkin's.
        FarmCrop(SeedRecommendation("Pechay", "Native", "🥬", 30, "Fertile loam"), "Brassicaceae", false,
            listOf(80.0..120.0, 40.0..80.0, 60.0..120.0)),
        FarmCrop(SeedRecommendation("Kangkong", "Upland", "🌿", 30, "Moist loam to clay"), "Convolvulaceae", false, null),
        FarmCrop(SeedRecommendation("Ampalaya", "Hybrid", "🥒", 60, "Well-drained loam"), "Cucurbitaceae", true, null),
        FarmCrop(SeedRecommendation("Sitaw", "Pole", "🫛", 60, "Well-drained loam"), "Fabaceae", true, null),
        FarmCrop(SeedRecommendation("Kalabasa", "Native", "🎃", 90, "Well-drained loam"), "Cucurbitaceae", false,
            listOf(80.0..120.0, 60.0..100.0, 100.0..150.0))
    )

    const val SOURCE_PROBE = "7-in-1 soil probe"
    const val SOURCE_AIR_SENSOR = "DHT11 air sensor"
    const val SOURCE_ECOCROP = "FAO ECOCROP crop ranges"
    const val SOURCE_RECENT_RAIN = "Open-Meteo rainfall history"
    const val SOURCE_PSA = "PSA yields and farmgate prices"

    /** Standard environmental lapse rate, °C per metre of height. */
    internal const val LAPSE_RATE = 0.0065

    // ponytail: the probe reports bulk soil EC; FAO salt data use ECe (saturated paste).
    // The ratio depends on soil water and texture, so set it from one lab ECe test.
    const val PROBE_EC_TO_ECE = 1.0

    // ponytail: general soil-test interpretation levels in mg/kg (available N from the
    // 280/560 kg/ha classes; Olsen/Bray P; exchangeable K at 0.2/0.4 cmol/kg). The
    // probe's NPK is an estimate, so confirm its class once against a BSWM lab test.
    internal val SOIL_TEST_LEVELS = listOf(125.0 to 250.0, 10.0 to 25.0, 78.0 to 156.0)

    fun recommend(
        soil: SensorReading?,
        site: FarmSiteData?,
        plantingMonth: Int,
        recentRainMm: Double? = null,
        irrigated: Boolean = false,
        latitude: Double? = null,
        previousCrop: String? = null,
        economics: Map<String, CropEconomics> = emptyMap()
    ): FarmRecommendation {
        val probe = soil?.takeIf { it.soilPh > 0.0 }
        val sources = linkedSetOf(SOURCE_ECOCROP)
        if (probe != null) sources += SOURCE_PROBE
        site?.status?.filterValues { it != SourceStatus.UNAVAILABLE }?.forEach { (name, status) ->
            sources += if (status == SourceStatus.CACHED) "$name (saved copy)" else name
        }
        if (recentRainMm != null) sources += SOURCE_RECENT_RAIN
        if (economics.isNotEmpty()) sources += SOURCE_PSA
        val climate = siteClimate(site)
        val previous = previousCrop?.let { name -> CROPS.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } }

        val ranked = CROPS.map { crop ->
            val range = ECOCROP.getValue(crop.name)
            val factors = factors(crop, range, plantingMonth, climate, site?.outlook, soil, probe, site?.soil, irrigated)
            factors.firstOrNull { it.source == SOURCE_AIR_SENSOR }?.let { sources += SOURCE_AIR_SENSOR }
            val score = combine(factors)
            CropSuitability(
                crop = crop.profile.copy(confidence = score),
                score = score,
                factors = factors,
                monthlyScores = if (climate == null) emptyList() else (1..12).map { month ->
                    if (month == plantingMonth) score
                    else combine(factors(crop, range, month, climate, outlook = null, soil, probe, site?.soil, irrigated))
                },
                risks = risks(crop, range, plantingMonth, climate, latitude, previous),
                economics = economics[crop.name]
            )
        }
            .filter { it.factors.isNotEmpty() }
            .sortedByDescending { it.score }  // stable: ties keep the catalog order

        val topCrop = ranked.firstOrNull()?.let { top -> CROPS.first { it.name == top.crop.name } }
        return FarmRecommendation(
            ranked = ranked,
            nutrientPlan = topCrop?.let { nutrientPlan(it, probe) }.orEmpty(),
            warnings = warnings(probe, site, climate, recentRainMm) +
                ranked.firstOrNull()?.risks.orEmpty().map { "${ranked.first().crop.name}: $it" },
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
        val crop = CROPS.first { it.name == cropName }
        val range = ECOCROP.getValue(cropName)
        val probe = soil?.takeIf { it.soilPh > 0.0 }
        return (1..12).map { month -> combine(factors(crop, range, month, climate, null, soil, probe, site.soil, irrigated)) }
    }

    /** Temperature caps the score; manageable factors scale it to 60-100 %; salinity and disease scale by relative yield. */
    private fun combine(factors: List<FactorScore>): Int {
        if (factors.isEmpty()) return 0
        val relativeYield = factors.filter { it.name in YIELD_LOSSES }.fold(1.0) { yield, it -> yield * it.score / 100.0 }
        val rest = factors.filter { it.name !in YIELD_LOSSES }.ifEmpty { return (100 * relativeYield).roundToInt() }
        val cap = rest.firstOrNull { it.name == "Temperature" }?.score ?: rest.minOf { it.score }
        val manageable = rest.filter { it.name in MANAGEABLE_WEIGHTS }
        val weight = manageable.sumOf { MANAGEABLE_WEIGHTS.getValue(it.name) }
        val quality = if (manageable.isEmpty()) 100.0
        else manageable.sumOf { it.score * MANAGEABLE_WEIGHTS.getValue(it.name) } / weight
        return (cap * (0.6 + 0.4 * quality / 100.0) * relativeYield).roundToInt()
    }

    private const val SALINITY = "Soil salinity"
    private const val DISEASE = "Wet-season disease"
    private val YIELD_LOSSES = setOf(SALINITY, DISEASE)

    /** Families whose wet-season diseases shape Philippine planting calendars, with the advice shown. */
    internal val WET_SEASON_DISEASE = mapOf(
        "Solanaceae" to "high risk of bacterial wilt and late blight. Use raised beds and resistant varieties.",
        "Cucurbitaceae" to "high risk of downy mildew. Trellis for airflow and avoid overhead watering.",
        "Brassicaceae" to "high risk of black rot and soft rot. Ensure drainage and rotate fields."
    )

    // ponytail: flat yield loss for a wet, warm season. Calibrated on the PSA harvest-quarter
    // test (15 % and 30 % agree equally; the gentler one is kept). Replace with per-crop losses
    // if wet-season yield data (e.g. PSA quarterly yields) become available.
    internal const val WET_SEASON_LOSS_PCT = 15
    private val MANAGEABLE_WEIGHTS = mapOf("Soil pH" to 0.5, "Season rainfall" to 0.3, "Soil texture" to 0.2)

    private fun factors(
        crop: FarmCrop,
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
        textureFactor(range, map),
        salinityFactor(crop, probe),
        diseaseFactor(crop, range, plantingMonth, climate)
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

    /** Calendar months (1-12) the growing season touches. */
    private fun seasonMonths(range: EcocropRange, plantingMonth: Int): List<Int> =
        (0 until ceil(range.seasonHalfMonths / 2.0).toInt()).map { (plantingMonth - 1 + it) % 12 + 1 }

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
        // Every crop here tolerates a wide texture range in ECOCROP, so a
        // non-optimal texture lowers the score instead of excluding the crop.
        val optimal = texture in range.textures || 'W' in range.textures
        return FactorScore(
            name = "Soil texture",
            score = if (optimal) 100 else 70,
            detail = "Mapped soil is $name, %.0f%% clay".format(clay) + if (optimal) "" else " – add organic matter",
            source = FarmSiteDataRepository.SOILGRIDS
        )
    }

    /** ECe in dS/m from the probe's bulk EC in µS/cm, or null when the probe sent none. */
    internal fun ece(probe: SensorReading?): Double? =
        probe?.electricalConductivity?.takeIf { it > 0.0 }?.let { it / 1000.0 * PROBE_EC_TO_ECE }

    private fun salinityFactor(crop: FarmCrop, probe: SensorReading?): FactorScore? {
        val salt = SALT[crop.name] ?: return null
        val ece = ece(probe) ?: return null
        val relativeYield = (100.0 - salt.slopePctPerDsM * (ece - salt.thresholdDsM).coerceAtLeast(0.0)).coerceIn(0.0, 100.0)
        return FactorScore(
            name = SALINITY,
            score = relativeYield.roundToInt(),
            detail = "ECe ≈ %.1f dS/m; yield falls %.1f %% per dS/m above %.1f (%s)".format(
                ece, salt.slopePctPerDsM, salt.thresholdDsM, salt.source
            ),
            source = SOURCE_PROBE
        )
    }

    /** Season mean rain above 250 mm/month and temperature above 24 °C over the crop's months. */
    private fun wetAndWarm(range: EcocropRange, plantingMonth: Int, climate: ClimateNormals): Boolean {
        val months = seasonMonths(range, plantingMonth)
        return months.map { climate.rainfallMm[it - 1] }.average() > 250.0 &&
            months.map { climate.temperatureC[it - 1] }.average() > 24.0
    }

    /**
     * Vegetables in the Philippines are mostly grown in the dry season because wet-season
     * disease cuts yields. ECOCROP has no disease term, so without this the engine favoured
     * the wettest months for eggplant and ampalaya, opposite to when PSA records harvests.
     */
    private fun diseaseFactor(crop: FarmCrop, range: EcocropRange, plantingMonth: Int, climate: ClimateNormals?): FactorScore? {
        if (climate == null || crop.family !in WET_SEASON_DISEASE) return null
        val wet = wetAndWarm(range, plantingMonth, climate)
        return FactorScore(
            name = DISEASE,
            score = if (wet) 100 - WET_SEASON_LOSS_PCT else 100,
            detail = if (wet) "Wet, warm season: about $WET_SEASON_LOSS_PCT % yield lost to disease – plant in the dry season"
            else "Dry or cool season: low disease pressure",
            source = FarmSiteDataRepository.NASA_POWER
        )
    }

    private fun risks(
        crop: FarmCrop,
        range: EcocropRange,
        plantingMonth: Int,
        climate: ClimateNormals?,
        latitude: Double?,
        previous: FarmCrop?
    ): List<String> = buildList {
        val months = seasonMonths(range, plantingMonth)
        // PAGASA: most tropical cyclones cross Luzon and the Visayas from July to November;
        // Mindanao south of about 9.5° N is rarely hit.
        if (crop.tall && latitude != null && latitude >= 9.5 && months.any { it in 7..11 }) {
            add("Typhoon season (Jul–Nov) overlaps the crop. Stake or trellis firmly, or plant after November.")
        }
        val advice = WET_SEASON_DISEASE[crop.family]
        if (climate != null && advice != null && wetAndWarm(range, plantingMonth, climate)) {
            add("Wet, warm season: $advice")
        }
        // Only families whose soil-borne diseases build up when replanted; continuous rice
        // and rice-corn sequences are normal Philippine practice.
        if (previous != null && previous.family == crop.family && crop.family in ROTATION_FAMILIES) {
            add("Follows ${previous.name} (same family, ${crop.family}). Rotate to another family to break soil-borne disease.")
        }
    }

    internal fun nutrientPlan(crop: FarmCrop, probe: SensorReading?): List<NutrientAdvice> {
        probe ?: return emptyList()
        val ph = ECOCROP.getValue(crop.name).ph
        val phAdvice = NutrientAdvice(
            nutrient = "Soil pH",
            status = when {
                probe.soilPh < ph[1] -> NutrientStatus.LOW
                probe.soilPh > ph[2] -> NutrientStatus.HIGH
                else -> NutrientStatus.OK
            },
            detail = "%.1f (best %.1f–%.1f)".format(probe.soilPh, ph[1], ph[2]),
            action = when {
                probe.soilPh < ph[1] -> "Acidic: apply agricultural lime (dolomite) 2-3 weeks before planting"
                probe.soilPh > ph[2] -> "Alkaline: add compost and use ammonium sulfate as the nitrogen source"
                else -> "No pH correction needed"
            }
        )
        val need = crop.needKgHa ?: return listOf(
            phAdvice,
            NutrientAdvice(
                nutrient = "Fertilizer",
                status = NutrientStatus.OK,
                detail = "No verified per-hectare rate for ${crop.name}",
                action = "Follow the DA production guide for ${crop.name}, or get a BSWM soil test"
            )
        )
        val readings = listOf(probe.nitrogen, probe.phosphorus, probe.potassium)
        val labels = listOf("Nitrogen", "Phosphorus", "Potassium")
        val oxides = listOf("N", "P₂O₅", "K₂O")
        // Straight fertilizers so each nutrient can be dosed alone.
        val products = listOf("Urea (46-0-0)" to 0.46, "Solophos (0-18-0)" to 0.18, "Muriate of potash (0-0-60)" to 0.60)
        return listOf(phAdvice) + readings.indices.map { i ->
            val (low, high) = SOIL_TEST_LEVELS[i]
            val status = when {
                readings[i] < low -> NutrientStatus.LOW
                readings[i] > high -> NutrientStatus.HIGH
                else -> NutrientStatus.OK
            }
            // Soil-test-level rates: low soils get the top of the crop's need, medium the
            // middle, high a maintenance dose of half the bottom of the range.
            val rate = when (status) {
                NutrientStatus.LOW -> need[i].endInclusive
                NutrientStatus.OK -> (need[i].start + need[i].endInclusive) / 2.0
                NutrientStatus.HIGH -> need[i].start / 2.0
            }
            val (product, fraction) = products[i]
            val productKg = rate / fraction
            NutrientAdvice(
                nutrient = labels[i],
                status = status,
                detail = "%.0f mg/kg, %s (medium %.0f–%.0f)".format(
                    readings[i],
                    when (status) { NutrientStatus.LOW -> "low"; NutrientStatus.OK -> "medium"; NutrientStatus.HIGH -> "high" },
                    low, high
                ),
                action = "Apply %.0f kg %s/ha = %.0f kg %s (%.1f bags/ha, %.1f kg per 1,000 m²)".format(
                    rate, oxides[i], productKg, product, productKg / 50.0, productKg / 10.0
                ),
                applyKgPerHa = rate
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
        ece(probe)?.takeIf { it >= 4.0 }?.let {
            add("Soil EC suggests saline soil (ECe ≈ %.1f dS/m). Confirm with a laboratory ECe test and leach with good-quality water.".format(it))
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

    /** Parses app/src/main/assets/crop_economics.json. */
    fun parseEconomics(json: String): Map<String, CropEconomics> {
        val crops = JSONObject(json).getJSONObject("crops")
        return crops.keys().asSequence().associateWith { name ->
            val entry = crops.getJSONObject(name)
            fun value(key: String) = entry.optDouble(key).takeIf { it.isFinite() }
            CropEconomics(value("yieldTonnesPerHa"), value("farmgatePricePhpPerKg"), value("grossPhpPerHa"))
        }
    }
}
