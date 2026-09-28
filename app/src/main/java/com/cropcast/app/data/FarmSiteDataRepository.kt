package com.cropcast.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** 20-year monthly normals, January first. Rainfall is the monthly total in mm. */
data class ClimateNormals(
    val temperatureC: List<Double>,
    val humidityPct: List<Double>,
    val rainfallMm: List<Double>,
    /** Mean height of the NASA grid cell the normals describe. */
    val cellElevationM: Double? = null,
    /** Months (1-12) replaced by the ECMWF seasonal forecast. */
    val forecastMonths: Set<Int> = emptySet()
) {
    val annualRainfallMm: Double get() = rainfallMm.sum()
}

/** The next days of weather forecast, summarized. */
data class WeatherOutlook(
    val meanTemperatureC: Double,
    val meanHumidityPct: Double,
    val rainfallMm: Double,
    val days: Int,
    /** Height of the farm itself, from Open-Meteo's 90 m terrain model. */
    val elevationM: Double? = null,
    /** Day by day, same order: ISO dates, rain (mm) and maximum wind gust (km/h). */
    val dailyDates: List<String> = emptyList(),
    val dailyRainMm: List<Double> = emptyList(),
    val dailyGustKmh: List<Double> = emptyList()
)

/** ECMWF SEAS5 seasonal forecast months, plus the farm elevation the API reports. */
data class SeasonalForecast(val months: List<SeasonalMonth>, val elevationM: Double?)

/** One month of the ECMWF SEAS5 seasonal forecast (ensemble mean). */
data class SeasonalMonth(
    val year: Int,
    val month: Int,
    val rainfallMm: Double,
    /** Forecast minus ECMWF's own long-term average for that month. */
    val rainfallAnomalyMm: Double,
    val temperatureAnomalyC: Double
)

/** Mapped topsoil (0-30 cm) at the farm location. Any field can be missing. */
data class SoilMap(
    val ph: Double?,
    val clayPct: Double?,
    val sandPct: Double?,
    val nitrogenGPerKg: Double?
)

enum class SourceStatus { LIVE, CACHED, UNAVAILABLE }

data class FarmSiteData(
    val normals: ClimateNormals? = null,
    val outlook: WeatherOutlook? = null,
    val soil: SoilMap? = null,
    val seasonal: List<SeasonalMonth>? = null,
    val status: Map<String, SourceStatus> = emptyMap(),
    /** Farm height when known without a forecast (validation fixtures). */
    val elevationM: Double? = null
)

/**
 * Loads free, keyless location data for the farm. Every successful response is
 * cached on the phone, so a later request without internet reuses the last
 * good copy instead of failing.
 */
class FarmSiteDataRepository(private val cacheDir: File) {

