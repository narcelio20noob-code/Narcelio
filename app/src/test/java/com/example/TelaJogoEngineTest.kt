package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.GameId
import com.example.core.model.TelaJogoSettings
import com.example.core.sandbox.LockSandboxStore
import com.example.games.ChromaticMemoryEngine
import com.example.games.MemoryPhase
import com.example.games.Micro2048Engine
import com.example.games.Micro2048State
import com.example.games.OrbitalReflexEngine
import com.example.games.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TelaJogoEngineTest {

    @Test
    fun `2048 micro merges matching tiles and persists cleanly`() {
        val initial = Micro2048State(
            board = listOf(
                2, 2, 0,
                4, 0, 4,
                0, 0, 0,
            ),
            score = 0,
        )
        val (afterSwipe, moved) = Micro2048Engine.move(initial, SwipeDirection.LEFT, Random(42))
        assertTrue(moved)
        assertEquals(12, afterSwipe.score)
        assertEquals(4, afterSwipe.board[0])
        assertEquals(8, afterSwipe.board[3])

        val json = Micro2048Engine.toJson(afterSwipe)
        val restored = Micro2048Engine.fromJson(json)
        assertNotNull(restored)
        assertEquals(afterSwipe.board, restored!!.board)
        assertEquals(afterSwipe.score, restored.score)
    }

    @Test
    fun `orbital reflex flips lane and serializes angle`() {
        val state = OrbitalReflexEngine.createNewGame(Random(1))
        val flipped = OrbitalReflexEngine.flipLane(state)
        assertEquals(0, flipped.playerLane)

        val stepped = OrbitalReflexEngine.step(flipped, 0.033f, Random(1))
        assertTrue(stepped.state.playerAngleDeg != state.playerAngleDeg)

        val restored = OrbitalReflexEngine.fromJson(OrbitalReflexEngine.toJson(stepped.state))
        assertNotNull(restored)
        assertEquals(stepped.state.playerLane, restored!!.playerLane)
    }

    @Test
    fun `chromatic memory supports rapid taps, detects mismatch, and serializes`() {
        val initial = ChromaticMemoryEngine.startInputPhase(
            ChromaticMemoryEngine.createNewGame(Random(7)).copy(sequence = listOf(0, 2, 1)),
        )
        val tap1 = ChromaticMemoryEngine.submitTap(initial, 0)
        assertTrue(tap1.accepted)
        assertFalse(tap1.failed)

        val tap2 = ChromaticMemoryEngine.submitTap(tap1.state, 2)
        assertTrue(tap2.accepted)
        assertEquals(2, tap2.state.userIndex)

        val restored = ChromaticMemoryEngine.fromJson(ChromaticMemoryEngine.toJson(tap2.state))
        assertNotNull(restored)
        assertEquals(2, restored!!.userIndex)
        assertEquals(listOf(0, 2, 1), restored.sequence)

        val tap3 = ChromaticMemoryEngine.submitTap(tap2.state, 1)
        assertTrue(tap3.roundCompleted)
        assertEquals(MemoryPhase.ROUND_CLEARED, tap3.state.phase)

        val wrongTap = ChromaticMemoryEngine.submitTap(initial, 3)
        assertTrue(wrongTap.failed)
        assertEquals(MemoryPhase.GAME_OVER, wrongTap.state.phase)
    }

    @Test
    fun `lock sandbox store saves and restores settings and game state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = LockSandboxStore(context)
        val settings = TelaJogoSettings(
            activeGame = GameId.ORBITAL_REFLEX,
            oledPowerSaver = true,
            silentMode = true,
            pocketGuardEnabled = true,
            inactivityTimeoutSec = 12,
        )
        store.saveSettings(settings)
        val loaded = store.loadSettings()
        assertEquals(GameId.ORBITAL_REFLEX, loaded.activeGame)
        assertTrue(loaded.silentMode)
        assertEquals(12, loaded.inactivityTimeoutSec)

        val updatedStats = store.recordRound(GameId.MICRO_2048, score = 148, durationSec = 25)
        assertEquals(148, updatedStats[GameId.MICRO_2048]?.highScore)
        assertEquals(1, updatedStats[GameId.MICRO_2048]?.roundsPlayed)
    }
}
