package com.cropcast.app.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropcast.app.data.FarmSuitabilityEngine
import com.cropcast.app.data.model.AlertSettings
import com.cropcast.app.ui.CropCastUiState
import com.cropcast.app.ui.components.CropIcon
import com.cropcast.app.ui.localization.SUPPORTED_LANGUAGES
import com.cropcast.app.ui.localization.localize
import com.cropcast.app.ui.localization.tr
import com.cropcast.app.ui.theme.CropGreen
import com.cropcast.app.ui.theme.DeepGreen
import java.text.DateFormat
import java.util.Locale
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: CropCastUiState,
    onBack: () -> Unit,
    onSaveSettings: (AlertSettings) -> Unit,
    onTestConnection: () -> Unit,
    onRestartEsp32: () -> Unit,
    onStartPairing: () -> Unit,
    onCancelPairing: () -> Unit,
    onUpdateAccount: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onChangePassword: (String) -> Unit,
    onSignOut: () -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var editor by remember { mutableStateOf<EditorRequest?>(null) }
    var passwordDialog by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }
    var cropDialog by remember { mutableStateOf(false) }
    var soilDialog by remember { mutableStateOf(false) }
    var mapPicker by remember { mutableStateOf(false) }
    val localizedMessage = state.message?.let { localize(it, state.settings.language) }
    val farmNameLabel = tr("Farm or field name")
    val deviceNameLabel = tr("ESP32 device name")
    val farmLatitudeLabel = tr("Farm latitude")
    val farmLongitudeLabel = tr("Farm longitude")
    val currentCropLabel = tr("Current crop")
    val cropVarietyLabel = tr("Crop variety")
    val plantingDateLabel = tr("Planting date")
    val plantingDateDialogTitle = tr("Planting date (YYYY-MM-DD)")
    val notSetLabel = tr("Not set")
    val displayNameLabel = tr("Display name")

    LaunchedEffect(localizedMessage) {
        localizedMessage?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tr("Settings"), fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Back")) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsSection("🚜", tr("Farm & Device")) {
                    EditableRow(farmNameLabel, state.settings.farmName) {
                        editor = EditorRequest(farmNameLabel, state.settings.farmName) {
                            onSaveSettings(state.settings.copy(farmName = it))
                        }
                    }
                    Divider()
                    EditableRow(deviceNameLabel, state.settings.deviceName) {
                        editor = EditorRequest(deviceNameLabel, state.settings.deviceName) {
                            onSaveSettings(state.settings.copy(deviceName = it))
                        }
                    }
                    Divider()
                    ActionValueRow(tr("Pair a device"), state.account.deviceId.ifBlank { notSetLabel }, onStartPairing)
                    Divider()
                    Text(
                        tr("Farm coordinates let CropCast obtain a 30-day rainfall estimate"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    EditableRow(farmLatitudeLabel, state.settings.farmLatitude.ifBlank { notSetLabel }) {
                        editor = EditorRequest(farmLatitudeLabel, state.settings.farmLatitude) {
                            onSaveSettings(state.settings.copy(farmLatitude = it))
                        }
                    }
                    Divider()
                    EditableRow(farmLongitudeLabel, state.settings.farmLongitude.ifBlank { notSetLabel }) {
                        editor = EditorRequest(farmLongitudeLabel, state.settings.farmLongitude) {
                            onSaveSettings(state.settings.copy(farmLongitude = it))
                        }
                    }
                    Divider()
                    OutlinedButton(onClick = { mapPicker = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("📍 ${tr("Pick farm on map or use my location")}")
                    }
                    Divider()
                    InfoRow(tr("Connection status"), if (state.status.online) tr("Online") else if (state.isDemo) tr("Demo mode") else tr("Offline"), if (state.status.online) CropGreen else Color(0xFFE45151))
                    Divider()
                    InfoRow(tr("Last synchronization"), formatSyncTime(state.reading.timestamp, tr("Waiting for data")))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onTestConnection, modifier = Modifier.weight(1f)) {
                            Text(tr("Test connection"))
                        }
                        Button(
                            onClick = onRestartEsp32,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
                        ) { Text(tr("Restart ESP32")) }
                    }
                }
            }

            item {
                SettingsSection("🔔", tr("Alerts & Notifications")) {
                    SwitchRow(tr("All farm alerts"), tr("Master control for sensor alerts"), state.settings.enabled) {
                        if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onSaveSettings(state.settings.copy(enabled = it))
                    }
                    SwitchRow(tr("Low soil moisture"), tr("Warn when moisture is below the threshold"), state.settings.lowMoistureAlertEnabled) {
                        onSaveSettings(state.settings.copy(lowMoistureAlertEnabled = it))
                    }
                    SliderRow(tr("Soil moisture threshold"), "${state.settings.soilMoistureThreshold.roundToInt()}%", state.settings.soilMoistureThreshold.toFloat(), 10f..80f) {
                        onSaveSettings(state.settings.copy(soilMoistureThreshold = it.toDouble()))
                    }
                    SliderRow(tr("High temperature"), "${state.settings.highTemperatureThreshold.roundToInt()}°C", state.settings.highTemperatureThreshold.toFloat(), 25f..50f) {
                        onSaveSettings(state.settings.copy(highTemperatureThreshold = it.toDouble()))
                    }
                    SliderRow(tr("Minimum soil pH"), "%.1f".format(state.settings.minSoilPh), state.settings.minSoilPh.toFloat(), 3.5f..7f) {
                        onSaveSettings(state.settings.copy(minSoilPh = it.toDouble()))
                    }
                    SliderRow(tr("Maximum soil pH"), "%.1f".format(state.settings.maxSoilPh), state.settings.maxSoilPh.toFloat(), 6f..10f) {
                        onSaveSettings(state.settings.copy(maxSoilPh = it.toDouble()))
                    }
                    SliderRow(tr("Device offline"), "${state.settings.deviceOfflineMinutes} ${tr("minutes")}", state.settings.deviceOfflineMinutes.toFloat(), 1f..15f) {
                        onSaveSettings(state.settings.copy(deviceOfflineMinutes = it.roundToInt()))
                    }
                    SliderRow(tr("Repeat alerts"), "${tr("Every")} ${state.settings.repeatIntervalMinutes} ${tr("minutes")}", state.settings.repeatIntervalMinutes.toFloat(), 15f..120f) {
                        onSaveSettings(state.settings.copy(repeatIntervalMinutes = it.roundToInt()))
                    }
                    SwitchRow(tr("Sound"), tr("Play a sound for important farm alerts"), state.settings.soundEnabled) {
                        onSaveSettings(state.settings.copy(soundEnabled = it))
                    }
                    SwitchRow(tr("Vibration"), tr("Vibrate for important farm alerts"), state.settings.vibrationEnabled) {
                        onSaveSettings(state.settings.copy(vibrationEnabled = it))
                    }
                    ActionRow(tr("Open Android notification settings")) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                }
            }

            item {
                SettingsSection("🌱", tr("Crop Profile")) {
                    val currentCrop = FarmSuitabilityEngine.CROPS.firstOrNull { it.name == state.settings.currentCrop }
                    ActionValueRow(
                        currentCropLabel,
                        currentCrop?.let { tr(it.name) } ?: notSetLabel,
                        onClick = { cropDialog = true },
                        leading = currentCrop?.let { { CropIcon(it.name, it.profile.icon, 40.dp) } }
                    )
                    Divider()
                    EditableRow(tr("Soil type"), state.settings.soilType.ifBlank { tr("Use soil map") }) {
                        soilDialog = true
                    }
                    Divider()
                    // A variety only makes sense once a crop is chosen.
                    if (state.settings.currentCrop.isNotBlank()) {
                        EditableRow(cropVarietyLabel, state.settings.cropVariety.ifBlank { notSetLabel }) {
                            editor = EditorRequest(cropVarietyLabel, state.settings.cropVariety) {
                                onSaveSettings(state.settings.copy(cropVariety = it))
                            }
                        }
                        Divider()
                    }
                    EditableRow(plantingDateLabel, state.settings.plantingDate.ifBlank { notSetLabel }) {
                        editor = EditorRequest(plantingDateDialogTitle, state.settings.plantingDate) {
                            onSaveSettings(state.settings.copy(plantingDate = it))
                        }
                    }
                    Divider()
                    SwitchRow(
                        tr("Irrigation available"),
                        tr("Dry months will not lower crop scores; too much rain still does"),
                        state.settings.irrigationAvailable
                    ) {
                        onSaveSettings(state.settings.copy(irrigationAvailable = it))
                    }
                }
            }

            item {
                SettingsSection("🌐", tr("Language & Offline Data")) {
                    ActionValueRow(tr("Language"), state.settings.language, onClick = { languageDialog = true })
                    Divider()
                    SwitchRow(tr("Dark mode"), tr("Use a darker theme throughout the app"), state.settings.darkModeEnabled, onDarkModeChange)
                    Divider()
                    InfoRow(tr("Offline data availability"), tr("Enabled · Firebase disk cache"), CropGreen)
                    SwitchRow(tr("Cloud history"), tr("Keep sensor history synchronized"), state.settings.cloudHistoryEnabled) {
                        onSaveSettings(state.settings.copy(cloudHistoryEnabled = it))
                    }
                }
            }

            item {
                SettingsSection("👤", tr("Account & Privacy")) {
                    EditableRow(tr("Account name"), tr(state.account.displayName)) {
                        editor = EditorRequest(displayNameLabel, state.account.displayName) {
                            onUpdateAccount(it)
                        }
                    }
                    Divider()
                    InfoRow(tr("Sign-in status"), if (state.account.isAnonymous) tr("Guest / anonymous") else state.account.email)
                    ActionRow(tr("Change password")) { passwordDialog = true }
                    SwitchRow(tr("Share diagnostic information"), tr("Helps troubleshoot sensor connection issues"), state.settings.diagnosticsEnabled) {
                        onSaveSettings(state.settings.copy(diagnosticsEnabled = it))
                    }
                    TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("Sign out"), color = Color(0xFFE45151), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    if (mapPicker) {
        FarmLocationPickerDialog(
            initialLatitude = state.settings.farmLatitude.toDoubleOrNull(),
            initialLongitude = state.settings.farmLongitude.toDoubleOrNull(),
            onDismiss = { mapPicker = false },
            onConfirm = { latitude, longitude ->
                mapPicker = false
                onSaveSettings(
                    state.settings.copy(
                        farmLatitude = "%.4f".format(Locale.US, latitude),
                        farmLongitude = "%.4f".format(Locale.US, longitude)
                    )
                )
            }
        )
    }
    state.pairingCode?.let { PairingDialog(it, onCancelPairing) }
    editor?.let { request ->
        TextEditDialog(request = request, onDismiss = { editor = null })
    }
    if (passwordDialog) {
        PasswordDialog(onDismiss = { passwordDialog = false }) {
            passwordDialog = false
            onChangePassword(it)
        }
    }
    if (cropDialog) {
        // The name must match the crop list or the fertilizer plan cannot find it. Crops without a
        // verified per-hectare rate are flagged: they get pH advice only.
        CropChoiceDialog(
            title = currentCropLabel,
            onDismiss = { cropDialog = false }
        ) { crop ->
            cropDialog = false
            onSaveSettings(state.settings.copy(currentCrop = crop.orEmpty()))
        }
    }
    if (soilDialog) {
        SoilTypeDialog(current = state.settings.soilType, onDismiss = { soilDialog = false }) { choice ->
            soilDialog = false
            onSaveSettings(state.settings.copy(soilType = choice))
        }
    }
    if (languageDialog) {
        ChoiceDialog(
            title = tr("Language"),
            choices = SUPPORTED_LANGUAGES,
            onDismiss = { languageDialog = false }
        ) {
            languageDialog = false
            onLanguageChange(it)
        }
    }
}

private data class EditorRequest(val title: String, val value: String, val onSave: (String) -> Unit)

@Composable
private fun SettingsSection(icon: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text("$icon  $title", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), content = content)
        }
    }
}

