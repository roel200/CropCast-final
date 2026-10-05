package com.cropcast.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cropcast.app.ui.localization.tr
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Locale

/**
 * Full-screen OpenStreetMap picker (osmdroid, no API key): tap the map to place the farm pin,
 * or use the phone's location. Only the chosen point is returned; nothing is tracked.
 */
@Composable
fun FarmLocationPickerDialog(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onConfirm: (latitude: Double, longitude: Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by remember {
        mutableStateOf(if (initialLatitude != null && initialLongitude != null) GeoPoint(initialLatitude, initialLongitude) else null)
    }
    var status by remember { mutableStateOf<String?>(null) }

    val locatingText = tr("Finding your location…")
    val noLocationText = tr("Could not get your location. Turn on GPS and try again, or tap the map.")
    val deniedText = tr("Location permission was denied. Tap the map to place your farm instead.")

    val mapView = remember {
        // OSM tile policy: identify the app, and keep the tile cache out of shared storage.
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false  // keeps longitude inside -180..180
            isVerticalMapRepetitionEnabled = false
            controller.setZoom(if (picked != null) 16.0 else 9.0)
            controller.setCenter(picked ?: GeoPoint(11.0, 122.6))  // Panay, Iloilo
            overlays.add(CopyrightOverlay(context))  // OSM requires visible attribution
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    picked = GeoPoint(p.latitude, p.longitude)
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            }))
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    @SuppressLint("MissingPermission")  // only called after the permission launcher reports a grant
    fun locate() {
        status = locatingText
        scope.launch {
            val location = runCatching {
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                    .await()
            }.getOrNull()
            if (location == null) {
                status = noLocationText
            } else {
                val point = GeoPoint(location.latitude, location.longitude)
                picked = point
                mapView.controller.setZoom(16.0)
                mapView.controller.animateTo(point)
                status = null
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) locate() else status = deniedText
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            view.overlays.removeAll { it is Marker }
                            picked?.let { point ->
                                view.overlays.add(Marker(view).apply {
                                    position = point
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                })
                            }
                            view.invalidate()
                        }
                    )
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Farm location"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        picked?.let { "%.4f, %.4f".format(Locale.US, it.latitude, it.longitude) }
                            ?: tr("Tap the map to place your farm."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    status?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("📍 ${tr("Use my current location")}") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(tr("Cancel")) }
                        Button(
                            onClick = { picked?.let { onConfirm(it.latitude, it.longitude) } },
                            enabled = picked != null,
                            modifier = Modifier.weight(1f)
                        ) { Text(tr("Save location")) }
                    }
                }
            }
        }
    }
}
