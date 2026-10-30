package com.cropcast.app.data

import com.cropcast.app.data.model.SensorReading
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CheckLevel { ACT, WATCH, TIP, OK, NONE }

/** One reading against the crop's range. [tolerates] null means no bar; [best] null means no range. */
data class CropCheck(
    val name: String,
    val value: Double,
    val unit: String,
    val level: CheckLevel,
    val best: ClosedFloatingPointRange<Double>?,
    val tolerates: ClosedFloatingPointRange<Double>?,
    val axis: ClosedFloatingPointRange<Double>,
    /** Fixed English label shown beside the value, e.g. "Low" for nutrients; null for range rows. */
    val note: String? = null
)

/** Fixed English sentences with no numbers, so the UI can translate each whole. */
data class CropAction(val level: CheckLevel, val title: String, val why: String, val how: String)

data class Fertilizer(
    val name: String,
    val alias: String,
    val grade: String,
    val kgPer1000m2: Double,
    /** N, P2O5, K2O supplied in kg per 1,000 m². */
    val gives: List<Double>,
    val whenText: String,
    val color: Long
) {
    val sacks: String get() = FarmSuitabilityEngine.sacks(kgPer1000m2)
}

data class MyCropAdvice(
    val cropName: String,
    val variety: String,
    val icon: String,
    val planted: LocalDate,
    val harvest: LocalDate,
    val day: Int,
    val totalDays: Int,
    val stage: String,
    val checks: List<CropCheck>,
    val actions: List<CropAction>,
    /** Empty when the crop has no verified fertilizer rate. */
    val mix: List<Fertilizer>,
    val single: List<Fertilizer>,
    val lowNutrients: List<String>,
    val nearHarvest: Boolean,
    val phHigh: Boolean
) {
    val progress: Float get() = (day.toFloat() / totalDays).coerceIn(0f, 1f)
    fun count(level: CheckLevel) = checks.count { it.level == level }
}

/** Turns the crop in the field, its planting date and the sensor into checks, actions and a fertilizer plan. */
object MyCropAdvisor {
    private class Product(
        val name: String, val alias: String, val grade: String, val color: Long,
        val n: Double, val p: Double, val k: Double
    )

    private val UREA = Product("Urea", "Urea", "46-0-0", 0xFF1F5F9E, 0.46, 0.0, 0.0)
    private val AMMOSUL = Product("Ammonium sulfate", "Ammosul", "21-0-0", 0xFF2E7D4F, 0.21, 0.0, 0.0)
    private val COMPLETE = Product("Complete", "Complete or Triple 14", "14-14-14", 0xFFB23A2B, 0.14, 0.14, 0.14)
    private val AMMOPHOS = Product("Ammophos", "Ammophos (16-20)", "16-20-0", 0xFF6B4A93, 0.16, 0.20, 0.0)
    private val SOLOPHOS = Product("Solophos", "Solophos", "0-18-0", 0xFF7A5C0C, 0.0, 0.18, 0.0)
    private val MOP = Product("Muriate of potash", "Potash (0-0-60)", "0-0-60", 0xFF8E1F2A, 0.0, 0.0, 0.60)

    private val LEAFY = setOf("Brassicaceae", "Asteraceae", "Amaranthaceae", "Basellaceae")

