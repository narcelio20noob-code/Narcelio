package com.example.configurator

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.model.GameId
import com.example.core.model.GameStats
import com.example.locksurface.LockSurfaceUiState
import com.example.locksurface.LockSurfaceViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.LocalTelaJogoTokens
import com.example.ui.theme.OledPureBlack
import com.example.ui.theme.PureWhite
import kotlin.math.roundToInt

@Composable
fun ConfiguratorScreen(
    uiState: LockSurfaceUiState,
    viewModel: LockSurfaceViewModel,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalTelaJogoTokens.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (uiState.settings.oledPowerSaver) OledPureBlack else MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = if (uiState.settings.oledPowerSaver) OledPureBlack else MaterialTheme.colorScheme.surface,
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text("Configurador") },
                    modifier = Modifier.testTag("tab_configurator"),
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.AccountTree, contentDescription = null) },
                    label = { Text("Arquitetura") },
                    modifier = Modifier.testTag("tab_architecture"),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar with Lock / Test Simulator CTA
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.configurator_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite,
                    )
                    Text(
                        text = "Área desbloqueada • Estatísticas & OLED",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.mutedText,
                    )
                }
                Button(
                    onClick = viewModel::lockDeviceBackToSimulator,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("lock_and_test_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhonelinkLock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Bloquear Tela")
                }
            }

            if (selectedTab == 0) {
                ConfiguratorContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                ArchitectureSpecScreen(
                    uiState = uiState,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ConfiguratorContent(
    uiState: LockSurfaceUiState,
    viewModel: LockSurfaceViewModel,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalTelaJogoTokens.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "CATÁLOGO DE MINIJOGOS DE BLOQUEIO",
            style = MaterialTheme.typography.labelLarge,
            color = CyanPulse,
        )

        GameId.entries.forEach { gameId ->
            val selected = uiState.settings.activeGame == gameId
            val stats = uiState.stats[gameId] ?: GameStats()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (selected) CyanPulse.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                    )
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) CyanPulse else tokens.hairlineBorder,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickable { viewModel.selectGame(gameId) }
                    .minimumInteractiveComponentSize()
                    .padding(14.dp)
                    .testTag("config_game_card_${gameId.name}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Games,
                            contentDescription = null,
                            tint = if (selected) CyanPulse else tokens.mutedText,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = gameId.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite,
                        )
                        Text(
                            text = gameId.durationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSignal,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = gameId.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.mutedText,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Recorde: ${stats.highScore} • Partidas: ${stats.roundsPlayed} • Tempo: ${stats.totalSecondsPlayed}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                    )
                }

                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selecionado",
                        tint = CyanPulse,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ENERGIA OLED, SENSORES E ERGONOMIA",
            style = MaterialTheme.typography.labelLarge,
            color = CyanPulse,
        )

        SettingToggleCard(
            icon = Icons.Rounded.Contrast,
            title = "Modo OLED Preto Puro (#000000)",
            subtitle = "Fundo com pixels apagados + limite adaptativo de 30 fps",
            checked = uiState.settings.oledPowerSaver,
            onCheckedChange = { viewModel.toggleOledPowerSaver() },
            tag = "config_toggle_oled",
        )

        SettingToggleCard(
            icon = Icons.Rounded.Sensors,
            title = "Proteção Anti-Bolso (Proximidade)",
            subtitle = if (uiState.sensorReading.hasHardwareProximity) {
                "Sensor físico ativo • Bloqueia primeiro toque no bolso"
            } else {
                "Modo emulador/fallback • Simulador de bolso habilitado"
            },
            checked = uiState.settings.pocketGuardEnabled,
            onCheckedChange = { viewModel.togglePocketGuardEnabled() },
            tag = "config_toggle_pocket",
        )

        SettingToggleCard(
            icon = Icons.Rounded.Vibration,
            title = "Modo Silencioso (Sem Vibração)",
            subtitle = "Desativa micro-hápticos em transportes ou salas de espera",
            checked = uiState.settings.silentMode,
            onCheckedChange = { viewModel.toggleSilentMode() },
            tag = "config_toggle_silent",
        )

        SettingToggleCard(
            icon = Icons.Rounded.Lock,
            title = "Troca Rápida no LockSurface",
            subtitle = "Permite alternar entre os 3 jogos na tela bloqueada",
            checked = uiState.settings.allowQuickSwitchOnLock,
            onCheckedChange = { viewModel.toggleQuickSwitchOnLock() },
            tag = "config_toggle_quickswitch",
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, tokens.hairlineBorder, RoundedCornerShape(18.dp))
                .padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = AmberAlert,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Pausa Automática por Inatividade: ${uiState.settings.inactivityTimeoutSec}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                )
            }
            Slider(
                value = uiState.settings.inactivityTimeoutSec.toFloat(),
                onValueChange = { viewModel.updateInactivityTimeout(it.roundToInt()) },
                valueRange = 6f..30f,
                steps = 7,
            )
            Text(
                text = "Padrão recomendado: 12 segundos sem toque suspendem o loop para 0 fps.",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.mutedText,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingToggleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    val tokens = LocalTelaJogoTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tokens.hairlineBorder, RoundedCornerShape(18.dp))
            .clickable { onCheckedChange(!checked) }
            .minimumInteractiveComponentSize()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) CyanPulse else tokens.mutedText,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.mutedText,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
