package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.AssistantUiState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OrbExecutingPulse
import com.example.ui.theme.RadiantEmerald
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbVisualizer(
    state: AssistantUiState,
    rmsDb: Float,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    logoTheme: com.example.data.local.AssistantLogoTheme = com.example.data.local.AssistantLogoTheme.NEON_CYAN,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbTransition")

    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    val (primaryColor, secondaryColor) = when (state) {
        AssistantUiState.LISTENING -> RadiantEmerald to logoTheme.primaryColor
        AssistantUiState.PROCESSING -> ElectricViolet to Color(0xFFF72585)
        AssistantUiState.EXECUTING -> OrbExecutingPulse to Color(0xFFFF9E00)
        AssistantUiState.SPEAKING -> logoTheme.primaryColor to logoTheme.secondaryColor
        AssistantUiState.ERROR -> ErrorRed to Color(0xFFBA1839)
        AssistantUiState.IDLE -> logoTheme.primaryColor to logoTheme.secondaryColor
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("animated_voice_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.65f

            val audioBoost = if (state == AssistantUiState.LISTENING || state == AssistantUiState.SPEAKING) {
                rmsDb * 0.45f
            } else 0f

            val currentRadius = baseRadius * (if (state == AssistantUiState.IDLE) idlePulse else (1.0f + audioBoost))

            if (state == AssistantUiState.LISTENING || state == AssistantUiState.SPEAKING) {
                val rippleRadius = currentRadius * rippleScale
                val rippleAlpha = (1f - (rippleScale - 1.0f) / 0.35f).coerceIn(0f, 1f) * 0.5f
                drawCircle(
                    color = primaryColor.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.45f),
                        secondaryColor.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.5f
                ),
                radius = currentRadius * 1.5f,
                center = center
            )

            if (state == AssistantUiState.PROCESSING) {
                val angleRad = Math.toRadians(rotationAngle.toDouble())
                val endX = center.x + (currentRadius * cos(angleRad)).toFloat()
                val endY = center.y + (currentRadius * sin(angleRad)).toFloat()
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor, primaryColor),
                        start = center,
                        end = Offset(endX, endY)
                    ),
                    radius = currentRadius * 1.12f,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor,
                        secondaryColor
                    ),
                    center = Offset(center.x - currentRadius * 0.2f, center.y - currentRadius * 0.2f),
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.7f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - currentRadius * 0.3f, center.y - currentRadius * 0.3f),
                    radius = currentRadius * 0.5f
                ),
                radius = currentRadius * 0.5f,
                center = Offset(center.x - currentRadius * 0.25f, center.y - currentRadius * 0.25f)
            )
        }
    }
}
