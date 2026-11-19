package com.example.locksurface

import android.content.Context
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.model.GameId
import com.example.core.model.LockSurfacePhase
import com.example.core.model.UnlockReason
import com.example.games.ui.ChromaticMemoryGameView
import com.example.games.ui.Micro2048GameView
import com.example.games.ui.OrbitalReflexGameView
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.LocalTelaJogoTokens
import com.example.ui.theme.OledPureBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockScreenSimulatorScreen(
    uiState: LockSurfaceUiState,
    viewModel: LockSurfaceViewModel,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalTelaJogoTokens.current
    val context = LocalContext.current

    var clockText by remember { mutableStateOf(formatTimeNow()) }
    var dateText by remember { mutableStateOf(formatDateNow()) }
    val batteryPercent = remember { readBatteryPercentage(context) }

    LaunchedEffect(uiState.phase) {
        while (true) {
            clockText = formatTimeNow()
            dateText = formatDateNow()
            delay(15_000L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (uiState.settings.oledPowerSaver) OledPureBlack else MaterialTheme.colorScheme.background,
            ),
    ) {
        // Main simulated lock-screen layout: top system area + bottom-half LockSurface widget
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. Simulator Hardware & Power Controls Bar (for interactive testing)
            SimulatorControlStrip(
                oledPowerSaver = uiState.settings.oledPowerSaver,
                silentMode = uiState.settings.silentMode,
                simulatedPocket = uiState.sensorReading.simulatedNear,
                onToggleOled = viewModel::toggleOledPowerSaver,
                onToggleSilent = viewModel::toggleSilentMode,
                onTogglePocket = viewModel::toggleSimulatedPocket,
                onSimulateScreenOff = viewModel::simulateScreenOff,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Top Lock Screen Area (Always visible & unobstructed: Clock, Battery, Masked Notifications)
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .weight(0.36f, fill = false),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PhaseTelemetryBadge(
                        phase = uiState.phase,
                        inactivityRemainingSec = uiState.inactivityRemainingSec,
                        oledPowerSaver = uiState.settings.oledPowerSaver,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryChargingFull,
                            contentDescription = "Bateria",
                            tint = EmeraldSignal,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${batteryPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite,
                        )
                    }
                }

                Text(
                    text = clockText,
                    style = MaterialTheme.typography.displayLarge,
                    color = PureWhite,
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.mutedText,
                )

                // Masked notification pill — tapping outside LockSurface enforces unlock
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (uiState.settings.oledPowerSaver) OledPureBlack else MaterialTheme.colorScheme.surface,
                        )
                        .border(1.dp, tokens.hairlineBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            viewModel.requestUnlock(UnlockReason.MASKED_NOTIFICATION_TAP)
                        }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("masked_notifications_pill"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        tint = tokens.mutedText,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.notif_masked_summary),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                AnimatedVisibility(
                    visible = uiState.resumedNoticeVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = "Estado restaurado instantaneamente sem recarregamento",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSignal,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. Bottom-Half Ergonomic LockSurface Sandbox Widget
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (uiState.settings.oledPowerSaver) OledPureBlack else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .border(1.dp, tokens.hairlineBorder, RoundedCornerShape(24.dp))
                    .padding(14.dp)
                    .testTag("locksurface_widget_container"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // LockSurface Header: Sandbox label + Quick Switcher + Configurator Unlock trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Sandbox LockSurface",
                                tint = CyanPulse,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "LOCKSURFACE SANDBOX",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPulse,
                            )
                        }

                        IconButton(
                            onClick = { viewModel.requestUnlock(UnlockReason.OPEN_CONFIGURATOR) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_configurator_lock_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Abrir Configurador (Exige Desbloqueio)",
                                tint = tokens.mutedText,
                            )
                        }
                    }

                    if (uiState.settings.allowQuickSwitchOnLock) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            GameId.entries.forEach { gameId ->
                                val selected = uiState.settings.activeGame == gameId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selected) CyanPulse.copy(alpha = 0.14f) else OledPureBlack,
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) CyanPulse else tokens.hairlineBorder,
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .clickable { viewModel.selectGame(gameId) }
                                        .minimumInteractiveComponentSize()
                                        .padding(horizontal = 6.dp, vertical = 8.dp)
                                        .testTag("lock_game_tab_${gameId.name}"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = gameId.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) CyanPulse else tokens.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    // Active Game Viewport
                    val isGameLoopActive = uiState.phase == LockSurfacePhase.PLAYING
                    when (uiState.settings.activeGame) {
                        GameId.MICRO_2048 -> {
                            Micro2048GameView(
                                state = uiState.micro2048,
                                highScore = uiState.stats[GameId.MICRO_2048]?.highScore ?: 0,
                                onSwipe = viewModel::on2048Swipe,
                                onRestart = viewModel::restart2048,
                            )
                        }

                        GameId.ORBITAL_REFLEX -> {
                            OrbitalReflexGameView(
                                state = uiState.orbitalReflex,
                                highScore = uiState.stats[GameId.ORBITAL_REFLEX]?.highScore ?: 0,
                                isLoopActive = isGameLoopActive,
                                isRunning = uiState.orbitalRunning,
                                oledPowerSaver = uiState.settings.oledPowerSaver,
                                onTapCanvas = viewModel::onOrbitalTap,
                                onStepFrame = viewModel::stepOrbital,
                                onRestart = viewModel::restartOrbital,
                            )
                        }

                        GameId.CHROMATIC_MEMORY -> {
                            ChromaticMemoryGameView(
                                state = uiState.chromaticMemory,
                                highScore = uiState.stats[GameId.CHROMATIC_MEMORY]?.highScore ?: 0,
                                onPadTap = viewModel::onChromaticPadTap,
                                onRestart = viewModel::restartChromatic,
                            )
                        }
                    }
                }

                // Inactivity Pause Overlay (12s timeout -> 0fps suspension)
                if (uiState.phase == LockSurfacePhase.PAUSED_INACTIVITY) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(OledPureBlack.copy(alpha = 0.90f))
                            .clickable { viewModel.resumeFromInactivity() }
                            .testTag("inactivity_pause_overlay"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(20.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.paused_inactivity_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = AmberAlert,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Loop de renderização suspenso (0 fps) • Estado salvo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.mutedText,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = { viewModel.resumeFromInactivity() },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Text(stringResource(R.string.paused_inactivity_cta))
                            }
                        }
                    }
                }

                // Anti-Pocket Accidental Touch Guard Overlay
                if (uiState.phase == LockSurfacePhase.POCKET_BLOCKED) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(OledPureBlack.copy(alpha = 0.96f))
                            .border(1.5.dp, AmberAlert, RoundedCornerShape(24.dp))
                            .testTag("pocket_guard_overlay"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(20.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhonelinkLock,
                                contentDescription = null,
                                tint = AmberAlert,
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                text = stringResource(R.string.pocket_blocked_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = AmberAlert,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = stringResource(R.string.pocket_blocked_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.mutedText,
                                textAlign = TextAlign.Center,
                            )
                            if (uiState.sensorReading.simulatedNear) {
                                FilledTonalButton(
                                    onClick = viewModel::toggleSimulatedPocket,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("pocket_clear_sim_button"),
                                ) {
                                    Text(stringResource(R.string.pocket_override_cta))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Bottom System Affordances: Emergency Call (Never Blocked) + Unlock Gate
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(CrimsonGuard.copy(alpha = 0.14f))
                        .border(1.dp, CrimsonGuard.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .clickable { viewModel.showEmergencyDialog(true) }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("emergency_call_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Emergency,
                            contentDescription = "Chamada de Emergência",
                            tint = CrimsonGuard,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.emergency_call),
                            style = MaterialTheme.typography.labelLarge,
                            color = CrimsonGuard,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (uiState.settings.oledPowerSaver) OledPureBlack else MaterialTheme.colorScheme.surface,
                        )
                        .border(1.dp, tokens.hairlineBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.requestUnlock(UnlockReason.BOTTOM_SWIPE_EXIT) }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("bottom_unlock_bar"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Desbloquear / Configurador",
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = CyanPulse,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Simulated Screen Off Overlay (Pure Black #000000)
        if (uiState.phase == LockSurfacePhase.SCREEN_OFF) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OledPureBlack)
                    .clickable { viewModel.simulateScreenOn() }
                    .testTag("screen_off_overlay"),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = null,
                        tint = CyanPulse,
                        modifier = Modifier.size(42.dp),
                    )
                    Text(
                        text = "TELA APAGADA (ESTADO SALVO)",
                        style = MaterialTheme.typography.titleLarge,
                        color = PureWhite,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Partida gravada atomicamente no sandbox local.\nToque para acender e retomar sem recarregamento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.mutedText,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = viewModel::simulateScreenOn,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("wake_screen_button"),
                    ) {
                        Text(stringResource(R.string.sim_screen_on))
                    }
                }
            }
        }

        // Unlock Gate Overlay (PIN / Biometric)
        if (uiState.phase == LockSurfacePhase.UNLOCK_REQUIRED) {
            UnlockGateOverlay(
                reason = uiState.unlockReason,
                pinError = uiState.pinError,
                onSubmitPin = viewModel::verifyPinAndUnlock,
                onBiometricUnlock = viewModel::unlockWithBiometricsDemo,
                onCancel = viewModel::cancelUnlockBackToLock,
            )
        }

        // Non-blocking Emergency Call Verification Dialog
        if (uiState.emergencyDialogVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.showEmergencyDialog(false) },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "Chamada de Emergência",
                        color = CrimsonGuard,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Text(
                        text = "O LockSurface nunca bloqueia ou atrasa discagem de emergência (190 / 192 / 112). Em um aparelho real, esta ação abre imediatamente o discador de emergência do sistema.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.showEmergencyDialog(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonGuard),
                    ) {
                        Text("Entendido")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showEmergencyDialog(false) }) {
                        Text("Fechar")
                    }
                },
            )
        }
    }
}

