package com.cropcast.app.data

import org.json.JSONObject

/**
 * BSWM soil series polygons for Panay (see scripts/build_iloilo_soil.py). Gives the
 * texture of the mapped soil and, for series in the Iloilo survey, its pH. It only
 * fills what SoilGrids leaves empty.
 */
class SoilSurvey(private val areas: List<Area>) {

    class Area(val name: String, val ph: Double?, val clayPct: Double?, val sandPct: Double?, val rings: List<DoubleArray>)

    /** The soil mapped at this point, or null outside the survey window. */
    fun lookup(latitude: Double, longitude: Double): SoilMap? {
        val area = areas.firstOrNull { contains(it.rings, longitude, latitude) } ?: return null
        return SoilMap(
            ph = area.ph, clayPct = area.clayPct, sandPct = area.sandPct, nitrogenGPerKg = null,
            phSource = SOURCE, textureSource = SOURCE
        )
    }

    companion object {
        const val SOURCE = "BSWM soil survey map"

        /** Even-odd rule over all rings, so holes and separate pieces both work. Rings are lon,lat pairs. */
        internal fun contains(rings: List<DoubleArray>, x: Double, y: Double): Boolean {
            var inside = false
            for (ring in rings) {
                var j = ring.size - 2
                for (i in 0 until ring.size step 2) {
                    val xi = ring[i]; val yi = ring[i + 1]; val xj = ring[j]; val yj = ring[j + 1]
                    if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
                    j = i
                }
            }
            return inside
        }

        fun parse(json: String): SoilSurvey {
            val soils = JSONObject(json).getJSONArray("soils")
            return SoilSurvey((0 until soils.length()).map { index ->
                val soil = soils.getJSONObject(index)
                val rings = soil.getJSONArray("rings")
                Area(
                    name = soil.getString("name"),
                    ph = soil.optDouble("ph").takeIf { it.isFinite() },
                    clayPct = soil.optDouble("clay").takeIf { it.isFinite() },
                    sandPct = soil.optDouble("sand").takeIf { it.isFinite() },
                    rings = (0 until rings.length()).map { r ->
                        val ring = rings.getJSONArray(r)
                        DoubleArray(ring.length()) { ring.getDouble(it) }
                    }
                )
            })
        }

        /** SoilGrids values win; the survey fills only the fields SoilGrids left empty. */
        internal fun merge(soilGrids: SoilMap?, survey: SoilMap?): SoilMap? {
            if (soilGrids == null || survey == null) return soilGrids ?: survey
            val hasTexture = soilGrids.clayPct != null && soilGrids.sandPct != null
            val useSurveyPh = soilGrids.ph == null && survey.ph != null
            val useSurveyTexture = !hasTexture && survey.clayPct != null
            return soilGrids.copy(
                ph = if (useSurveyPh) survey.ph else soilGrids.ph,
                phSource = if (useSurveyPh) survey.phSource else soilGrids.phSource,
                clayPct = if (useSurveyTexture) survey.clayPct else soilGrids.clayPct,
                sandPct = if (useSurveyTexture) survey.sandPct else soilGrids.sandPct,
                textureSource = if (useSurveyTexture) survey.textureSource else soilGrids.textureSource
            )
        }
    }
}
