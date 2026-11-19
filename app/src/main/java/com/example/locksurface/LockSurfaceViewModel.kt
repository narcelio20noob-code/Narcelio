package com.example.locksurface

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.feedback.HapticFeedbackEngine
import com.example.core.model.GameId
import com.example.core.model.GameStats
import com.example.core.model.LockSurfacePhase
import com.example.core.model.PocketSensorReading
import com.example.core.model.TelaJogoSettings
import com.example.core.model.UnlockReason
import com.example.core.sandbox.LockSandboxStore
import com.example.core.sandbox.PocketGuardSensorManager
import com.example.games.ChromaticMemoryEngine
import com.example.games.ChromaticMemoryState
import com.example.games.MemoryPhase
import com.example.games.Micro2048Engine
import com.example.games.Micro2048State
import com.example.games.OrbitalReflexEngine
import com.example.games.OrbitalReflexState
import com.example.games.SwipeDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockSurfaceUiState(
    val phase: LockSurfacePhase = LockSurfacePhase.LOCKED,
    val settings: TelaJogoSettings = TelaJogoSettings(),
    val stats: Map<GameId, GameStats> = GameId.entries.associateWith { GameStats() },
    val sensorReading: PocketSensorReading = PocketSensorReading(),
    val micro2048: Micro2048State = Micro2048Engine.createNewGame(),
    val orbitalReflex: OrbitalReflexState = OrbitalReflexEngine.createNewGame(),
    val chromaticMemory: ChromaticMemoryState = ChromaticMemoryEngine.createNewGame(),
    val inactivityRemainingSec: Int = 12,
    val unlockReason: UnlockReason? = null,
    val pinError: String? = null,
    val emergencyDialogVisible: Boolean = false,
    val lastSavedTimeLabel: String = "Sincronizado localmente",
    val orbitalRunning: Boolean = false,
    val sessionSecondsActive: Int = 0,
    val resumedNoticeVisible: Boolean = false,
)

class LockSurfaceViewModel(application: Application) : AndroidViewModel(application) {

    private val sandboxStore = LockSandboxStore(application)
    private val haptics = HapticFeedbackEngine(application)
    private val pocketSensorManager = PocketGuardSensorManager(application)

    private val _uiState = MutableStateFlow(LockSurfaceUiState())
    val uiState: StateFlow<LockSurfaceUiState> = _uiState.asStateFlow()

    private var inactivityJob: Job? = null
    private var chromaticPlaybackJob: Job? = null
    private var sessionTickerJob: Job? = null
    private var phaseBeforePocketOrScreenOff: LockSurfacePhase = LockSurfacePhase.LOCKED
    private var orbitalSaveTickCounter = 0

    init {
        val loadedSettings = sandboxStore.loadSettings()
        val loadedStats = sandboxStore.loadAllStats()
        val restored2048 = sandboxStore.loadGamePayload(GameId.MICRO_2048)
            ?.let { Micro2048Engine.fromJson(it) }
            ?: Micro2048Engine.createNewGame()
        val restoredOrbital = sandboxStore.loadGamePayload(GameId.ORBITAL_REFLEX)
            ?.let { OrbitalReflexEngine.fromJson(it) }
            ?: OrbitalReflexEngine.createNewGame()
        val restoredChromatic = sandboxStore.loadGamePayload(GameId.CHROMATIC_MEMORY)
            ?.let { ChromaticMemoryEngine.fromJson(it) }
            ?: ChromaticMemoryEngine.createNewGame()

        haptics.isSilentMode = loadedSettings.silentMode

        _uiState.value = LockSurfaceUiState(
            phase = LockSurfacePhase.LOCKED,
            settings = loadedSettings,
            stats = loadedStats,
            micro2048 = restored2048,
            orbitalReflex = restoredOrbital,
            chromaticMemory = restoredChromatic,
            inactivityRemainingSec = loadedSettings.inactivityTimeoutSec,
        )

        viewModelScope.launch {
            pocketSensorManager.reading.collect { reading ->
                onSensorReadingUpdated(reading)
            }
        }
    }