@Composable
private fun EditableRow(label: String, value: String, onClick: () -> Unit) = ActionValueRow(label, value, onClick)

@Composable
private fun ActionValueRow(label: String, value: String, onClick: () -> Unit, leading: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Text(value, color = valueColor ?: MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = CropGreen, fontWeight = FontWeight.Bold)
        Icon(Icons.Default.ChevronRight, null, tint = CropGreen)
    }
}

@Composable
private fun SwitchRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = CropGreen))
    }
}

@Composable
private fun SliderRow(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(valueLabel, color = CropGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

@Composable
private fun TextEditDialog(request: EditorRequest, onDismiss: () -> Unit) {
    var value by remember(request) { mutableStateOf(request.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(request.title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { request.onSave(value.trim()); onDismiss() }, enabled = value.isNotBlank()) { Text(tr("Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } }
    )
}

@Composable
private fun PairingDialog(code: String, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(tr("Pair a device")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. ${tr("Power the ESP32. If it was paired before, hold its BOOT button for 5 seconds.")}")
                Text("2. ${tr("On your phone, join the Wi-Fi network named CropCast-XXXX.")}")
                Text("3. ${tr("In the setup page, choose your Wi-Fi, enter its password and this pairing code:")}")
                Text(code, fontFamily = FontFamily.Monospace, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = CropGreen)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Text(tr("Waiting for the device to come online"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text(tr("Cancel")) } }
    )
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Change password")) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(tr("New password")) }, visualTransformation = PasswordVisualTransformation(), singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(value) }, enabled = value.length >= 6) { Text(tr("Change")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } }
    )
}

/** Crop picker with a picture per crop; reports the crop's English name, or null for "None". */
@Composable
private fun CropChoiceDialog(title: String, onDismiss: () -> Unit, onChoice: (String?) -> Unit) {
    val flag = " (${tr("pH advice only")})"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    tr("None"),
                    Modifier.fillMaxWidth().clickable { onChoice(null) }.padding(vertical = 14.dp),
                    fontWeight = FontWeight.SemiBold
                )
                FarmSuitabilityEngine.CROPS.forEach { crop ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onChoice(crop.name) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CropIcon(crop.name, crop.profile.icon, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            tr(crop.name) + if (crop.needKgHa == null) flag else "",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } }
    )
}

/** How each soil type looks and feels: (hand test, base colour, grain count, grain size in dp). */
private class SoilLook(val feel: String, val color: Color, val grains: Int, val grainDp: Float)

private val SOIL_LOOKS = mapOf(
    "Clay" to SoilLook("Sticky when wet, cracks when dry. Squeezed, it makes a long ribbon.", Color(0xFF6B4A3A), 160, 0.8f),
    "Clay loam" to SoilLook("Sticky but crumbly. Makes a short ribbon that breaks.", Color(0xFF7A5A43), 110, 1.1f),
    "Loam" to SoilLook("Soft and crumbly, like cake crumbs. Holds a ball, but no ribbon.", Color(0xFF8B6B4A), 70, 1.5f),
    "Sandy loam" to SoilLook("Slightly gritty. Falls apart when you open your hand.", Color(0xFFA88B63), 45, 2.2f),
    "Sand" to SoilLook("Very gritty and loose, drains fast. Will not hold a shape.", Color(0xFFD2B98A), 30, 3.0f)
)

/** A drawn close-up of the soil: fine grains for clay, coarse grains for sand. */
@Composable
private fun SoilSwatch(type: String, look: SoilLook) {
    Canvas(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))) {
        drawRect(look.color)
        val random = java.util.Random(type.hashCode().toLong())
        val radius = look.grainDp.dp.toPx()
        repeat(look.grains) {
            val shade = if (random.nextBoolean()) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.25f)
            drawCircle(shade, radius, Offset(random.nextFloat() * size.width, random.nextFloat() * size.height))
        }
    }
}

