package com.cropcast.app.data

import com.cropcast.app.data.model.SensorReading
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Checks the engine against where crops really grow in the Philippines.
 *
 * Ground truth: PSA 2021-2025 production by province and the SoilsSync farms
 * (see scripts/build_validation_data.py). Each site gets the data the app would
 * download there: NASA POWER normals, farm elevation and SoilGrids soil. A crop
 * counts as suitable at a site when its best planting month scores >= 50.
 */
class FarmRealWorldValidationTest {
    private val fixture: JSONObject? = listOf(
        File("../data/validation/sites.json"),
        File("data/validation/sites.json")
    ).firstOrNull(File::isFile)?.let { JSONObject(it.readText()) }

    private val daysInMonth = listOf(31.0, 28.25, 31.0, 30.0, 31.0, 30.0, 31.0, 31.0, 30.0, 31.0, 30.0, 31.0)

    private fun JSONArray.doubles() = (0 until length()).map(::getDouble)

    private fun site(json: JSONObject): FarmSiteData? {
        val climate = json.optJSONObject("climate") ?: return null
        val soil = json.optJSONObject("soil")
        return FarmSiteData(
            normals = ClimateNormals(
                temperatureC = climate.getJSONArray("temperatureC").doubles(),
                humidityPct = climate.getJSONArray("humidityPct").doubles(),
                rainfallMm = climate.getJSONArray("rainMmPerDay").doubles().mapIndexed { i, perDay -> perDay * daysInMonth[i] },
                cellElevationM = climate.optDouble("cellElevationM").takeIf { it.isFinite() }
            ),
            soil = soil?.let {
                SoilMap(
                    ph = it.optDouble("ph").takeIf(Double::isFinite),
                    clayPct = it.optDouble("clayPct").takeIf(Double::isFinite),
                    sandPct = it.optDouble("sandPct").takeIf(Double::isFinite),
                    nitrogenGPerKg = null
                )
            },
            elevationM = json.optDouble("elevationM").takeIf { it.isFinite() }
        )
    }

    private fun bestScore(crop: String, site: FarmSiteData, soil: SensorReading? = null): Int =
        FarmSuitabilityEngine.monthlyScores(crop, site, soil).maxOrNull() ?: 0

    /** Probability a producing province outscores a non-producing one (ties count half). */
    private fun auc(producers: List<Int>, others: List<Int>): Double {
        if (producers.isEmpty() || others.isEmpty()) return Double.NaN
        var wins = 0.0
        for (p in producers) for (o in others) wins += if (p > o) 1.0 else if (p == o) 0.5 else 0.0
        return wins / (producers.size * others.size)
    }

    @Test
    fun ratesCropsSuitableWhereThePhilippinesGrowsThem() {
        val data = fixture
        assumeTrue("run scripts/build_validation_data.py first", data != null)
        val provinces = data!!.getJSONArray("provinces").let { array ->
            (0 until array.length()).map(array::getJSONObject).mapNotNull { json -> site(json)?.let { json to it } }
        }
        val report = StringBuilder("\nPSA provinces with climate data: ${provinces.size}\n")
        report.append("%-12s %9s %11s %7s   %s\n".format("crop", "prod.cov", "top5 suit.", "AUC", "top producers (best score)"))
        val coverage = mutableMapOf<String, Double>()
        for (crop in FarmSuitabilityEngine.ECOCROP.keys) {
            val rows = provinces.map { (json, site) ->
                val tonnes = json.getJSONObject("production").optJSONObject(crop)?.optDouble("annualTonnes") ?: 0.0
                Triple(json.getString("name"), tonnes, bestScore(crop, site))
            }
            val total = rows.sumOf { it.second }
            val covered = rows.filter { it.third >= 50 }.sumOf { it.second } / total
            coverage[crop] = covered
            val top5 = rows.sortedByDescending { it.second }.take(5)
            val producing = rows.filter { it.second > 0.0 }
            // Major producers: the biggest quarter of producing provinces. Non-producers: none recorded.
            val major = producing.sortedByDescending { it.second }.take(maxOf(1, producing.size / 4)).map { it.third }
            val none = rows.filter { it.second == 0.0 }.map { it.third }
            report.append("%-12s %8.0f%% %8d/5 %7s   %s\n".format(
                crop, covered * 100, top5.count { it.third >= 50 },
                // AUC only means something when enough provinces grow none of the crop.
                if (none.size >= 10) "%.2f".format(auc(major, none)) else "n/a",
                top5.joinToString { "${it.first} ${it.third}" }
            ))
        }
        println(report)
        // At least 80 % of each crop's national production is grown where the engine says it can grow.
        // Spinach is excluded: CropCast models true spinach (Spinacia oleracea, dies above 27 °C),
        // while PSA's lowland "spinach" in Capiz or Isabela is a different, heat-tolerant leafy green.
        coverage.filterKeys { it != "Spinach" }.forEach { (crop, share) ->
            assertTrue("$crop production coverage %.2f".format(share), share >= 0.80)
        }
    }