    fun startSensors() {
        pocketSensorManager.start()
    }

    fun stopSensorsAndPersist() {
        pocketSensorManager.stop()
        persistAllGames(immediate = true)
    }

    private fun onSensorReadingUpdated(reading: PocketSensorReading) {
        val current = _uiState.value
        val guardActive = current.settings.pocketGuardEnabled && reading.isBlockedInPocket

        if (guardActive &&
            current.phase != LockSurfacePhase.POCKET_BLOCKED &&
            current.phase != LockSurfacePhase.SCREEN_OFF &&
            current.phase != LockSurfacePhase.CONFIGURATOR
        ) {
            phaseBeforePocketOrScreenOff = current.phase
            cancelActiveLoops()
            persistAllGames(immediate = true)
            _uiState.update {
                it.copy(
                    sensorReading = reading,
                    phase = LockSurfacePhase.POCKET_BLOCKED,
                    orbitalRunning = false,
                )
            }
        } else if (!guardActive && current.phase == LockSurfacePhase.POCKET_BLOCKED) {
            val restorePhase = if (phaseBeforePocketOrScreenOff == LockSurfacePhase.PLAYING) {
                LockSurfacePhase.PAUSED_INACTIVITY
            } else {
                LockSurfacePhase.LOCKED
            }
            _uiState.update {
                it.copy(
                    sensorReading = reading,
                    phase = restorePhase,
                    inactivityRemainingSec = it.settings.inactivityTimeoutSec,
                )
            }
        } else {
            _uiState.update { it.copy(sensorReading = reading) }
        }
    }

    fun toggleSimulatedPocket() {
        val next = !_uiState.value.sensorReading.simulatedNear
        pocketSensorManager.setSimulatedPocket(next)
    }

    fun toggleOledPowerSaver() {
        val updated = _uiState.value.settings.copy(
            oledPowerSaver = !_uiState.value.settings.oledPowerSaver,
        )
        sandboxStore.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
        haptics.tapLight()
    }

    fun toggleSilentMode() {
        val nextSilent = !_uiState.value.settings.silentMode
        val updated = _uiState.value.settings.copy(silentMode = nextSilent)
        haptics.isSilentMode = nextSilent
        sandboxStore.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
        if (!nextSilent) haptics.tapLight()
    }

    fun togglePocketGuardEnabled() {
        val updated = _uiState.value.settings.copy(
            pocketGuardEnabled = !_uiState.value.settings.pocketGuardEnabled,
        )
        sandboxStore.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
        onSensorReadingUpdated(_uiState.value.sensorReading)
    }

