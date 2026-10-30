package com.cropcast.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cropcast.app.data.FirebaseSensorRepository
import com.cropcast.app.data.LocalPreferences
import com.cropcast.app.ui.CropCastApp
import com.cropcast.app.ui.CropCastViewModel
import com.cropcast.app.ui.CropCastViewModelFactory
import com.cropcast.app.ui.SplashOverlay
import com.cropcast.app.ui.theme.CropCastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val forceDemo = isDebuggable && intent.getBooleanExtra(EXTRA_FORCE_DEMO, false)
        val demoCrop = if (isDebuggable) intent.getStringExtra(EXTRA_DEMO_CROP) ?: "Potato" else "Potato"
        splashScreen.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(180L)
                .withEndAction(provider::remove)
                .start()
        }
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            CropCastTheme {
                val viewModel: CropCastViewModel = viewModel(
                    factory = CropCastViewModelFactory(
                        FirebaseSensorRepository(applicationContext, forceDemo = forceDemo),
                        demoCrop = demoCrop,
                        preferences = LocalPreferences(applicationContext)
                    )
                )
                // Only on a cold start: rotating or returning to the app skips it.
                var showSplash by rememberSaveable { mutableStateOf(savedInstanceState == null) }
                LaunchedEffect(Unit) {
                    delay(1800L)
                    showSplash = false
                }
                Box(Modifier.fillMaxSize()) {
                    CropCastApp(viewModel)
                    AnimatedVisibility(showSplash, exit = fadeOut(tween(450))) { SplashOverlay() }
                }
            }
        }
    }

    private companion object {
        const val EXTRA_FORCE_DEMO = "forceDemo"
        const val EXTRA_DEMO_CROP = "demoCrop"
    }
}