    /**
     * Emits the site data again as each source arrives, so a slow source (SoilGrids
     * can take a minute on a cold request) never holds back the others.
     */
    fun load(latitude: Double, longitude: Double): Flow<FarmSiteData> = channelFlow {
        val lat = String.format(Locale.US, "%.3f", latitude)
        val lon = String.format(Locale.US, "%.3f", longitude)
        val location = "$lat,$lon"
        var data = FarmSiteData()
        val lock = Mutex()
        suspend fun publish(update: (FarmSiteData) -> FarmSiteData) = lock.withLock {
            data = update(data)
            send(data)
        }

        launch(Dispatchers.IO) {
            val (status, normals) = fetch(NASA_POWER, location, ::parseNasaPowerClimatology) {
                "https://power.larc.nasa.gov/api/temporal/climatology/point" +
                    "?parameters=T2M,RH2M,PRECTOTCORR&community=AG&format=JSON&latitude=$lat&longitude=$lon"
            }
            publish { it.copy(normals = normals, status = it.status + (NASA_POWER to status)) }
        }
        launch(Dispatchers.IO) {
            val (status, outlook) = fetch(OPEN_METEO_FORECAST, location, ::parseOpenMeteoForecast) {
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&daily=temperature_2m_mean,relative_humidity_2m_mean,precipitation_sum,wind_gusts_10m_max" +
                    "&forecast_days=16&timezone=auto"
            }
            publish { it.copy(outlook = outlook, status = it.status + (OPEN_METEO_FORECAST to status)) }
        }
        launch(Dispatchers.IO) {
            val (status, soil) = fetch(SOILGRIDS, location, ::parseSoilGrids, timeoutMs = 60_000) {
                "https://rest.isric.org/soilgrids/v2.0/properties/query?lat=$lat&lon=$lon" +
                    "&property=phh2o&property=clay&property=sand&property=nitrogen" +
                    "&depth=0-5cm&depth=5-15cm&depth=15-30cm&value=mean"
            }
            publish { it.copy(soil = soil, status = it.status + (SOILGRIDS to status)) }
        }
        launch(Dispatchers.IO) {
            val (status, seasonal) = fetch(ECMWF_SEASONAL, location, ::parseSeasonalForecast) {
                "https://seasonal-api.open-meteo.com/v1/seasonal?latitude=$lat&longitude=$lon" +
                    "&monthly=precipitation_mean,precipitation_anomaly,temperature_2m_anomaly"
            }
            // Its elevation keeps the highland temperature correction working when the
            // 16-day forecast (the other elevation source) is unavailable.
            publish {
                it.copy(
                    seasonal = seasonal?.months,
                    elevationM = it.elevationM ?: seasonal?.elevationM,
                    status = it.status + (ECMWF_SEASONAL to status)
                )
            }
        }
    }

    internal fun <T> fetch(
        source: String,
        location: String,
        parse: (String) -> T,
        timeoutMs: Int = 30_000,
        url: () -> String
    ): Pair<SourceStatus, T?> {
        val cacheFile = File(cacheDir, "site_${source.filter(Char::isLetter)}_${location.replace(',', '_')}.json")
        runCatching {
            val body = httpGetText(url(), readTimeoutMs = timeoutMs)
            val parsed = parse(body)  // only cache a body that parses
            cacheFile.writeText(body)
            return SourceStatus.LIVE to parsed
        }
        val cached = runCatching { parse(cacheFile.readText()) }.getOrNull()
        return if (cached != null) SourceStatus.CACHED to cached else SourceStatus.UNAVAILABLE to null
    }

