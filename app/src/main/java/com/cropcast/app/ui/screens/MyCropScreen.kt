package com.cropcast.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropcast.app.data.CheckLevel
import com.cropcast.app.data.CropCheck
import com.cropcast.app.data.Fertilizer
import com.cropcast.app.data.MyCropAdvice
import com.cropcast.app.data.MyCropAdvisor
import com.cropcast.app.data.model.SensorReading
import com.cropcast.app.ui.components.CropIcon
import com.cropcast.app.ui.components.RoundedCard
import com.cropcast.app.ui.components.StatusPill
import com.cropcast.app.ui.localization.tr
import com.cropcast.app.ui.theme.CropGreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ActColor = Color(0xFFE95D5D)
private val WatchColor = Color(0xFFFF962E)
private val TipColor = Color(0xFF4A90D9)

private fun levelColor(level: CheckLevel, ok: Color, none: Color) = when (level) {
    CheckLevel.ACT -> ActColor
    CheckLevel.WATCH -> WatchColor
    CheckLevel.TIP -> TipColor
    CheckLevel.OK -> ok
    CheckLevel.NONE -> none
}

private fun levelLabel(level: CheckLevel) = when (level) {
    CheckLevel.ACT -> "Act now"
    CheckLevel.WATCH -> "Watch"
    CheckLevel.TIP -> "Tip"
    CheckLevel.OK -> "OK"
    CheckLevel.NONE -> "No range"
}

private val DAY = DateTimeFormatter.ofPattern("MMM d", Locale.US)

private fun number(v: Double) = if (v >= 100) "%.0f".format(v) else "%.1f".format(v).removeSuffix(".0")

private fun range(r: ClosedFloatingPointRange<Double>) = "${number(r.start)}–${number(r.endInclusive)}"

