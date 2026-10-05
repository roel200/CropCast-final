package com.cropcast.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropcast.app.data.model.SensorReading
import com.cropcast.app.ui.localization.tr
import com.cropcast.app.ui.theme.CropGreen
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** One month's readings grouped by local day; tap a day to see every reading in it. */
@Composable
fun MonthDetailScreen(monthKey: String, readings: List<SensorReading>, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    // Months are keyed by UTC (see MonthlySensorAggregator), so days and times are UTC too.
    val zone = ZoneOffset.UTC
    val days = remember(readings) {
        readings.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .toSortedMap(compareByDescending { it })
    }
    var openDay by remember(monthKey) { mutableStateOf<LocalDate?>(null) }
    val title = runCatching {
        val month = YearMonth.parse(monthKey)
        "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"
    }.getOrDefault(monthKey)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Back")) }
                Column {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(
                        "${days.size} ${tr("days")} · ${readings.size} ${tr("readings")}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
        item {
            Text(
                "👆 ${tr("Tap a day to see every reading")} · ${tr("times are UTC (Philippines is UTC+8)")}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        items(days.entries.toList(), key = { it.key.toString() }) { (day, dayReadings) ->
            DayCard(day, dayReadings.sortedBy { it.timestamp }, zone, open = openDay == day) {
                openDay = if (openDay == day) null else day
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun DayCard(day: LocalDate, readings: List<SensorReading>, zone: ZoneId, open: Boolean, onClick: () -> Unit) {
    val dayLabel = day.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dayLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${readings.size} ${tr("readings")}", color = CropGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "🌡️ %.1f°C   💧 %.0f%%   🌫️ %.0f%%   🧪 %.1f".format(
                    readings.mean { it.temperature }, readings.mean { it.soilMoisture },
                    readings.mean { it.humidity }, readings.mean { it.soilPh }
                ),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                "N ${readings.mean { it.nitrogen }.toInt()} · P ${readings.mean { it.phosphorus }.toInt()} · " +
                    "K ${readings.mean { it.potassium }.toInt()} mg/kg · ☀️ ${readings.mean { it.lightIntensity }.toInt()} lux",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            if (open) {
                Spacer(Modifier.height(10.dp))
                val hour = DateTimeFormatter.ofPattern("HH:mm")
                readings.forEach { r ->
                    Column(Modifier.padding(vertical = 5.dp)) {
                        Text(
                            Instant.ofEpochMilli(r.timestamp).atZone(zone).format(hour) +
                                "   🌡️ %.1f°C  💧 %.0f%%  🌫️ %.0f%%  🧪 %.1f".format(
                                    r.temperature, r.soilMoisture, r.humidity, r.soilPh
                                ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "      N ${r.nitrogen.toInt()} · P ${r.phosphorus.toInt()} · K ${r.potassium.toInt()} mg/kg · " +
                                "☀️ ${r.lightIntensity.toInt()} lux" +
                                if (r.electricalConductivity > 0) " · EC ${r.electricalConductivity.toInt()} µS/cm" else "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private inline fun List<SensorReading>.mean(field: (SensorReading) -> Double): Double =
    if (isEmpty()) 0.0 else sumOf(field) / size