    fun toggleQuickSwitchOnLock() {
        val updated = _uiState.value.settings.copy(
            allowQuickSwitchOnLock = !_uiState.value.settings.allowQuickSwitchOnLock,
        )
        sandboxStore.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun updateInactivityTimeout(seconds: Int) {
        val clamped = seconds.coerceIn(6, 30)
        val updated = _uiState.value.settings.copy(inactivityTimeoutSec = clamped)
        sandboxStore.saveSettings(updated)
        _uiState.update {
            it.copy(
                settings = updated,
                inactivityRemainingSec = clamped,
            )
        }
    }

    fun selectGame(gameId: GameId) {
        if (_uiState.value.settings.activeGame == gameId) return
        cancelActiveLoops()
        persistAllGames(immediate = true)
        val updated = _uiState.value.settings.copy(activeGame = gameId)
        sandboxStore.saveSettings(updated)
        haptics.tapLight()
        _uiState.update {
            it.copy(
                settings = updated,
                orbitalRunning = false,
                inactivityRemainingSec = updated.inactivityTimeoutSec,
                phase = if (it.phase == LockSurfacePhase.CONFIGURATOR) {
                    LockSurfacePhase.CONFIGURATOR
                } else {
                    LockSurfacePhase.LOCKED
                },
            )
        }
    }

    fun simulateScreenOff() {
        val current = _uiState.value
        if (current.phase == LockSurfacePhase.SCREEN_OFF) return
        phaseBeforePocketOrScreenOff = current.phase
        cancelActiveLoops()
        persistAllGames(immediate = true)
        _uiState.update {
            it.copy(
                phase = LockSurfacePhase.SCREEN_OFF,
                orbitalRunning = false,
                resumedNoticeVisible = false,
            )
        }
    }

    fun simulateScreenOn() {
        val current = _uiState.value
        if (current.phase != LockSurfacePhase.SCREEN_OFF) return
        val reading = current.sensorReading
        val blocked = current.settings.pocketGuardEnabled && reading.isBlockedInPocket
        val nextPhase = when {
            blocked -> LockSurfacePhase.POCKET_BLOCKED
            phaseBeforePocketOrScreenOff == LockSurfacePhase.CONFIGURATOR -> LockSurfacePhase.LOCKED
            else -> LockSurfacePhase.LOCKED
        }
        _uiState.update {
            it.copy(
                phase = nextPhase,
                inactivityRemainingSec = it.settings.inactivityTimeoutSec,
                resumedNoticeVisible = true,
            )
        }
        viewModelScope.launch {
            delay(2200L)
            _uiState.update { it.copy(resumedNoticeVisible = false) }
        }
    }

    fun resumeFromInactivity() {
        val current = _uiState.value
        if (current.phase != LockSurfacePhase.PAUSED_INACTIVITY &&
            current.phase != LockSurfacePhase.LOCKED
        ) {
            return
        }
        if (isBlockedByPocket()) return
        enterPlayingPhase()
    }

    fun requestUnlock(reason: UnlockReason) {
        val current = _uiState.value
        if (current.phase == LockSurfacePhase.SCREEN_OFF) return
        cancelActiveLoops()
        persistAllGames(immediate = true)
        haptics.unlockReject()
        _uiState.update {
            it.copy(
                phase = LockSurfacePhase.UNLOCK_REQUIRED,
                unlockReason = reason,
                pinError = null,
                orbitalRunning = false,
            )
        }
    }

    fun cancelUnlockBackToLock() {
        _uiState.update {
            it.copy(
                phase = LockSurfacePhase.LOCKED,
                unlockReason = null,
                pinError = null,
                inactivityRemainingSec = it.settings.inactivityTimeoutSec,
            )
        }
    }

    fun verifyPinAndUnlock(pin: String) {
        if (pin == DEMO_PIN) {
            haptics.scorePulse()
            _uiState.update {
                it.copy(
                    phase = LockSurfacePhase.CONFIGURATOR,
                    unlockReason = null,
                    pinError = null,
                )
            }
        } else {
            haptics.collisionHeavy()
            _uiState.update {
                it.copy(pinError = "PIN incorreto. Use 1234 para demonstração.")
            }
        }
    }

    fun unlockWithBiometricsDemo() {
        haptics.scorePulse()
        _uiState.update {
            it.copy(
                phase = LockSurfacePhase.CONFIGURATOR,
                unlockReason = null,
                pinError = null,
            )
        }
    }

    fun lockDeviceBackToSimulator() {
        cancelActiveLoops()
        persistAllGames(immediate = true)
        _uiState.update {
            it.copy(
                phase = LockSurfacePhase.LOCKED,
                inactivityRemainingSec = it.settings.inactivityTimeoutSec,
                orbitalRunning = false,
            )
        }
    }

    fun showEmergencyDialog(visible: Boolean) {
        _uiState.update { it.copy(emergencyDialogVisible = visible) }
    }

    // --- 2048 Micro ---

    fun on2048Swipe(direction: SwipeDirection) {
        if (isBlockedByPocket()) return
        markUserInteraction()
        val current = _uiState.value.micro2048
        val (nextState, moved) = Micro2048Engine.move(current, direction)
        if (!moved) return

        if (nextState.lastMergedIndices.isNotEmpty()) {
            haptics.mergeTick()
        } else {
            haptics.tapLight()
        }

        _uiState.update { it.copy(micro2048 = nextState) }
        sandboxStore.saveGamePayload(
            GameId.MICRO_2048,
            Micro2048Engine.toJson(nextState),
            immediate = false,
        )

        if (nextState.isGameOver) {
            haptics.collisionHeavy()
            recordCompletedRound(GameId.MICRO_2048, nextState.score)
        }
    }

    fun restart2048() {
        if (isBlockedByPocket()) return
        markUserInteraction()
        val fresh = Micro2048Engine.createNewGame()
        _uiState.update { it.copy(micro2048 = fresh) }
        sandboxStore.saveGamePayload(GameId.MICRO_2048, Micro2048Engine.toJson(fresh), immediate = true)
        haptics.tapLight()
    }

    // --- Reflexo Orbital ---

    fun onOrbitalTap() {
        if (isBlockedByPocket()) return
        val current = _uiState.value
        if (current.orbitalReflex.isGameOver) {
            restartOrbital()
            return
        }
        markUserInteraction()
        if (!current.orbitalRunning) {
            _uiState.update { it.copy(orbitalRunning = true) }
            haptics.tapLight()
            return
        }
        val flipped = OrbitalReflexEngine.flipLane(current.orbitalReflex)
        _uiState.update { it.copy(orbitalReflex = flipped) }
        haptics.tapLight()
    }

    fun stepOrbital(deltaSec: Float) {
        val current = _uiState.value
        if (current.phase != LockSurfacePhase.PLAYING || !current.orbitalRunning) return
        val result = OrbitalReflexEngine.step(current.orbitalReflex, deltaSec)
        if (result.collectedOrb) {
            haptics.scorePulse()
        }
        if (result.collided) {
            haptics.collisionHeavy()
            _uiState.update {
                it.copy(
                    orbitalReflex = result.state,
                    orbitalRunning = false,
                )
            }
            sandboxStore.saveGamePayload(
                GameId.ORBITAL_REFLEX,
                OrbitalReflexEngine.toJson(result.state),
                immediate = true,
            )
            recordCompletedRound(GameId.ORBITAL_REFLEX, result.state.score)
            return
        }

        _uiState.update { it.copy(orbitalReflex = result.state) }
        orbitalSaveTickCounter++
        if (orbitalSaveTickCounter >= 30) {
            orbitalSaveTickCounter = 0
            sandboxStore.saveGamePayload(
                GameId.ORBITAL_REFLEX,
                OrbitalReflexEngine.toJson(result.state),
                immediate = false,
            )
        }
    }

    fun restartOrbital() {
        if (isBlockedByPocket()) return
        markUserInteraction()
        val fresh = OrbitalReflexEngine.createNewGame()
        _uiState.update { it.copy(orbitalReflex = fresh, orbitalRunning = true) }
        sandboxStore.saveGamePayload(
            GameId.ORBITAL_REFLEX,
            OrbitalReflexEngine.toJson(fresh),
            immediate = true,
        )
        haptics.tapLight()
    }

    // --- Memória Cromática ---

    fun startOrResumeChromaticSequence() {
        if (isBlockedByPocket()) return
        val state = _uiState.value.chromaticMemory
        if (state.phase == MemoryPhase.GAME_OVER) {
            restartChromatic()
            return
        }
        markUserInteraction()
        if (state.phase == MemoryPhase.SHOWING_SEQUENCE) {
            launchChromaticPlayback()
        }
    }

    fun onChromaticPadTap(padIndex: Int) {
        if (isBlockedByPocket()) return
        val current = _uiState.value.chromaticMemory
        if (current.phase == MemoryPhase.GAME_OVER) {
            restartChromatic()
            return
        }
        if (current.phase == MemoryPhase.SHOWING_SEQUENCE) {
            markUserInteraction()
            if (chromaticPlaybackJob?.isActive != true) {
                launchChromaticPlayback()
            }
            return
        }

        markUserInteraction()
        val tapResult = ChromaticMemoryEngine.submitTap(current, padIndex)
        if (!tapResult.accepted) return

        _uiState.update { it.copy(chromaticMemory = tapResult.state) }
        sandboxStore.saveGamePayload(
            GameId.CHROMATIC_MEMORY,
            ChromaticMemoryEngine.toJson(tapResult.state),
            immediate = tapResult.failed || tapResult.roundCompleted,
        )

        // Brief flash cleanup without blocking rapid next taps
        viewModelScope.launch {
            delay(160L)
            _uiState.update { latest ->
                if (latest.chromaticMemory.activeLitPad == padIndex &&
                    latest.chromaticMemory.phase == MemoryPhase.AWAITING_INPUT
                ) {
                    latest.copy(
                        chromaticMemory = ChromaticMemoryEngine.setLitPad(latest.chromaticMemory, null),
                    )
                } else {
                    latest
                }
            }
        }

        when {
            tapResult.failed -> {
                haptics.collisionHeavy()
                recordCompletedRound(GameId.CHROMATIC_MEMORY, tapResult.state.score)
            }

            tapResult.roundCompleted -> {
                haptics.scorePulse()
                chromaticPlaybackJob?.cancel()
                chromaticPlaybackJob = viewModelScope.launch {
                    delay(480L)
                    val latest = _uiState.value
                    if (latest.phase == LockSurfacePhase.PLAYING &&
                        latest.settings.activeGame == GameId.CHROMATIC_MEMORY
                    ) {
                        val nextRoundState = ChromaticMemoryEngine.advanceNextRound(latest.chromaticMemory)
                        _uiState.update { it.copy(chromaticMemory = nextRoundState) }
                        sandboxStore.saveGamePayload(
                            GameId.CHROMATIC_MEMORY,
                            ChromaticMemoryEngine.toJson(nextRoundState),
                            immediate = false,
                        )
                        playSequenceSteps()
                    }
                }
            }

            else -> haptics.tapLight()
        }
    }

    fun restartChromatic() {
        if (isBlockedByPocket()) return
        chromaticPlaybackJob?.cancel()
        markUserInteraction()
        val fresh = ChromaticMemoryEngine.createNewGame()
        _uiState.update { it.copy(chromaticMemory = fresh) }
        sandboxStore.saveGamePayload(
            GameId.CHROMATIC_MEMORY,
            ChromaticMemoryEngine.toJson(fresh),
            immediate = true,
        )
        haptics.tapLight()
        launchChromaticPlayback()
    }

    private fun launchChromaticPlayback() {
        chromaticPlaybackJob?.cancel()
        chromaticPlaybackJob = viewModelScope.launch {
            playSequenceSteps()
        }
    }

    private suspend fun playSequenceSteps() {
        delay(260L)
        val seq = _uiState.value.chromaticMemory.sequence
        for (pad in seq) {
            if (_uiState.value.phase != LockSurfacePhase.PLAYING ||
                _uiState.value.settings.activeGame != GameId.CHROMATIC_MEMORY
            ) {
                return
            }
            _uiState.update {
                it.copy(chromaticMemory = ChromaticMemoryEngine.setLitPad(it.chromaticMemory, pad))
            }
            haptics.tapLight()
            delay(340L)
            _uiState.update {
                it.copy(chromaticMemory = ChromaticMemoryEngine.setLitPad(it.chromaticMemory, null))
            }
            delay(150L)
        }
        _uiState.update {
            it.copy(chromaticMemory = ChromaticMemoryEngine.startInputPhase(it.chromaticMemory))
        }
    }

    // --- State & Lifecycle Helpers ---

    private fun isBlockedByPocket(): Boolean {
        val current = _uiState.value
        return current.settings.pocketGuardEnabled && current.sensorReading.isBlockedInPocket
    }

    private fun markUserInteraction() {
        val current = _uiState.value
        if (current.phase == LockSurfacePhase.LOCKED ||
            current.phase == LockSurfacePhase.PAUSED_INACTIVITY
        ) {
            enterPlayingPhase()
        } else if (current.phase == LockSurfacePhase.PLAYING) {
            restartInactivityWatchdog()
        }
    }

    private fun enterPlayingPhase() {
        val timeout = _uiState.value.settings.inactivityTimeoutSec
        _uiState.update {
            it.copy(
                phase = LockSurfacePhase.PLAYING,
                inactivityRemainingSec = timeout,
            )
        }
        restartInactivityWatchdog()
        startSessionTickerIfNeeded()
        if (_uiState.value.settings.activeGame == GameId.CHROMATIC_MEMORY &&
            _uiState.value.chromaticMemory.phase == MemoryPhase.SHOWING_SEQUENCE
        ) {
            launchChromaticPlayback()
        }
    }

    private fun restartInactivityWatchdog() {
        inactivityJob?.cancel()
        val timeout = _uiState.value.settings.inactivityTimeoutSec
        _uiState.update { it.copy(inactivityRemainingSec = timeout) }
        inactivityJob = viewModelScope.launch {
            var remaining = timeout
            while (remaining > 0) {
                delay(1000L)
                if (_uiState.value.phase != LockSurfacePhase.PLAYING) return@launch
                remaining -= 1
                _uiState.update { it.copy(inactivityRemainingSec = remaining) }
            }
            if (_uiState.value.phase == LockSurfacePhase.PLAYING) {
                cancelActiveLoops()
                persistAllGames(immediate = true)
                _uiState.update {
                    it.copy(
                        phase = LockSurfacePhase.PAUSED_INACTIVITY,
                        orbitalRunning = false,
                    )
                }
            }
        }
    }

    private fun startSessionTickerIfNeeded() {
        if (sessionTickerJob?.isActive == true) return
        sessionTickerJob = viewModelScope.launch {
            while (_uiState.value.phase == LockSurfacePhase.PLAYING) {
                delay(1000L)
                if (_uiState.value.phase == LockSurfacePhase.PLAYING) {
                    _uiState.update { it.copy(sessionSecondsActive = it.sessionSecondsActive + 1) }
                }
            }
        }
    }

    private fun recordCompletedRound(gameId: GameId, score: Int) {
        val duration = _uiState.value.sessionSecondsActive.coerceAtLeast(3)
        val updatedStats = sandboxStore.recordRound(gameId, score, duration)
        _uiState.update {
            it.copy(
                stats = updatedStats,
                sessionSecondsActive = 0,
            )
        }
    }

    private fun cancelActiveLoops() {
        inactivityJob?.cancel()
        chromaticPlaybackJob?.cancel()
        sessionTickerJob?.cancel()
    }

    fun persistAllGames(immediate: Boolean = true) {
        val current = _uiState.value
        sandboxStore.saveGamePayload(
            GameId.MICRO_2048,
            Micro2048Engine.toJson(current.micro2048),
            immediate = immediate,
        )
        sandboxStore.saveGamePayload(
            GameId.ORBITAL_REFLEX,
            OrbitalReflexEngine.toJson(current.orbitalReflex),
            immediate = immediate,
        )
        sandboxStore.saveGamePayload(
            GameId.CHROMATIC_MEMORY,
            ChromaticMemoryEngine.toJson(current.chromaticMemory),
            immediate = immediate,
        )
        sandboxStore.saveLastSavedTimestamp(System.currentTimeMillis())
    }

    override fun onCleared() {
        cancelActiveLoops()
        pocketSensorManager.stop()
        persistAllGames(immediate = true)
        super.onCleared()
    }

    companion object {
        const val DEMO_PIN = "1234"
    }
}