/** The crop in the field (Settings), its growth stage, what to do about it and a fertilizer plan. */
@Composable
fun MyCropScreen(
    cropName: String,
    plantingDate: String,
    reading: SensorReading,
    readingNote: String,
    onOpenSettings: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val planted = remember(plantingDate) { runCatching { LocalDate.parse(plantingDate.trim()) }.getOrNull() }
    val advice = remember(cropName, planted, reading) { MyCropAdvisor.advise(cropName, planted ?: today, today, reading) }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(tr("My Crop"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        if (advice == null) {
            RoundedCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("No crop set"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(tr("Pick the crop growing in your field and its planting date in Settings."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onOpenSettings) { Text(tr("Open Settings")) }
                }
            }
            return@Column
        }
        CropCard(advice, planted != null, onOpenSettings)
        SummaryRow(advice)
        Heading(tr("What to do"))
        if (advice.actions.isEmpty()) Text(tr("Nothing needs doing right now."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        advice.actions.forEach { ActionCard(it.level, it.title, it.why, it.how) }
        if (advice.mix.isNotEmpty()) {
            Heading(tr("Fertilizer plan"))
            FertilizerPlan(advice)
        }
        Heading("${tr("Your readings vs crop needs")}: ${tr(advice.cropName)}")
        ReadingsCard(advice.checks)
        Text(
            readingNote + " " + tr("Ranges: FAO ECOCROP (temperature, pH) and the CropCast crop table (humidity, moisture). N, P and K use the soil-test levels of the fertilizer plan."),
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun CropCard(advice: MyCropAdvice, hasDate: Boolean, onChange: () -> Unit) {
    RoundedCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CropIcon(advice.cropName, advice.icon, 48.dp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr(advice.cropName), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        tr(advice.variety) + " · " + if (hasDate) "${tr("planted")} ${advice.planted.format(DAY)}" else tr("Planting date not set"),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onChange) { Text(tr("Change")) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr(advice.stage), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${tr("Day")} ${advice.day} ${tr("of")} ${advice.totalDays} · ${tr("harvest")} ~${advice.harvest.format(DAY)}",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { advice.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = CropGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryRow(advice: MyCropAdvice) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(CheckLevel.ACT, CheckLevel.WATCH, CheckLevel.OK).forEach { level ->
            val color = levelColor(level, CropGreen, Color.Gray)
            Column(
                Modifier.weight(1f).background(color.copy(alpha = .14f), RoundedCornerShape(12.dp)).padding(10.dp)
            ) {
                Text("${advice.count(level)}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
                Text(tr(levelLabel(level)), fontSize = 12.sp, color = color)
            }
        }
    }
}

@Composable
private fun ActionCard(level: CheckLevel, title: String, why: String, how: String) {
    val color = levelColor(level, CropGreen, Color.Gray)
    RoundedCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(tr(levelLabel(level)), color)
                Text(tr(title), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            }
            Text(tr(why), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tr(how), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun FertilizerPlan(advice: MyCropAdvice) {
    var common by remember { mutableStateOf(true) }
    val items = if (common) advice.mix else advice.single
    RoundedCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                when {
                    advice.nearHarvest -> tr("Harvest is close: skip fertilizer now. This plan is for the next planting.")
                    advice.lowNutrients.isNotEmpty() -> tr("Low") + ": " + advice.lowNutrients.map { tr(it) }.joinToString() + ". " + tr("Buy one of these sets:")
                    else -> tr("Nothing is low. These are normal maintenance doses.")
                },
                fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (common) Button(onClick = {}, modifier = Modifier.weight(1f)) { Text(tr("Common mix"), fontSize = 13.sp) }
                else OutlinedButton(onClick = { common = true }, modifier = Modifier.weight(1f)) { Text(tr("Common mix"), fontSize = 13.sp) }
                if (!common) Button(onClick = {}, modifier = Modifier.weight(1f)) { Text(tr("Single fertilizers"), fontSize = 13.sp) }
                else OutlinedButton(onClick = { common = false }, modifier = Modifier.weight(1f)) { Text(tr("Single fertilizers"), fontSize = 13.sp) }
            }
            Text(
                tr(if (common) "Fewest sacks. Complete and Urea are the fertilizers most farmers already buy." else "One fertilizer per nutrient. Exact doses, but more sacks to buy."),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            items.forEach { FertilizerRow(it) }
            if (advice.phHigh) Text(tr("Soil pH is high, so the nitrogen is Ammosul (21-0-0). It lowers pH a little. Urea (46-0-0) also works if Ammosul is not in stock."), fontSize = 12.sp, color = TipColor)
            Text(tr("Have chicken manure or vermicast? Mix it in too. It feeds the soil, but it does not replace the doses above."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tr("Amounts are per 1,000 m² (10 × 100 m). 1 sack = 50 kg."), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FertilizerRow(f: Fertilizer) {
    val gives = listOf("N", "P", "K").zip(f.gives).filter { it.second > 0.0 }.joinToString(" · ") { "${it.first} ${number(it.second)} kg" }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(72.dp).background(Color(f.color), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Text(f.grade, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr(f.name), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("${number(f.kgPer1000m2)} kg", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("${tr("Ask for")}: ${f.alias} · ${tr(f.sacks)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${tr("Gives")} $gives", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tr(f.whenText), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ReadingsCard(checks: List<CropCheck>) {
    RoundedCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            checks.forEach { CheckRow(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Legend(CropGreen.copy(alpha = .55f), tr("Best"))
                Legend(WatchColor.copy(alpha = .35f), tr("Tolerates"))
                Legend(MaterialTheme.colorScheme.onSurface, tr("Your farm"))
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(width = 12.dp, height = 8.dp).background(color, RoundedCornerShape(4.dp)))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CheckRow(check: CropCheck) {
    val color = levelColor(check.level, CropGreen, MaterialTheme.colorScheme.onSurfaceVariant)
    val unit = if (check.unit.isEmpty()) "" else " ${check.unit}"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr(check.name), Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(number(check.value) + unit, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            StatusPill(tr(levelLabel(check.level)), color)
        }
        check.tolerates?.let { tol -> check.best?.let { RangeBar(check.axis, tol, it, check.value) } }
        Text(
            when {
                check.note == "Field should be drained now" -> tr(check.note)
                check.best == null -> tr("No range for this crop yet")
                check.note != null -> "${tr(check.note)} (${tr("soil test level")} ${range(check.best)})"
                check.tolerates == null -> "${tr("Best")} ${range(check.best)}$unit"
                else -> "${tr("Best")} ${range(check.best)}$unit, ${tr("Tolerates").lowercase()} ${range(check.tolerates)}$unit"
            },
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RangeBar(axis: ClosedFloatingPointRange<Double>, tolerates: ClosedFloatingPointRange<Double>, best: ClosedFloatingPointRange<Double>, value: Double) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val mark = MaterialTheme.colorScheme.onSurface
    fun at(v: Double, width: Float) = (((v - axis.start) / (axis.endInclusive - axis.start)).toFloat().coerceIn(0f, 1f)) * width
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        val top = 3.dp.toPx()
        val h = 10.dp.toPx()
        val corner = CornerRadius(h / 2)
        drawRoundRect(track, Offset(0f, top), Size(size.width, h), corner)
        drawRoundRect(WatchColor.copy(alpha = .35f), Offset(at(tolerates.start, size.width), top), Size(at(tolerates.endInclusive, size.width) - at(tolerates.start, size.width), h), corner)
        drawRoundRect(CropGreen.copy(alpha = .55f), Offset(at(best.start, size.width), top), Size(at(best.endInclusive, size.width) - at(best.start, size.width), h), corner)
        val x = at(value, size.width)
        drawRoundRect(mark, Offset(x - 2.dp.toPx(), 0f), Size(4.dp.toPx(), size.height), CornerRadius(2.dp.toPx()))
    }
}
