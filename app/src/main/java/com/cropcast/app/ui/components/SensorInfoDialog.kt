package com.cropcast.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropcast.app.ui.localization.tr
import com.cropcast.app.ui.theme.CropGreen

/** What one sensor reading means; the strings are English keys that tr() translates. */
enum class SensorInfo(
    val icon: String,
    val title: String,
    val meaning: String,
    val purpose: String,
    val goodRange: String,
    val tooLow: String,
    val tooHigh: String
) {
    SOIL_MOISTURE(
        "💧", "Soil Moisture",
        "How much water is in the soil around the probe, shown as a percentage.",
        "Roots drink water from the soil. This tells you if your crop needs watering now or if the soil is too wet.",
        "Most vegetables grow best at 50–70%. Rice likes wet, flooded soil.",
        "Below 40%: the soil is dry and plants wilt. Water the field.",
        "Above 80%: the soil is waterlogged. Roots can rot, so improve drainage."
    ),
    TEMPERATURE(
        "🌡️", "Temperature",
        "How hot or cool the air is around the sensor, in degrees Celsius (°C).",
        "Each crop grows in a certain temperature. Too hot or too cold slows growth and flowering.",
        "Most crops here grow best at 20–30 °C.",
        "Below 15 °C: growth slows down.",
        "Above 35 °C: flowers drop and plants get stressed. Give shade and water."
    ),
    HUMIDITY(
        "🌫️", "Humidity",
        "How much water vapor is in the air, shown as a percentage.",
        "Damp air helps fungus and leaf diseases spread. Very dry air makes plants lose water fast.",
        "50–80% suits most crops.",
        "Below 40%: plants lose water fast. Water more often.",
        "Above 85%: fungal diseases like blight and mildew spread. Keep good airflow."
    ),
    SOIL_PH(
        "🧪", "Soil pH",
        "How acidic or alkaline the soil is, on a scale of 0 to 14. 7 is neutral.",
        "pH decides whether plants can take up nutrients. A wrong pH locks nutrients in the soil even when you add fertilizer.",
        "Most crops grow best at pH 5.5–7.0.",
        "Below 5.5: the soil is too acidic. Apply agricultural lime (dolomite).",
        "Above 7.5: the soil is too alkaline. Mix in compost or use ammonium sulfate."
    ),
    NITROGEN(
        "🧬", "Nitrogen",
        "N, a nutrient in the soil that makes leaves and stems grow, measured in mg/kg.",
        "Nitrogen keeps plants green and leafy.",
        "About 125–250 mg/kg is a good level.",
        "Below 125: pale yellow leaves and slow growth. Add urea (46-0-0).",
        "Above 250: too many leaves, weak fruit and more pests."
    ),
    PHOSPHORUS(
        "🔬", "Phosphorus",
        "P, a nutrient that helps roots, flowers and fruit, measured in mg/kg.",
        "Phosphorus builds strong roots and helps plants flower and set fruit.",
        "About 10–25 mg/kg is a good level.",
        "Below 10: purple-tinted leaves and weak roots. Add Solophos (0-18-0) before planting.",
        "Above 25: more than the plant needs. Skip phosphorus fertilizer this season."
    ),
    POTASSIUM(
        "⚡", "Potassium",
        "K, a nutrient that helps fruit fill out and keeps plants healthy, measured in mg/kg.",
        "Potassium makes strong stems, sweet fruit and better resistance to disease and drought.",
        "About 78–156 mg/kg is a good level.",
        "Below 78: weak stems, yellow leaf edges and poor fruit. Add muriate of potash (0-0-60).",
        "Above 156: already enough. Extra potassium can block calcium and magnesium."
    ),
    LIGHT(
        "☀️", "Light Intensity",
        "How bright the light is at the sensor, measured in lux.",
        "Plants use light to make food (photosynthesis). It also shows if your crop is shaded.",
        "Open sun is about 30,000–100,000 lux. Most vegetables need many hours of bright light a day.",
        "Too dark: slow, leggy growth. Trim nearby shade or move the crop.",
        "Very strong light with heat can scorch leaves. Give shade in the hottest hours."
    )
}

@Composable
fun SensorInfoDialog(info: SensorInfo, currentValue: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${info.icon}  ${tr(info.title)}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${tr("Current reading")}: $currentValue",
                    color = CropGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                InfoSection("What is it?", info.meaning)
                InfoSection("Why it matters", info.purpose)
                InfoSection("Good range", info.goodRange, CropGreen)
                InfoSection("If too low", info.tooLow)
                InfoSection("If too high", info.tooHigh)
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Close")) } }
    )
}

@Composable
private fun InfoSection(heading: String, body: String, headingColor: androidx.compose.ui.graphics.Color? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(tr(heading), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = headingColor ?: MaterialTheme.colorScheme.onSurface)
        Text(tr(body), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