@Composable
private fun SimulatorControlStrip(
    oledPowerSaver: Boolean,
    silentMode: Boolean,
    simulatedPocket: Boolean,
    onToggleOled: () -> Unit,
    onToggleSilent: () -> Unit,
    onTogglePocket: () -> Unit,
    onSimulateScreenOff: () -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SimChipButton(
            icon = Icons.Rounded.Contrast,
            label = if (oledPowerSaver) "OLED" else "60fps",
            active = oledPowerSaver,
            activeColor = CyanPulse,
            onClick = onToggleOled,
            tag = "sim_toggle_oled",
            modifier = Modifier.weight(1f),
        )
        SimChipButton(
            icon = Icons.Rounded.PowerSettingsNew,
            label = "Apagar",
            active = false,
            activeColor = EmeraldSignal,
            onClick = onSimulateScreenOff,
            tag = "sim_screen_off_btn",
            modifier = Modifier.weight(1f),
        )
        SimChipButton(
            icon = Icons.Rounded.Sensors,
            label = "Bolso",
            active = simulatedPocket,
            activeColor = AmberAlert,
            onClick = onTogglePocket,
            tag = "sim_pocket_btn",
            modifier = Modifier.weight(1f),
        )
        SimChipButton(
            icon = if (silentMode) Icons.Rounded.VolumeOff else Icons.Rounded.Vibration,
            label = if (silentMode) "Mudo" else "Táteis",
            active = silentMode,
            activeColor = AmberAlert,
            onClick = onToggleSilent,
            tag = "sim_silent_btn",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SimChipButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalTelaJogoTokens.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) activeColor.copy(alpha = 0.16f) else OledPureBlack)
            .border(
                width = 1.dp,
                color = if (active) activeColor else tokens.hairlineBorder,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) activeColor else tokens.mutedText,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) activeColor else PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PhaseTelemetryBadge(
    phase: LockSurfacePhase,
    inactivityRemainingSec: Int,
    oledPowerSaver: Boolean,
) {
    val tokens = LocalTelaJogoTokens.current
    val badgeColor = when (phase) {
        LockSurfacePhase.LOCKED -> CyanPulse
        LockSurfacePhase.PLAYING -> EmeraldSignal
        LockSurfacePhase.PAUSED_INACTIVITY, LockSurfacePhase.POCKET_BLOCKED -> AmberAlert
        LockSurfacePhase.UNLOCK_REQUIRED -> CrimsonGuard
        else -> tokens.mutedText
    }
    val text = when (phase) {
        LockSurfacePhase.PLAYING -> "${phase.label} • ${inactivityRemainingSec}s"
        else -> if (oledPowerSaver) "${phase.label} • OLED" else phase.label
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, badgeColor.copy(alpha = 0.65f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(badgeColor),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
        )
    }
}

private fun formatTimeNow(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun formatDateNow(): String =
    SimpleDateFormat("EEE, dd MMM", Locale("pt", "BR")).format(Date())

private fun readBatteryPercentage(context: Context): Int = runCatching {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 84
    if (level in 1..100) level else 84
}.getOrDefault(84)
