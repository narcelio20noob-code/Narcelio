package com.example.games.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.Micro2048State
import com.example.games.SwipeDirection
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.LocalTelaJogoTokens
import com.example.ui.theme.OledPureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.VioletOrbital
import kotlin.math.abs

@Composable
fun Micro2048GameView(
    state: Micro2048State,
    highScore: Int,
    onSwipe: (SwipeDirection) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalTelaJogoTokens.current
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var swipeTriggeredInGesture by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Compact HUD row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactScorePill(label = "PONTOS", value = state.score.toString())
                CompactScorePill(label = "RECORDE", value = maxOf(highScore, state.score).toString())
                if (state.reachedTarget256) {
                    CompactScorePill(
                        label = "META",
                        value = "256+",
                        accent = tokens.accentEmerald,
                    )
                }
            }
            IconButton(
                onClick = onRestart,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("micro_2048_restart_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reiniciar 2048 Micro",
                    tint = tokens.accentCyan,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3x3 single-thumb swipe board
        Box(
            modifier = Modifier
                .widthIn(max = 264.dp)
                .fillMaxWidth(0.84f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(if (tokens.oledPowerSaver) OledPureBlack else MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = tokens.hairlineBorder,
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(8.dp)
                .pointerInput(state.isGameOver) {
                    detectDragGestures(
                        onDragStart = {
                            dragX = 0f
                            dragY = 0f
                            swipeTriggeredInGesture = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (swipeTriggeredInGesture) return@detectDragGestures
                            dragX += dragAmount.x
                            dragY += dragAmount.y
                            val threshold = 34f
                            if (abs(dragX) > threshold || abs(dragY) > threshold) {
                                swipeTriggeredInGesture = true
                                val dir = if (abs(dragX) > abs(dragY)) {
                                    if (dragX > 0f) SwipeDirection.RIGHT else SwipeDirection.LEFT
                                } else {
                                    if (dragY > 0f) SwipeDirection.DOWN else SwipeDirection.UP
                                }
                                onSwipe(dir)
                            }
                        },
                    )
                }
                .testTag("micro_2048_board"),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (col in 0 until 3) {
                            val idx = row * 3 + col
                            val value = state.board[idx]
                            val highlighted = idx in state.lastMergedIndices || idx == state.lastSpawnedIndex
                            MicroTileCell(
                                value = value,
                                isHighlighted = highlighted,
                                oledPowerSaver = tokens.oledPowerSaver,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                            )
                        }
                    }
                }
            }

            if (state.isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OledPureBlack.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "FIM DE JOGO",
                            style = MaterialTheme.typography.titleLarge,
                            color = CrimsonGuard,
                        )
                        Text(
                            text = "Placar ${state.score} • Maior ${state.bestTile}",
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite,
                        )
                        FilledTonalButton(
                            onClick = onRestart,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("micro_2048_gameover_retry"),
                        ) {
                            Text("Nova Partida")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Deslize com o polegar em qualquer direção",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.mutedText,
        )
    }
}

@Composable
private fun MicroTileCell(
    value: Int,
    isHighlighted: Boolean,
    oledPowerSaver: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = tileAccentColor(value)
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted && value > 0) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileScale",
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            value == 0 -> if (oledPowerSaver) OledPureBlack else Color(0xFF111822)
            oledPowerSaver -> OledPureBlack
            else -> accent.copy(alpha = 0.14f)
        },
        label = "tileBg",
    )
    val borderColor = when {
        value == 0 -> Color(0xFF161D27)
        else -> accent.copy(alpha = if (oledPowerSaver) 0.9f else 0.7f)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (value == 0) 1.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (value > 0) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (value >= 100) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = accent,
            )
        }
    }
}

@Composable
fun CompactScorePill(
    label: String,
    value: String,
    accent: Color = MaterialTheme.colorScheme.onSurface,
) {
    val tokens = LocalTelaJogoTokens.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, tokens.hairlineBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.mutedText,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )
    }
}

private fun tileAccentColor(value: Int): Color = when (value) {
    2 -> Color(0xFFCBD5E1)
    4 -> CyanPulse
    8 -> EmeraldSignal
    16 -> AmberAlert
    32 -> VioletOrbital
    64 -> CrimsonGuard
    128 -> Color(0xFF38BDF8)
    256 -> Color(0xFFFACC15)
    else -> PureWhite
}
