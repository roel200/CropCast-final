package com.cropcast.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Plain-language explanation of a [FarmRecommendation], written by an LLM behind the CropCast Worker. */
class FarmAdviceRepository(private val endpoint: String) {

    suspend fun explain(recommendation: FarmRecommendation, language: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("facts", buildFacts(recommendation)).put("language", language).toString()
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 45_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(payload.toByteArray()) }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(code in 200..299) { "Advice service returned HTTP $code" }
            JSONObject(body).getString("text")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** Only crop, soil, season and price facts: no account, coordinates or place names leave the phone. */
        internal fun buildFacts(recommendation: FarmRecommendation): String = buildString {
            appendLine("Ranked crops for this farm this month (FAO suitability class, score 0-100):")
            recommendation.ranked.take(3).forEachIndexed { index, crop ->
                append("${index + 1}. ${crop.crop.name}: ${crop.suitabilityClass.code} ${crop.suitabilityClass.label}, ${crop.score}")
                crop.limitingFactor?.takeIf { it.score < 100 }?.let { append("; main limit ${it.name}: ${it.detail}") }
                crop.economics?.grossPhpPerHa?.let { append("; PSA gross income about PHP ${it.toLong()}/ha") }
                appendLine()
            }
            val top = recommendation.top ?: return@buildString
            if (top.bestPlantingMonths.isNotEmpty() && top.bestPlantingMonths.size < 12) {
                appendLine("Best planting months for ${top.crop.name}: ${top.bestPlantingMonths.joinToString()} (1 = January)")
            }
            top.risks.forEach { appendLine("Season risk: $it") }
            recommendation.nutrientCrop?.let {
                appendLine("Fertilizer plan below is for ${if (recommendation.growing) "the crop growing now" else "the top crop, not yet planted"}: $it")
            }
            recommendation.nutrientPlan.forEach { appendLine("Soil ${it.nutrient}: ${it.status} - ${it.detail}. ${it.action}") }
            recommendation.warnings.forEach { appendLine("Warning: $it") }
        }.trim()
    }
}