    fun advise(cropName: String, planted: LocalDate, today: LocalDate, reading: SensorReading): MyCropAdvice? {
        val crop = FarmSuitabilityEngine.CROPS.firstOrNull { it.name == cropName } ?: return null
        val eco = FarmSuitabilityEngine.ECOCROP.getValue(cropName)
        val profile = SeedRecommendationEngine.crops.firstOrNull { it.name == cropName }
        val total = crop.profile.growDays
        val day = ChronoUnit.DAYS.between(planted, today).toInt().coerceIn(0, total)
        val fraction = day.toDouble() / total
        val stageIdx = when { fraction < 0.2 -> 0; fraction < 0.5 -> 1; fraction < 0.85 -> 2; else -> 3 }
        val nearHarvest = stageIdx == 3
        val rice = cropName == "Rice"
        val drained = rice && nearHarvest

        fun fourPoint(v: Double, r: List<Double>) = when {
            v in r[1]..r[2] -> CheckLevel.OK
            v in r[0]..r[3] -> CheckLevel.WATCH
            else -> CheckLevel.ACT
        }
        val tempLv = fourPoint(reading.temperature, eco.temperature)
        val phLv = fourPoint(reading.soilPh, eco.ph)
        val phHigh = reading.soilPh > eco.ph[2]
        val humLv = profile?.humidity?.let { r ->
            when {
                reading.humidity in r -> CheckLevel.OK
                (if (reading.humidity < r.start) r.start - reading.humidity else reading.humidity - r.endInclusive) <= 10 -> CheckLevel.WATCH
                else -> CheckLevel.ACT
            }
        } ?: CheckLevel.NONE
        val moistLv = profile?.moisture?.let { r ->
            when {
                drained -> CheckLevel.OK
                reading.soilMoisture < r.start -> CheckLevel.ACT
                reading.soilMoisture > r.endInclusive + 10 && !rice -> CheckLevel.ACT
                reading.soilMoisture > r.endInclusive -> CheckLevel.WATCH
                else -> CheckLevel.OK
            }
        } ?: CheckLevel.NONE

        // Same soil-test levels and rates as the Crop Recommendation fertilizer plan.
        val nutrients = FarmSuitabilityEngine.nutrientPlan(crop, reading, growing = true).drop(1).takeIf { it.size == 3 }
        val lowNutrients = nutrients?.filter { it.status == NutrientStatus.LOW }?.map { it.nutrient }.orEmpty()

        val checks = buildList {
            add(CropCheck("Air temperature", reading.temperature, "°C", tempLv, eco.temperature[1]..eco.temperature[2], eco.temperature[0]..eco.temperature[3], 0.0..45.0))
            add(profile?.humidity?.let { r ->
                CropCheck("Humidity", reading.humidity, "%", humLv, r, (r.start - 10).coerceAtLeast(0.0)..(r.endInclusive + 10).coerceAtMost(100.0), 30.0..100.0)
            } ?: CropCheck("Humidity", reading.humidity, "%", humLv, null, null, 30.0..100.0))
            add(profile?.moisture?.let { r ->
                CropCheck("Soil moisture", reading.soilMoisture, "%", moistLv, r, r.start..(r.endInclusive + 10).coerceAtMost(100.0), 0.0..100.0,
                    note = if (drained) "Field should be drained now" else null)
            } ?: CropCheck("Soil moisture", reading.soilMoisture, "%", moistLv, null, null, 0.0..100.0))
            add(CropCheck("Soil pH", reading.soilPh, "", phLv, eco.ph[1]..eco.ph[2], eco.ph[0]..eco.ph[3], 3.0..9.5))
            nutrients?.forEachIndexed { i, advice ->
                val (low, high) = FarmSuitabilityEngine.SOIL_TEST_LEVELS[i]
                val level = when (advice.status) {
                    NutrientStatus.LOW -> if (nearHarvest) CheckLevel.WATCH else CheckLevel.ACT
                    NutrientStatus.HIGH -> CheckLevel.WATCH
                    NutrientStatus.OK -> CheckLevel.OK
                }
                val value = listOf(reading.nitrogen, reading.phosphorus, reading.potassium)[i]
                add(CropCheck(advice.nutrient, value, "mg/kg", level, low..high, null, 0.0..1.0,
                    note = when (advice.status) { NutrientStatus.LOW -> "Low"; NutrientStatus.HIGH -> "High"; NutrientStatus.OK -> "Normal" }))
            }
        }

        val actions = buildList {
            if (moistLv == CheckLevel.ACT) {
                val low = reading.soilMoisture < (profile?.moisture?.start ?: 0.0)
                add(when {
                    low && rice -> CropAction(CheckLevel.ACT, "Flood the field", "Soil moisture is below what rice needs.", "Raise the water to 3–5 cm. Check the bunds for leaks.")
                    low -> CropAction(CheckLevel.ACT, "Water today", "Soil moisture is below what this crop needs.", "Water slowly at the base until the top 15 cm is moist. Water in the early morning. Check again tomorrow.")
                    else -> CropAction(CheckLevel.ACT, "Drain the field", "Soil moisture is far above what this crop tolerates.", "Open a drainage canal between rows. Stop watering until it drops.")
                })
            }
            if (lowNutrients.isNotEmpty()) {
                add(if (nearHarvest) CropAction(CheckLevel.WATCH, "No more fertilizer", "Some nutrients read low, but harvest is close.", "Skip fertilizer now. Use the fertilizer plan below before the next planting.")
                else CropAction(CheckLevel.ACT, "Add fertilizer", "Some nutrients read low while the crop is growing, so feed it now.", "See the fertilizer plan below."))
            }
            if (reading.humidity > 80 && reading.temperature > 25) {
                add(when {
                    rice -> CropAction(CheckLevel.WATCH, "Humid: blast and sheath blight risk", "Humidity and heat are both high.", "Do not add extra nitrogen. Walk the field and look for diamond-shaped leaf spots.")
                    cropName == "Corn" -> CropAction(CheckLevel.WATCH, "Humid: leaf blight risk", "Humidity and heat are both high.", "Look for long grey-brown leaf spots. Remove badly hit lower leaves.")
                    crop.family in LEAFY -> CropAction(CheckLevel.WATCH, "Humid: soft rot risk", "Humidity and heat are both high.", "Harvest on time. Do not leave cut leaves in the bed.")
                    else -> CropAction(CheckLevel.WATCH, "Warm and humid: wilt and blight risk", "Humidity and heat are both high.", "Remove yellow lower leaves. Water the soil, not the leaves. Keep space between plants for air.")
                })
            }
            if (tempLv != CheckLevel.OK) {
                add(if (reading.temperature < eco.temperature[1])
                    CropAction(tempLv, "Cool for this crop", "Air temperature is below the best range, so growth slows.", "Cover young plants at night. Do not over-water.")
                else if (crop.family in LEAFY)
                    CropAction(tempLv, "Hot for leafy crops", "Air temperature is above the best range. Leafy crops may flower early and turn bitter.", "Give afternoon shade, or harvest a few days early.")
                else
                    CropAction(tempLv, "Warm for this crop", "Air temperature is above the best range. Hot days reduce flowering and fruit set.", "Mulch with rice straw to keep roots cool. Water in the morning."))
            }
            if (phLv != CheckLevel.OK) {
                add(if (phHigh)
                    CropAction(phLv, "Soil pH is high for this crop", "The probe reads above the best range. Check the probe first.", "Dip the probe in pH 7 buffer. If the reading holds, use ammonium sulfate (21-0-0) instead of urea and mix in compost.")
                else
                    CropAction(phLv, "Soil pH is low for this crop", "The probe reads below the best range. Check the probe first.", "Dip the probe in pH 7 buffer. If the reading holds, mix in agricultural lime (dolomite) before the next planting."))
            }
            val tip = when {
                cropName == "Rice" && stageIdx == 3 -> CropAction(CheckLevel.TIP, "Drain before harvest", "Your rice is ripening.", "Keep the field drained 1–2 weeks before harvest so grain dries evenly.")
                cropName == "Corn" && stageIdx == 2 -> CropAction(CheckLevel.TIP, "Do not let it dry out now", "Your corn is tasseling.", "Tasseling is when corn needs water most. A dry week now cuts the harvest.")
                crop.family == "Solanaceae" && stageIdx == 2 -> CropAction(CheckLevel.TIP, "Keep moisture steady", "Your crop is flowering and fruiting.", "Dry-then-wet swings cause blossom-end rot (black fruit bottoms). Water a little every day.")
                stageIdx == 0 -> CropAction(CheckLevel.TIP, "Protect young plants", "Your crop is a seedling.", "Shade at noon on hot days. Check for cutworms at night.")
                stageIdx == 3 -> CropAction(CheckLevel.TIP, "Plan the harvest", "Harvest is close.", "Line up buyers and sacks this week.")
                else -> null
            }
            tip?.let(::add)
        }.sortedBy { it.level }

        val (mix, single) = fertilizer(nutrients, stageIdx, phHigh)
        return MyCropAdvice(
            cropName, crop.profile.variety, crop.profile.icon, planted, planted.plusDays(total.toLong()), day, total,
            listOf("Seedling", "Growing", "Flowering and fruiting", "Ready to harvest")[stageIdx],
            checks, actions, mix, single, lowNutrients, nearHarvest, phHigh
        )
    }

