package com.cropcast.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropcast.app.R
import com.cropcast.app.ui.theme.CropGreen
import com.cropcast.app.ui.theme.DeepGreen
import com.cropcast.app.ui.theme.ScreenBackground
import com.cropcast.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** Cold-start screen shown over the app: soft green wash, the logo springing in, name, tagline, loading dots. */
@Composable
fun SplashOverlay() {
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(350)) }
        launch { logoScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
        launch { textAlpha.animateTo(1f, tween(600, delayMillis = 350)) }
    }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE3F6D6), ScreenBackground, Color.White)))
    ) {
        // Two large soft discs for depth, bleeding off the corners.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(CropGreen.copy(alpha = 0.10f), radius = size.width * 0.75f, center = Offset(size.width * 1.0f, size.height * 0.05f))
            drawCircle(DeepGreen.copy(alpha = 0.07f), radius = size.width * 0.65f, center = Offset(0f, size.height * 0.98f))
        }
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(176.dp).scale(logoScale.value).alpha(logoAlpha.value)
                    .shadow(24.dp, CircleShape, ambientColor = DeepGreen, spotColor = DeepGreen)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.cropcast_logo_mark), contentDescription = null, modifier = Modifier.size(124.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("CropCast", Modifier.alpha(textAlpha.value), color = DeepGreen, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text("Smart Farming Assistant", Modifier.alpha(textAlpha.value), color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp).alpha(textAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoadingDots()
            Spacer(Modifier.height(14.dp))
            Text("From soil to harvest", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun LoadingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val level by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(
                    keyframes {
                        durationMillis = 1200
                        0.25f at (index * 200)
                        1f at (index * 200 + 300) using LinearEasing
                        0.25f at (index * 200 + 600) using LinearEasing
                    },
                    RepeatMode.Restart
                ),
                label = "dot$index"
            )
            Box(Modifier.size(9.dp).alpha(level).background(CropGreen, CircleShape))
        }
    }
}