@Composable
private fun SoilTypeDialog(current: String, onDismiss: () -> Unit, onChoice: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Soil type")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr("Rub a pinch of moist soil between your fingers and pick the closest match."),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FarmSuitabilityEngine.SOIL_TYPES.keys.forEach { type ->
                    val look = SOIL_LOOKS[type] ?: return@forEach
                    val selected = type == current
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape)
                            .border(if (selected) 2.dp else 1.dp, if (selected) CropGreen else MaterialTheme.colorScheme.outline, shape)
                            .clickable { onChoice(type) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SoilSwatch(type, look)
                        Column(Modifier.weight(1f)) {
                            Text(tr(type), fontWeight = FontWeight.Bold)
                            Text(tr(look.feel), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                TextButton(onClick = { onChoice("") }, modifier = Modifier.fillMaxWidth()) { Text(tr("Use soil map")) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } }
    )
}

@Composable
private fun ChoiceDialog(title: String, choices: List<String>, onDismiss: () -> Unit, onChoice: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                choices.forEach { choice ->
                    Text(choice, Modifier.fillMaxWidth().clickable { onChoice(choice) }.padding(vertical = 14.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } }
    )
}

private fun formatSyncTime(timestamp: Long, waitingText: String): String = if (timestamp <= 0L) {
    waitingText
} else {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
}