    /**
     * PSA reports production by harvest quarter. If the planting calendar is
     * right, quarters the engine favours for harvest should be the quarters in
     * which provinces actually harvest most.
     */
    @Test
    fun plantingCalendarMatchesWhenProvincesHarvest() {
        val data = fixture
        assumeTrue("run scripts/build_validation_data.py first", data != null)
        val provinces = data!!.getJSONArray("provinces").let { array ->
            (0 until array.length()).map(array::getJSONObject).mapNotNull { json -> site(json)?.let { json to it } }
        }
        val report = StringBuilder("\nHarvest-season agreement (production-weighted correlation, engine vs PSA quarters):\n")
        val agreement = mutableMapOf<Pair<String, Boolean>, Double>()
        for (irrigated in listOf(false, true)) for (crop in FarmSuitabilityEngine.ECOCROP.keys) {
            val days = SeedRecommendationEngine.crops.first { it.name == crop }.days
            var weighted = 0.0
            var weights = 0.0
            for ((json, site) in provinces) {
                val production = json.getJSONObject("production").optJSONObject(crop) ?: continue
                val quarters = production.getJSONArray("quarterTonnes").doubles()
                if (quarters.sum() < 100.0 || quarters.distinct().size == 1) continue
                val monthly = FarmSuitabilityEngine.monthlyScores(crop, site, irrigated = irrigated)
                // Mean score of the planting months whose harvest falls in each quarter.
                val byQuarter = (0 until 4).map { quarter ->
                    (1..12).filter { plant -> ((plant - 1 + (days / 30.0).roundToInt()) % 12) / 3 == quarter }
                        .map { monthly[it - 1] }.average()
                }
                if (byQuarter.distinct().size == 1) continue
                weighted += pearson(byQuarter, quarters) * quarters.sum()
                weights += quarters.sum()
            }
            if (weights > 0.0) {
                agreement[crop to irrigated] = weighted / weights
                report.append("  %-12s %-9s %+.2f\n".format(crop, if (irrigated) "irrigated" else "rainfed", weighted / weights))
            }
        }
        println(report)
        // Each crop under the water regime Filipino farmers mostly use for it: rain-fed wet-season
        // grains and highland potato; irrigated dry-season vegetables.
        val rainfed = listOf("Rice", "Corn", "Potato")
        val irrigated = listOf("Tomato", "Okra", "Eggplant", "Lettuce")
        for (crop in rainfed) assertTrue("$crop rain-fed season", agreement.getValue(crop to false) > 0.0)
        for (crop in irrigated) assertTrue("$crop irrigated season", agreement.getValue(crop to true) > 0.0)
    }

    private fun pearson(x: List<Double>, y: List<Double>): Double {
        val mx = x.average()
        val my = y.average()
        val cov = x.indices.sumOf { (x[it] - mx) * (y[it] - my) }
        val sx = sqrt(x.sumOf { (it - mx) * (it - mx) })
        val sy = sqrt(y.sumOf { (it - my) * (it - my) })
        return if (sx == 0.0 || sy == 0.0) 0.0 else cov / (sx * sy)
    }

    @Test
    fun ratesTheCropARealFarmGrowsAsSuitable() {
        val data = fixture
        assumeTrue("run scripts/build_validation_data.py first", data != null)
        val farms = data!!.getJSONArray("farms").let { array -> (0 until array.length()).map(array::getJSONObject) }
        val results = farms.mapNotNull { json ->
            val site = site(json) ?: return@mapNotNull null
            val crop = json.getString("crop")
            val labPh = json.getDouble("labPh")
            val soil = SensorReading(temperature = 25.0, humidity = 80.0, soilMoisture = 50.0, soilPh = labPh, timestamp = 1L)
            val score = bestScore(crop, site, soil)
            val rank = FarmSuitabilityEngine.ECOCROP.keys
                .map { it to bestScore(it, site, soil) }
                .sortedByDescending { it.second }
                .indexOfFirst { it.first == crop } + 1
            Triple(crop, score, rank) to json
        }
        val report = StringBuilder("\nSoilsSync farms with climate data: ${results.size}\n")
        for ((crop, group) in results.groupBy { it.first.first }) {
            val suitable = group.count { it.first.second >= 50 }
            val low = group.filter { it.first.second < 50 }
                .joinToString { "${it.second.getString("province")} pH ${it.second.getDouble("labPh")} → ${it.first.second}" }
            report.append("  %-6s %2d/%2d rated suitable, median rank %d of 12%s\n".format(
                crop, suitable, group.size, group.map { it.first.third }.sorted()[group.size / 2],
                if (low.isNotEmpty()) "; low: $low" else ""
            ))
        }
        println(report)
        val share = results.count { it.first.second >= 50 }.toDouble() / results.size
        assertTrue("real farms rated suitable %.2f".format(share), share >= 0.80)
    }
}
