package com.example.games.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.games.OrbitalReflexState
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.LocalTelaJogoTokens
import com.example.ui.theme.OledPureBlack
import com.example.ui.theme.PureWhite
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbitalReflexGameView(
    state: OrbitalReflexState,
    highScore: Int,
    isLoopActive: Boolean,
    isRunning: Boolean,
    oledPowerSaver: Boolean,
    onTapCanvas: () -> Unit,
    onStepFrame: (Float) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalTelaJogoTokens.current

    // Adaptive power-budget frame loop: 30fps cap in OLED Pure Black mode, 60fps otherwise.
    // Automatically cancels when paused, pocket-blocked, screen-off, or disposed (zero leaks).
    LaunchedEffect(isLoopActive, isRunning, oledPowerSaver) {
        if (!isLoopActive || !isRunning || state.isGameOver) return@LaunchedEffect
        val minFrameNanos = if (oledPowerSaver) 33_333_333L else 16_666_666L
        var lastTickNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastTickNanos == 0L) {
                    lastTickNanos = frameNanos
                } else {
                    val elapsedNanos = frameNanos - lastTickNanos
                    if (elapsedNanos >= minFrameNanos) {
                        lastTickNanos = frameNanos
                        onStepFrame(elapsedNanos / 1_000_000_000f)
                    }
                }
            }
        }
    }

    val laneRatio by animateFloatAsState(
        targetValue = if (state.playerLane == 0) 0.56f else 0.84f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "orbitalLaneRatio",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactScorePill(label = "PONTOS", value = state.score.toString())
                CompactScorePill(
                    label = "COMBO",
                    value = "×${state.combo}",
                    accent = if (state.combo > 1) EmeraldSignal else PureWhite,
                )
                CompactScorePill(label = "RECORDE", value = maxOf(highScore, state.score).toString())
            }
            IconButton(
                onClick = onRestart,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("orbital_restart_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reiniciar Reflexo Orbital",
                    tint = tokens.accentCyan,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .widthIn(max = 264.dp)
                .fillMaxWidth(0.84f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(if (oledPowerSaver) OledPureBlack else MaterialTheme.colorScheme.surface)
                .border(1.dp, tokens.hairlineBorder, RoundedCornerShape(18.dp))
                .pointerInput(state.isGameOver) {
                    detectTapGestures {
                        onTapCanvas()
                    }
                }
                .testTag("orbital_canvas"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f
                val innerRadius = maxRadius * 0.56f
                val outerRadius = maxRadius * 0.84f

                // Track rings (minimal hairline strokes for OLED efficiency)
                drawCircle(
                    color = tokens.hairlineBorder,
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawCircle(
                    color = tokens.hairlineBorder,
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )

                // Core anchor
                drawCircle(
                    color = CyanPulse.copy(alpha = 0.25f),
                    radius = maxRadius * 0.16f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )

                // Hazards as glowing red/amber orbital arcs
                state.hazards.forEach { hazard ->
                    val r = if (hazard.lane == 0) innerRadius else outerRadius
                    drawArc(
                        color = CrimsonGuard,
                        startAngle = hazard.angleDeg - hazard.arcSpanDeg / 2f,
                        sweepAngle = hazard.arcSpanDeg,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                // Energy orbs
                state.orbs.forEach { orb ->
                    val r = if (orb.lane == 0) innerRadius else outerRadius
                    val rad = Math.toRadians(orb.angleDeg.toDouble())
                    val pos = Offset(
                        x = center.x + (r * cos(rad)).toFloat(),
                        y = center.y + (r * sin(rad)).toFloat(),
                    )
                    drawCircle(
                        color = EmeraldSignal,
                        radius = 6.dp.toPx(),
                        center = pos,
                    )
                }

                // Player probe
                val playerR = maxRadius * laneRatio
                val playerRad = Math.toRadians(state.playerAngleDeg.toDouble())
                val playerPos = Offset(
                    x = center.x + (playerR * cos(playerRad)).toFloat(),
                    y = center.y + (playerR * sin(playerRad)).toFloat(),
                )
                drawCircle(
                    color = CyanPulse,
                    radius = 9.dp.toPx(),
                    center = playerPos,
                )
                drawCircle(
                    color = PureWhite,
                    radius = 4.dp.toPx(),
                    center = playerPos,
                )
            }

            if (!isRunning && !state.isGameOver) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(OledPureBlack.copy(alpha = 0.78f))
                        .border(1.dp, CyanPulse.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "TOQUE PARA ORBITAR",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyanPulse,
                    )
                }
            }

            if (state.isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OledPureBlack.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "COLISÃO ORBITAL",
                            style = MaterialTheme.typography.titleLarge,
                            color = AmberAlert,
                        )
                        Text(
                            text = "Placar ${state.score}",
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite,
                        )
                        FilledTonalButton(
                            onClick = onRestart,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("orbital_gameover_retry"),
                        ) {
                            Text("Orbitar Novamente")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (oledPowerSaver) {
                "Toque único alterna órbita • Loop 30fps OLED"
            } else {
                "Toque único alterna órbita • Loop 60fps"
            },
            style = MaterialTheme.typography.labelSmall,
            color = tokens.mutedText,
        )
    }
}
