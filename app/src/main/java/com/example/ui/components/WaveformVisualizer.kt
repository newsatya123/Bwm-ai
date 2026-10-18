package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RadiantEmerald
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    rmsDb: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveformTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        val barCount = 28
        val spacing = size.width / (barCount + 1)
        val barWidth = spacing * 0.55f
        val maxHeight = size.height * 0.85f
        val minHeight = 6.dp.toPx()

        for (i in 0 until barCount) {
            val x = (i + 1) * spacing - (barWidth / 2f)

            val waveFactor = (sin(phase + i * 0.45f) + 1f) / 2f
            val activityMultiplier = when {
                isListening -> (0.35f + rmsDb * 0.95f).coerceIn(0.2f, 1.3f)
                isSpeaking -> (0.5f + waveFactor * 0.5f)
                else -> 0.08f
            }

            val barHeight = (minHeight + (maxHeight - minHeight) * waveFactor * activityMultiplier)
                .coerceIn(minHeight, maxHeight)

            val y = (size.height - barHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(NeonCyan, ElectricViolet, RadiantEmerald)
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
