package com.cropcast.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropcast.app.BuildConfig
import com.cropcast.app.R
import com.cropcast.app.data.CropSuitability
import com.cropcast.app.data.FarmAdviceRepository
import com.cropcast.app.data.FarmRecommendation
import com.cropcast.app.data.NutrientStatus
import com.cropcast.app.data.model.SeedRecommendation
import com.cropcast.app.ui.components.CropIcon
import com.cropcast.app.ui.components.RoundedCard
import com.cropcast.app.ui.localization.LocalCropCastLanguage
import com.cropcast.app.ui.localization.tr
import com.cropcast.app.ui.theme.CropGreen
import kotlinx.coroutines.launch

private val WarningColor = Color(0xFFE95D5D)
/** 677900.0 -> "678k"; 85200.0 -> "85k". */
private fun formatPesos(amount: Double): String =
    if (amount >= 1_000_000) "%.1fM".format(amount / 1_000_000) else "%.0fk".format(amount / 1_000)

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun suitabilityColor(score: Int): Color = when {
    score >= 80 -> CropGreen
    score >= 60 -> Color(0xFFFFBD16)
    score >= 40 -> Color(0xFF7D68EE)
    else -> WarningColor
}

@Composable
private fun CropScoreRow(rank: Int, suitability: CropSuitability) {
    val color = suitabilityColor(suitability.score)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CropIcon(suitability.crop.name, suitability.crop.icon, if (rank == 1) 52.dp else 38.dp)
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "$rank. ${tr(suitability.crop.name)} (${tr(suitability.crop.variety)})",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (rank == 1) 16.sp else 14.sp
                )
                Text(
                    "${suitability.suitabilityClass.code} · ${tr(suitability.suitabilityClass.label)}" +
                        (suitability.economics?.grossPhpPerHa?.let { " · ≈ ₱${formatPesos(it)}/ha ${tr("gross")}" } ?: ""),
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
                val limit = suitability.limitingFactor?.takeIf { it.score < 100 }
                if (limit != null) {
                    Text(
                        "${tr("Main limit")}: ${tr(limit.name)} · ${tr(limit.detail)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                } else {
                    Text(tr("All checked conditions are in the optimal range"), color = CropGreen, fontSize = 11.sp)
                }
            }
            Text("${suitability.score}%", color = color, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        LinearProgressIndicator(
            progress = { suitability.score / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/** Main card on the Crop Recommendation screen. */
@Composable
fun FarmRecommendationCard(recommendation: FarmRecommendation?, loadingSiteData: Boolean) {
    RoundedCard(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "🌾 ${tr("Best crops for your farm")}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
            if (loadingSiteData) {
                Text(
                    tr("Loading climate and soil maps for your farm…"),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .78f),
                    fontSize = 11.sp
                )
            }
            if (recommendation == null || recommendation.ranked.isEmpty()) {
                Text(
                    tr("Waiting for soil readings or farm location data"),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 13.sp
                )
                return@Column
            }
            recommendation.ranked.take(3).forEachIndexed { index, suitability ->
                CropScoreRow(index + 1, suitability)
            }
            recommendation.bestValue?.takeIf { it.crop.name != recommendation.top?.crop?.name }?.let { value ->
                Text(
                    "💰 ${tr("Best income among suitable crops")}: ${tr(value.crop.name)} " +
                        "(${value.suitabilityClass.code}, ≈ ₱${formatPesos(value.economics!!.grossPhpPerHa!!)}/ha ${tr("gross")})",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
            Text(
                "${tr("FAO suitability classes")} S1–S3/N · ${tr("planting this month")} · " +
                    tr("income: PSA national yield × farmgate price, before costs"),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .70f),
                fontSize = 10.sp
            )
        }
    }
}

/** LLM-written explanation of the ranking above. The rule-based cards stay on screen if the call fails. */
@Composable
fun FarmAiAdviceCard(recommendation: FarmRecommendation) {
    if (BuildConfig.ADVICE_URL.isBlank()) return
    val language = LocalCropCastLanguage.current
    val scope = rememberCoroutineScope()
    val repository = remember { FarmAdviceRepository(BuildConfig.ADVICE_URL) }
    var loading by remember { mutableStateOf(false) }
    var advice by remember(recommendation, language) { mutableStateOf<String?>(null) }
    var failed by remember(recommendation, language) { mutableStateOf(false) }
    RoundedCard(color = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "🤖 ${tr("AI explanation")}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            advice?.let { Text(it, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp) }
            if (failed) {
                Text(
                    tr("AI advice is unavailable right now. The ranking and plan above still apply."),
                    color = WarningColor,
                    fontSize = 11.sp
                )
            }
            Button(
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    loading = true
                    failed = false
                    scope.launch {
                        runCatching { repository.explain(recommendation, language) }
                            .onSuccess { advice = it }
                            .onFailure { failed = true }
                        loading = false
                    }
                }
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(tr(if (advice == null) "Explain with AI" else "Explain again"))
            }
            Text(
                tr("Written by an AI from the numbers above. It can make mistakes; confirm with a BSWM soil test or your local agriculturist."),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .75f),
                fontSize = 10.sp
            )
        }
    }
}