    companion object {
        const val NASA_POWER = "NASA POWER climate normals"
        const val OPEN_METEO_FORECAST = "Open-Meteo 16-day forecast"
        const val SOILGRIDS = "ISRIC SoilGrids soil map"
        const val ECMWF_SEASONAL = "ECMWF seasonal forecast"
        val SOURCES = listOf(NASA_POWER, OPEN_METEO_FORECAST, SOILGRIDS, ECMWF_SEASONAL)

        private val MONTHS = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        private val DAYS_IN_MONTH = listOf(31.0, 28.25, 31.0, 30.0, 31.0, 30.0, 31.0, 31.0, 30.0, 31.0, 30.0, 31.0)

        internal fun parseNasaPowerClimatology(body: String): ClimateNormals {
            val root = JSONObject(body)
            val parameters = root.getJSONObject("properties").getJSONObject("parameter")
            fun monthly(name: String): List<Double> {
                val values = parameters.getJSONObject(name)
                return MONTHS.map { month ->
                    values.getDouble(month).also { check(it > -900.0) { "NASA POWER returned a fill value for $name" } }
                }
            }
            return ClimateNormals(
                temperatureC = monthly("T2M"),
                humidityPct = monthly("RH2M"),
                // PRECTOTCORR is mm/day; convert to a monthly total.
                rainfallMm = monthly("PRECTOTCORR").mapIndexed { index, perDay -> perDay * DAYS_IN_MONTH[index] },
                cellElevationM = root.optJSONObject("geometry")?.optJSONArray("coordinates")
                    ?.takeIf { it.length() >= 3 }?.getDouble(2)
            )
        }

        internal fun parseOpenMeteoForecast(body: String): WeatherOutlook {
            val root = JSONObject(body)
            val daily = root.getJSONObject("daily")
            fun values(name: String): List<Double> {
                val array = daily.optJSONArray(name) ?: return emptyList()
                return (0 until array.length()).filterNot(array::isNull).map(array::getDouble)
            }
            val temperature = values("temperature_2m_mean")
            val humidity = values("relative_humidity_2m_mean")
            val rain = values("precipitation_sum")
            check(temperature.isNotEmpty() && humidity.isNotEmpty() && rain.isNotEmpty()) {
                "Forecast response had no usable daily values"
            }
            // Keep the day-by-day lists aligned: only days that have both a date and rain.
            val dates = daily.optJSONArray("time")
            val rainArray = daily.getJSONArray("precipitation_sum")
            val gustArray = daily.optJSONArray("wind_gusts_10m_max")
            val days = (0 until rainArray.length()).filter { !rainArray.isNull(it) && dates != null && it < dates.length() }
            return WeatherOutlook(
                meanTemperatureC = temperature.average(),
                meanHumidityPct = humidity.average(),
                rainfallMm = rain.sum(),
                days = rain.size,
                elevationM = root.optDouble("elevation").takeIf { it.isFinite() },
                dailyDates = days.map { dates!!.getString(it) },
                dailyRainMm = days.map(rainArray::getDouble),
                dailyGustKmh = if (gustArray == null) emptyList()
                else days.map { if (it < gustArray.length() && !gustArray.isNull(it)) gustArray.getDouble(it) else 0.0 }
            )
        }

        internal fun parseSeasonalForecast(body: String): SeasonalForecast {
            val root = JSONObject(body)
            val monthly = root.getJSONObject("monthly")
            val time = monthly.getJSONArray("time")
            val rain = monthly.getJSONArray("precipitation_mean")
            val rainAnomaly = monthly.getJSONArray("precipitation_anomaly")
            val temperatureAnomaly = monthly.getJSONArray("temperature_2m_anomaly")
            val months = (0 until time.length())
                .filter { !rain.isNull(it) && !rainAnomaly.isNull(it) && !temperatureAnomaly.isNull(it) }
                .map { i ->
                    val (year, month) = time.getString(i).split("-").map(String::toInt)
                    SeasonalMonth(year, month, rain.getDouble(i), rainAnomaly.getDouble(i), temperatureAnomaly.getDouble(i))
                }
            check(months.isNotEmpty()) { "Seasonal forecast had no usable months" }
            return SeasonalForecast(months, root.optDouble("elevation").takeIf { it.isFinite() })
        }

        /** Thickness-weighted 0-30 cm average; SoilGrids returns null over water and cities. */
        internal fun parseSoilGrids(body: String): SoilMap {
            val layers = JSONObject(body).getJSONObject("properties").getJSONArray("layers")
            val byName = (0 until layers.length()).map(layers::getJSONObject).associateBy { it.getString("name") }
            fun topsoil(name: String): Double? {
                val layer = byName[name] ?: return null
                val factor = layer.getJSONObject("unit_measure").getDouble("d_factor")
                val depths = layer.getJSONArray("depths")
                var weighted = 0.0
                var thickness = 0.0
                for (index in 0 until depths.length()) {
                    val depth = depths.getJSONObject(index)
                    val values = depth.getJSONObject("values")
                    if (values.isNull("mean")) continue
                    val range = depth.getJSONObject("range")
                    val cm = range.getDouble("bottom_depth") - range.getDouble("top_depth")
                    weighted += values.getDouble("mean") / factor * cm
                    thickness += cm
                }
                return if (thickness > 0.0) weighted / thickness else null
            }
            val soil = SoilMap(
                ph = topsoil("phh2o"),
                clayPct = topsoil("clay"),  // d_factor already converts g/kg to %
                sandPct = topsoil("sand"),
                nitrogenGPerKg = topsoil("nitrogen")
            )
            check(soil.ph != null || soil.clayPct != null) { "SoilGrids has no data for this location" }
            return soil
        }
    }
}