    /** Common mix: one compound sized to the phosphorus need, topped up with straights. Single: one product per nutrient. */
    private fun fertilizer(nutrients: List<NutrientAdvice>?, stageIdx: Int, phHigh: Boolean): Pair<List<Fertilizer>, List<Fertilizer>> {
        nutrients ?: return emptyList<Fertilizer>() to emptyList()
        val (needN, needP, needK) = nutrients.map { it.applyKgPerHa ?: 0.0 }
        fun item(p: Product, kgHa: Double) = Fertilizer(
            p.name, p.alias, p.grade, kgHa / 10.0, listOf(kgHa * p.n / 10.0, kgHa * p.p / 10.0, kgHa * p.k / 10.0),
            when {
                p.p > 0 -> "Now, in a small hole 5 cm beside the roots, then cover"
                p.k > 0 -> "Now, together with the nitrogen"
                stageIdx <= 1 -> "Half now, half in 3–4 weeks, beside the plants"
                else -> "Now, beside the plants, then water it in"
            },
            p.color
        )
        val nSource = if (phHigh) AMMOSUL else UREA
        val base = if (nutrients[2].status == NutrientStatus.HIGH) AMMOPHOS else COMPLETE
        val baseKg = needP / base.p
        val mix = buildList {
            add(item(base, baseKg))
            (needN - baseKg * base.n).takeIf { it > 5 }?.let { add(item(nSource, it / nSource.n)) }
            (needK - baseKg * base.k).takeIf { it > 5 }?.let { add(item(MOP, it / MOP.k)) }
        }
        return mix to listOf(item(nSource, needN / nSource.n), item(SOLOPHOS, needP / SOLOPHOS.p), item(MOP, needK / MOP.k))
    }
}
