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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropcast.app.R
import com.cropcast.app.data.CropSuitability
import com.cropcast.app.data.FarmRecommendation
import com.cropcast.app.data.NutrientStatus
import com.cropcast.app.data.model.SeedRecommendation
import com.cropcast.app.ui.components.RoundedCard
import com.cropcast.app.ui.localization.tr
import com.cropcast.app.ui.theme.CropGreen

private val WarningColor = Color(0xFFE95D5D)
private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun suitabilityColor(score: Int): Color = when {
    score >= 80 -> CropGreen
    score >= 60 -> Color(0xFFFFBD16)
    score >= 40 -> Color(0xFF7D68EE)
    else -> WarningColor
}

@Composable
private fun CropIcon(crop: SeedRecommendation, size: Int) {
    Box(
        Modifier.size(size.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (crop.name.equals("Alugbati", ignoreCase = true)) {
            Image(
                painter = painterResource(R.drawable.crop_alugbati),
                contentDescription = tr("Alugbati crop"),
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(crop.icon, fontSize = (size / 2).sp)
        }
    }
}

@Composable
private fun CropScoreRow(rank: Int, suitability: CropSuitability) {
    val color = suitabilityColor(suitability.score)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CropIcon(suitability.crop, size = if (rank == 1) 52 else 38)
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "$rank. ${suitability.crop.name} (${suitability.crop.variety})",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (rank == 1) 16.sp else 14.sp
                )
                val limit = suitability.limitingFactor?.takeIf { it.score < 100 }
                if (limit != null) {
                    Text(
                        "${tr("Main limit")}: ${tr(limit.name)} · ${limit.detail}",
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
            Text(
                "${tr("Suitability score")} · ${tr("planting this month")}",
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .70f),
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
                "🔎 ${tr("Why")} ${top.crop.name}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            if (top.bestPlantingMonths.isNotEmpty() && top.bestPlantingMonths.size < 12) {
                Text(
                    "📅 ${tr("Best months to plant")}: ${top.bestPlantingMonths.joinToString { MONTH_NAMES[it - 1] }}",
                    color = CropGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
            for (factor in top.factors) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr(factor.name), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(factor.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(factor.source, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f), fontSize = 10.sp)
                    }
                    Text("${factor.score}%", color = suitabilityColor(factor.score), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun NutrientPlanCard(recommendation: FarmRecommendation) {
    val top = recommendation.top ?: return
    if (recommendation.nutrientPlan.isEmpty()) return
    RoundedCard(color = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "🧪 ${tr("Soil and fertilizer plan for")} ${top.crop.name}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
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
                            // pH needs a decimal; N/P/K do not.
                            (if (advice.nutrient == "Soil pH") "%s: %.1f (%s %.1f–%.1f)" else "%s: %.0f (%s %.0f–%.0f)")
                                .format(tr(advice.nutrient), advice.measured, tr("target"), advice.targetLow, advice.targetHigh),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(advice.action, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Text(
                tr("N, P and K do not change the crop ranking because fertilizer can correct them. Confirm exact rates with a soil laboratory test."),
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
                    Text("• $warning", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
            Text(
                "${tr("Data used")}: ${recommendation.sources.joinToString(" · ")}",
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
                        "${tr("Also suitable")}: ${recommendation.ranked.drop(1).take(2).joinToString { "${it.crop.name} ${it.score}%" }}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                recommendation.warnings.firstOrNull()?.let {
                    Text("⚠️ $it", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Button(onClick = onOpenRecommendations, modifier = Modifier.fillMaxWidth()) {
                Text(tr("View full recommendation"))
            }
        }
    }
}
