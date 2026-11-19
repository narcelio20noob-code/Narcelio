package com.example.games.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.games.ChromaticMemoryState
import com.example.games.MemoryPhase
import com.example.games.PadSymbol
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.LocalTelaJogoTokens
import com.example.ui.theme.OledPureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.VioletOrbital

@Composable
fun ChromaticMemoryGameView(
    state: ChromaticMemoryState,
    highScore: Int,
    onPadTap: (Int) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalTelaJogoTokens.current

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
                CompactScorePill(label = "RODADA", value = state.round.toString())
                CompactScorePill(label = "PONTOS", value = state.score.toString())
                CompactScorePill(label = "RECORDE", value = maxOf(highScore, state.score).toString())
            }
            IconButton(
                onClick = onRestart,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("chromatic_restart_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reiniciar Memória Cromática",
                    tint = tokens.accentCyan,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val statusPrompt = when (state.phase) {
            MemoryPhase.SHOWING_SEQUENCE -> "OBSERVE A SEQUÊNCIA (${state.sequence.size})"
            MemoryPhase.AWAITING_INPUT -> "SUA VEZ (${state.userIndex}/${state.sequence.size})"
            MemoryPhase.ROUND_CLEARED -> "SEQUÊNCIA CORRETA!"
            MemoryPhase.GAME_OVER -> "SEQUÊNCIA INTERROMPIDA"
        }
        val statusColor = when (state.phase) {
            MemoryPhase.SHOWING_SEQUENCE -> CyanPulse
            MemoryPhase.AWAITING_INPUT -> EmeraldSignal
            MemoryPhase.ROUND_CLEARED -> EmeraldSignal
            MemoryPhase.GAME_OVER -> CrimsonGuard
        }
        Text(
            text = statusPrompt,
            style = MaterialTheme.typography.labelLarge,
            color = statusColor,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .widthIn(max = 264.dp)
                .fillMaxWidth(0.86f),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ChromaticPadButton(
                        symbol = PadSymbol.CIRCLE,
                        accent = CyanPulse,
                        isLit = state.activeLitPad == 0,
                        oledPowerSaver = tokens.oledPowerSaver,
                        onClick = { onPadTap(0) },
                        modifier = Modifier
                            .weight(1f)
                            .height(104.dp),
                    )
                    ChromaticPadButton(
                        symbol = PadSymbol.TRIANGLE,
                        accent = EmeraldSignal,
                        isLit = state.activeLitPad == 1,
                        oledPowerSaver = tokens.oledPowerSaver,
                        onClick = { onPadTap(1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(104.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ChromaticPadButton(
                        symbol = PadSymbol.SQUARE,
                        accent = AmberAlert,
                        isLit = state.activeLitPad == 2,
                        oledPowerSaver = tokens.oledPowerSaver,
                        onClick = { onPadTap(2) },
                        modifier = Modifier
                            .weight(1f)
                            .height(104.dp),
                    )
                    ChromaticPadButton(
                        symbol = PadSymbol.DIAMOND,
                        accent = VioletOrbital,
                        isLit = state.activeLitPad == 3,
                        oledPowerSaver = tokens.oledPowerSaver,
                        onClick = { onPadTap(3) },
                        modifier = Modifier
                            .weight(1f)
                            .height(104.dp),
                    )
                }
            }

            if (state.phase == MemoryPhase.GAME_OVER) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(218.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(OledPureBlack.copy(alpha = 0.90f))
                        .border(1.dp, CrimsonGuard.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "FIM DA SEQUÊNCIA",
                            style = MaterialTheme.typography.titleLarge,
                            color = CrimsonGuard,
                        )
                        Text(
                            text = "Rodada ${state.round} • ${state.score} pts",
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite,
                        )
                        FilledTonalButton(
                            onClick = onRestart,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chromatic_gameover_retry"),
                        ) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "4 quadrantes táteis • Suporte a toques rápidos",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.mutedText,
        )
    }
}

@Composable
private fun ChromaticPadButton(
    symbol: PadSymbol,
    accent: Color,
    isLit: Boolean,
    oledPowerSaver: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isLit) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "padScale",
    )
    val fillBg by animateColorAsState(
        targetValue = when {
            isLit -> accent.copy(alpha = 0.28f)
            oledPowerSaver -> OledPureBlack
            else -> accent.copy(alpha = 0.08f)
        },
        label = "padFill",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .scale(scale)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .background(fillBg)
            .border(
                width = if (isLit) 2.5.dp else 1.5.dp,
                color = if (isLit) PureWhite else accent.copy(alpha = 0.85f),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = accent),
                onClick = onClick,
            )
            .semantics { contentDescription = symbol.label }
            .testTag("chromatic_pad_${symbol.index}"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(44.dp)) {
            val stroke = Stroke(width = 3.dp.toPx())
            val style = if (isLit) Fill else stroke
            val drawColor = if (isLit) PureWhite else accent
            when (symbol) {
                PadSymbol.CIRCLE -> {
                    drawCircle(
                        color = drawColor,
                        radius = size.minDimension * 0.42f,
                        style = style,
                    )
                }

                PadSymbol.TRIANGLE -> {
                    val path = Path().apply {
                        moveTo(size.width / 2f, size.height * 0.1f)
                        lineTo(size.width * 0.9f, size.height * 0.86f)
                        lineTo(size.width * 0.1f, size.height * 0.86f)
                        close()
                    }
                    drawPath(path = path, color = drawColor, style = style)
                }

                PadSymbol.SQUARE -> {
                    val inset = size.minDimension * 0.14f
                    drawRect(
                        color = drawColor,
                        topLeft = Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - inset * 2f,
                            size.height - inset * 2f,
                        ),
                        style = style,
                    )
                }

                PadSymbol.DIAMOND -> {
                    val path = Path().apply {
                        moveTo(size.width / 2f, size.height * 0.08f)
                        lineTo(size.width * 0.92f, size.height / 2f)
                        lineTo(size.width / 2f, size.height * 0.92f)
                        lineTo(size.width * 0.08f, size.height / 2f)
                        close()
                    }
                    drawPath(path = path, color = drawColor, style = style)
                }
            }
        }
    }
}