/** Why the top crop scored what it did, factor by factor. */
@Composable
fun FarmFactorsCard(recommendation: FarmRecommendation) {
    val top = recommendation.top ?: return
    RoundedCard(color = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "🔎 ${tr("Why")} ${tr(top.crop.name)}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            if (top.bestPlantingMonths.isNotEmpty() && top.bestPlantingMonths.size < 12) {
                Text(
                    "📅 ${tr("Best months to plant")}: ${top.bestPlantingMonths.map { tr(MONTH_NAMES[it - 1]) }.joinToString()}",
                    color = CropGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
            for (risk in top.risks) {
                Text("⚠️ ${tr(risk)}", color = WarningColor, fontSize = 11.sp)
            }
            for (factor in top.factors) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr(factor.name), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(tr(factor.detail), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(tr(factor.source), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f), fontSize = 10.sp)
                    }
                    Text("${factor.score}%", color = suitabilityColor(factor.score), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun NutrientPlanCard(recommendation: FarmRecommendation) {
    val cropName = recommendation.nutrientCrop ?: return
    if (recommendation.nutrientPlan.isEmpty()) return
    RoundedCard(color = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "🧪 ${tr("Soil and fertilizer plan for")} ${tr(cropName)}" +
                    if (recommendation.growing) " (${tr("growing now")})" else "",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            Text(
                tr("What to buy and how much to use. Amounts are for every 1,000 m² of field, for example a 25 m × 40 m plot."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            for (advice in recommendation.nutrientPlan) {
                val (label, color) = when (advice.status) {
                    NutrientStatus.LOW -> tr("Low") to WarningColor
                    NutrientStatus.HIGH -> tr("High") to Color(0xFFFFBD16)
                    NutrientStatus.OK -> tr("OK") to CropGreen
                }
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "${tr(advice.nutrient)}: ${tr(advice.detail)}",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(tr(advice.action), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Text(
                tr("Can't find these? Ask the agri-supply store for the same numbers (like 46-0-0) or a complete fertilizer such as 14-14-14. N, P and K do not change the crop ranking because fertilizer can correct them. Confirm with a BSWM soil test or your municipal agriculturist."),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .75f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun FarmNoticesCard(recommendation: FarmRecommendation) {
    RoundedCard(color = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (recommendation.warnings.isNotEmpty()) {
                Text(
                    "⚠️ ${tr("Before planting")}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                for (warning in recommendation.warnings) {
                    Text("• ${tr(warning)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
            Text(
                "${tr("Data used")}: ${recommendation.sources.map { tr(it) }.joinToString(" · ")}",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .75f),
                fontSize = 10.sp
            )
        }
    }
}

/** Compact version for the Home screen. */
@Composable
fun FarmPreviewCard(recommendation: FarmRecommendation?, loadingSiteData: Boolean, onOpenRecommendations: () -> Unit) {
    val top = recommendation?.top
    RoundedCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (recommendation == null || top == null) {
                Text(
                    tr(if (loadingSiteData) "Loading climate and soil maps for your farm…" else "Waiting for soil readings or farm location data"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                CropScoreRow(1, top)
                if (recommendation.ranked.size > 1) {
                    Text(
                        "${tr("Also suitable")}: ${recommendation.ranked.drop(1).take(2).map { "${tr(it.crop.name)} ${it.score}%" }.joinToString()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                recommendation.warnings.firstOrNull()?.let {
                    Text("⚠️ ${tr(it)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Button(onClick = onOpenRecommendations, modifier = Modifier.fillMaxWidth()) {
                Text(tr("View full recommendation"))
            }
        }
    }
}
