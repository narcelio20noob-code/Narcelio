package com.example.core.model

enum class GameId(
    val title: String,
    val subtitle: String,
    val durationLabel: String,
) {
    MICRO_2048(
        title = "2048 Micro",
        subtitle = "Grade 3×3 • Polegar",
        durationLabel = "20–45s",
    ),
    ORBITAL_REFLEX(
        title = "Reflexo Orbital",
        subtitle = "1 toque • Ciclo contínuo",
        durationLabel = "15–30s",
    ),
    CHROMATIC_MEMORY(
        title = "Memória Cromática",
        subtitle = "4 símbolos • Tátil",
        durationLabel = "15–40s",
    ),
}

enum class LockSurfacePhase(val label: String) {
    LOCKED("BLOQUEADO"),
    PLAYING("JOGANDO"),
    PAUSED_INACTIVITY("PAUSADO (12s)"),
    POCKET_BLOCKED("BOLSO DETECTADO"),
    SCREEN_OFF("TELA APAGADA"),
    UNLOCK_REQUIRED("DESBLOQUEIO EXIGIDO"),
    CONFIGURATOR("APP CONFIGURADOR"),
}

enum class UnlockReason(val message: String) {
    OPEN_CONFIGURATOR("Abrir App Configurador"),
    MASKED_NOTIFICATION_TAP("Notificação protegida — desbloqueie para visualizar"),
    BOTTOM_SWIPE_EXIT("Saída do sandbox LockSurface"),
}

data class TelaJogoSettings(
    val activeGame: GameId = GameId.MICRO_2048,
    val oledPowerSaver: Boolean = true,
    val silentMode: Boolean = false,
    val pocketGuardEnabled: Boolean = true,
    val inactivityTimeoutSec: Int = 12,
    val allowQuickSwitchOnLock: Boolean = true,
)

data class GameStats(
    val highScore: Int = 0,
    val roundsPlayed: Int = 0,
    val totalSecondsPlayed: Int = 0,
)

data class PocketSensorReading(
    val hasHardwareProximity: Boolean = false,
    val hardwareNear: Boolean = false,
    val simulatedNear: Boolean = false,
    val lastAccelMagnitude: Float = 9.81f,
) {
    val isBlockedInPocket: Boolean
        get() = hardwareNear || simulatedNear
}
