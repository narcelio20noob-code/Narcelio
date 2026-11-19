package com.example.configurator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.locksurface.LockSurfaceUiState
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.LocalTelaJogoTokens
import com.example.ui.theme.OledPureBlack
import com.example.ui.theme.PureWhite

@Composable
fun ArchitectureSpecScreen(
    uiState: LockSurfaceUiState,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalTelaJogoTokens.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpecCard(
            title = "FLUXO DE ESTADOS DO LOCKSURFACE",
            accent = CyanPulse,
            body = """
                1. BLOQUEADO (LOCKED)
                   ↳ Relógio/notificações ocultas no topo; LockSurface na metade inferior.
                   ↳ SensorManager verifica proximidade antes do 1º toque.
                2. JOGANDO (PLAYING)
                   ↳ Loop adaptativo (${if (uiState.settings.oledPowerSaver) "30 fps OLED" else "60 fps"}).
                   ↳ Salvamento atômico contínuo em sandbox local criptografado.
                3. PAUSADO POR INATIVIDADE (12s) / BOLSO / TELA APAGADA
                   ↳ Cancelamento imediato de coroutines/timers (0 fps).
                   ↳ Retomada instantânea sem recarregamento ao acender a tela.
                4. DESBLOQUEIO EXIGIDO (UNLOCK_REQUIRED)
                   ↳ Tocar fora do widget (notificação ou barra inferior) aciona PIN/Biometria.
            """.trimIndent(),
        )

        SpecCard(
            title = "ARQUITETURA MULTIPLATAFORMA",
            accent = EmeraldSignal,
            body = """
                TelaJogo/
                ├── shared/ (Regra de negócio & engines determinísticos)
                │   ├── Micro2048Engine (3×3 polegar)
                │   ├── OrbitalReflexEngine (1 toque contínuo)
                │   └── ChromaticMemoryEngine (4 símbolos táteis)
                ├── android/ (Implementado neste APK)
                │   ├── locksurface/ (LockSurfaceViewModel + Simulator + UnlockGate)
                │   ├── core/sandbox/LockSandboxStore (AES-256-GCM Keystore local)
                │   ├── core/sandbox/PocketGuardSensorManager (Proximidade + Acel.)
                │   └── configurator/ (App Configurador pós-desbloqueio)
                └── ios/ (Módulo nativo equivalente)
                    ├── LockWidgetExtension/ (WidgetKit + Live Activities)
                    ├── AppIntents/ (Ações táteis sem abrir o app principal)
                    └── AppGroupStore/ (group.com.telajogo.sandbox compartilhado)
            """.trimIndent(),
        )

        SpecCard(
            title = "POLÍTICA ZERO VAZAMENTO (SANDBOX)",
            accent = AmberAlert,
            body = """
                • Proibido ler notificações, contatos, câmera ou galeria.
                • Nenhuma chamada de rede ou telemetria externa no LockSurface.
                • Emergência (190/192/112) nunca é bloqueada nem atrasada.
                • Limpeza automática de coroutines e SensorManager no onCleared/onStop.
            """.trimIndent(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(OledPureBlack)
                .border(1.dp, tokens.hairlineBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = EmeraldSignal,
            )
            Text(
                text = "Sandbox ativo • ${uiState.lastSavedTimeLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SpecCard(
    title: String,
    accent: androidx.compose.ui.graphics.Color,
    body: String,
) {
    val tokens = LocalTelaJogoTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tokens.hairlineBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
        )
    }
}
