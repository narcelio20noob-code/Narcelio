package com.example.games

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class MemoryPhase {
    SHOWING_SEQUENCE,
    AWAITING_INPUT,
    ROUND_CLEARED,
    GAME_OVER,
}

enum class PadSymbol(val index: Int, val label: String) {
    CIRCLE(0, "Círculo Ciano"),
    TRIANGLE(1, "Triângulo Esmeralda"),
    SQUARE(2, "Quadrado Âmbar"),
    DIAMOND(3, "Losango Violeta"),
}

data class ChromaticMemoryState(
    val sequence: List<Int> = emptyList(),
    val userIndex: Int = 0,
    val round: Int = 1,
    val score: Int = 0,
    val phase: MemoryPhase = MemoryPhase.SHOWING_SEQUENCE,
    val activeLitPad: Int? = null,
)

data class ChromaticTapResult(
    val state: ChromaticMemoryState,
    val accepted: Boolean,
    val roundCompleted: Boolean = false,
    val failed: Boolean = false,
)

object ChromaticMemoryEngine {

    fun createNewGame(random: Random = Random.Default): ChromaticMemoryState =
        ChromaticMemoryState(
            sequence = listOf(random.nextInt(4), random.nextInt(4)),
            userIndex = 0,
            round = 1,
            score = 0,
            phase = MemoryPhase.SHOWING_SEQUENCE,
            activeLitPad = null,
        )

    fun startInputPhase(state: ChromaticMemoryState): ChromaticMemoryState =
        if (state.phase == MemoryPhase.SHOWING_SEQUENCE) {
            state.copy(phase = MemoryPhase.AWAITING_INPUT, activeLitPad = null, userIndex = 0)
        } else {
            state
        }

    fun setLitPad(state: ChromaticMemoryState, padIndex: Int?): ChromaticMemoryState =
        state.copy(activeLitPad = padIndex)

    /**
     * Synchronous tap evaluation guarantees zero dropped taps even during rapid thumb tapping.
     */
    fun submitTap(state: ChromaticMemoryState, padIndex: Int): ChromaticTapResult {
        if (state.phase != MemoryPhase.AWAITING_INPUT || padIndex !in 0..3) {
            return ChromaticTapResult(state = state, accepted = false)
        }

        val expected = state.sequence.getOrNull(state.userIndex)
            ?: return ChromaticTapResult(state = state, accepted = false)

        if (padIndex != expected) {
            return ChromaticTapResult(
                state = state.copy(
                    phase = MemoryPhase.GAME_OVER,
                    activeLitPad = padIndex,
                ),
                accepted = true,
                failed = true,
            )
        }

        val nextUserIndex = state.userIndex + 1
        val nextScore = state.score + 10
        return if (nextUserIndex >= state.sequence.size) {
            ChromaticTapResult(
                state = state.copy(
                    userIndex = nextUserIndex,
                    score = nextScore + state.round * 15,
                    phase = MemoryPhase.ROUND_CLEARED,
                    activeLitPad = padIndex,
                ),
                accepted = true,
                roundCompleted = true,
            )
        } else {
            ChromaticTapResult(
                state = state.copy(
                    userIndex = nextUserIndex,
                    score = nextScore,
                    activeLitPad = padIndex,
                ),
                accepted = true,
            )
        }
    }

    fun advanceNextRound(
        state: ChromaticMemoryState,
        random: Random = Random.Default,
    ): ChromaticMemoryState {
        val nextSeq = state.sequence + random.nextInt(4)
        return state.copy(
            sequence = nextSeq,
            userIndex = 0,
            round = state.round + 1,
            phase = MemoryPhase.SHOWING_SEQUENCE,
            activeLitPad = null,
        )
    }

    fun toJson(state: ChromaticMemoryState): String {
        val arr = JSONArray()
        state.sequence.forEach { arr.put(it) }
        return JSONObject().apply {
            put("sequence", arr)
            put("userIndex", state.userIndex)
            put("round", state.round)
            put("score", state.score)
            put("phase", state.phase.name)
        }.toString()
    }

    fun fromJson(json: String): ChromaticMemoryState? = runCatching {
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("sequence")
        val seq = List(arr.length()) { idx -> arr.getInt(idx) }
        if (seq.isEmpty()) return null
        val rawPhase = obj.optString("phase", MemoryPhase.SHOWING_SEQUENCE.name)
        val restoredPhase = MemoryPhase.entries.firstOrNull { it.name == rawPhase }
            ?: MemoryPhase.SHOWING_SEQUENCE
        // If interrupted mid-show or round cleared, cleanly restart sequence playback for current round
        val safePhase = when (restoredPhase) {
            MemoryPhase.ROUND_CLEARED -> MemoryPhase.SHOWING_SEQUENCE
            else -> restoredPhase
        }
        ChromaticMemoryState(
            sequence = seq,
            userIndex = obj.optInt("userIndex", 0).coerceIn(0, seq.size),
            round = obj.optInt("round", 1),
            score = obj.optInt("score", 0),
            phase = safePhase,
            activeLitPad = null,
        )
    }.getOrNull()
}
