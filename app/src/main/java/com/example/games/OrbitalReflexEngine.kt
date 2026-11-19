package com.example.games

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.random.Random

data class OrbitalHazard(
    val id: Int,
    val angleDeg: Float,
    val lane: Int, // 0 = inner, 1 = outer
    val arcSpanDeg: Float = 16f,
)

data class OrbitalOrb(
    val id: Int,
    val angleDeg: Float,
    val lane: Int,
)

data class OrbitalReflexState(
    val playerAngleDeg: Float = 270f,
    val playerLane: Int = 1, // 0 = inner, 1 = outer
    val angularVelocityDegPerSec: Float = 115f,
    val score: Int = 0,
    val combo: Int = 1,
    val elapsedSec: Float = 0f,
    val isGameOver: Boolean = false,
    val hazards: List<OrbitalHazard> = emptyList(),
    val orbs: List<OrbitalOrb> = emptyList(),
    val nextEntityId: Int = 10,
)

data class OrbitalStepResult(
    val state: OrbitalReflexState,
    val collectedOrb: Boolean = false,
    val collided: Boolean = false,
)

object OrbitalReflexEngine {

    fun createNewGame(random: Random = Random.Default): OrbitalReflexState {
        val initialHazards = listOf(
            OrbitalHazard(id = 1, angleDeg = 60f, lane = 1),
            OrbitalHazard(id = 2, angleDeg = 175f, lane = 0),
            OrbitalHazard(id = 3, angleDeg = 225f, lane = 1),
        )
        val initialOrbs = listOf(
            OrbitalOrb(id = 4, angleDeg = 15f, lane = 1),
            OrbitalOrb(id = 5, angleDeg = 120f, lane = 0),
            OrbitalOrb(id = 6, angleDeg = 315f, lane = random.nextInt(2)),
        )
        return OrbitalReflexState(
            playerAngleDeg = 270f,
            playerLane = 1,
            angularVelocityDegPerSec = 115f,
            score = 0,
            combo = 1,
            elapsedSec = 0f,
            isGameOver = false,
            hazards = initialHazards,
            orbs = initialOrbs,
            nextEntityId = 10,
        )
    }

    fun flipLane(state: OrbitalReflexState): OrbitalReflexState {
        if (state.isGameOver) return state
        return state.copy(playerLane = if (state.playerLane == 0) 1 else 0)
    }

    fun step(
        state: OrbitalReflexState,
        deltaSec: Float,
        random: Random = Random.Default,
    ): OrbitalStepResult {
        if (state.isGameOver || deltaSec <= 0f) {
            return OrbitalStepResult(state = state)
        }

        val clampedDelta = deltaSec.coerceAtMost(0.08f)
        val speedBoost = (state.elapsedSec * 2.2f).coerceAtMost(90f)
        val currentVelocity = 115f + speedBoost
        val prevAngle = state.playerAngleDeg
        val nextAngle = normalizeAngle(prevAngle + currentVelocity * clampedDelta)

        // Check collision with hazards on the same lane
        val hitHazard = state.hazards.any { hazard ->
            hazard.lane == state.playerLane &&
                angularDistance(nextAngle, hazard.angleDeg) <= (hazard.arcSpanDeg * 0.5f + 5f)
        }

        if (hitHazard) {
            return OrbitalStepResult(
                state = state.copy(
                    playerAngleDeg = nextAngle,
                    angularVelocityDegPerSec = currentVelocity,
                    elapsedSec = state.elapsedSec + clampedDelta,
                    isGameOver = true,
                ),
                collided = true,
            )
        }

        // Check orb collection
        var collected = false
        var newScore = state.score
        var newCombo = state.combo
        var nextId = state.nextEntityId
        val updatedOrbs = state.orbs.map { orb ->
            val touched = orb.lane == state.playerLane &&
                angularDistance(nextAngle, orb.angleDeg) <= 13f
            if (touched) {
                collected = true
                newScore += 10 * newCombo
                newCombo = (newCombo + 1).coerceAtMost(5)
                val respawnAngle = normalizeAngle(nextAngle + 110f + random.nextInt(150))
                val newLane = random.nextInt(2)
                OrbitalOrb(id = nextId++, angleDeg = respawnAngle, lane = newLane)
            } else {
                orb
            }
        }

        // Slowly drift hazards away if player completed a full lap & add variety without overlapping player
        val updatedHazards = if (collected && newScore % 40 == 0) {
            state.hazards.mapIndexed { index, hazard ->
                if (index == (newScore / 40) % state.hazards.size) {
                    val safeAngle = normalizeAngle(nextAngle + 145f + random.nextInt(80))
                    hazard.copy(
                        angleDeg = safeAngle,
                        lane = if (hazard.lane == 0) 1 else 0,
                    )
                } else {
                    hazard
                }
            }
        } else {
            state.hazards
        }

        return OrbitalStepResult(
            state = state.copy(
                playerAngleDeg = nextAngle,
                angularVelocityDegPerSec = currentVelocity,
                score = newScore,
                combo = newCombo,
                elapsedSec = state.elapsedSec + clampedDelta,
                hazards = updatedHazards,
                orbs = updatedOrbs,
                nextEntityId = nextId,
            ),
            collectedOrb = collected,
        )
    }

    private fun normalizeAngle(deg: Float): Float {
        var angle = deg % 360f
        if (angle < 0f) angle += 360f
        return angle
    }

    private fun angularDistance(a: Float, b: Float): Float {
        val diff = abs(normalizeAngle(a) - normalizeAngle(b))
        return if (diff > 180f) 360f - diff else diff
    }

    fun toJson(state: OrbitalReflexState): String {
        val hazardsArr = JSONArray().apply {
            state.hazards.forEach { h ->
                put(
                    JSONObject().apply {
                        put("id", h.id)
                        put("angle", h.angleDeg.toDouble())
                        put("lane", h.lane)
                        put("span", h.arcSpanDeg.toDouble())
                    },
                )
            }
        }
        val orbsArr = JSONArray().apply {
            state.orbs.forEach { o ->
                put(
                    JSONObject().apply {
                        put("id", o.id)
                        put("angle", o.angleDeg.toDouble())
                        put("lane", o.lane)
                    },
                )
            }
        }
        return JSONObject().apply {
            put("playerAngle", state.playerAngleDeg.toDouble())
            put("playerLane", state.playerLane)
            put("velocity", state.angularVelocityDegPerSec.toDouble())
            put("score", state.score)
            put("combo", state.combo)
            put("elapsedSec", state.elapsedSec.toDouble())
            put("isGameOver", state.isGameOver)
            put("hazards", hazardsArr)
            put("orbs", orbsArr)
            put("nextId", state.nextEntityId)
        }.toString()
    }

    fun fromJson(json: String): OrbitalReflexState? = runCatching {
        val obj = JSONObject(json)
        val hazardsArr = obj.getJSONArray("hazards")
        val hazards = List(hazardsArr.length()) { idx ->
            val h = hazardsArr.getJSONObject(idx)
            OrbitalHazard(
                id = h.getInt("id"),
                angleDeg = h.getDouble("angle").toFloat(),
                lane = h.getInt("lane"),
                arcSpanDeg = h.optDouble("span", 16.0).toFloat(),
            )
        }
        val orbsArr = obj.getJSONArray("orbs")
        val orbs = List(orbsArr.length()) { idx ->
            val o = orbsArr.getJSONObject(idx)
            OrbitalOrb(
                id = o.getInt("id"),
                angleDeg = o.getDouble("angle").toFloat(),
                lane = o.getInt("lane"),
            )
        }
        OrbitalReflexState(
            playerAngleDeg = obj.optDouble("playerAngle", 270.0).toFloat(),
            playerLane = obj.optInt("playerLane", 1),
            angularVelocityDegPerSec = obj.optDouble("velocity", 115.0).toFloat(),
            score = obj.optInt("score", 0),
            combo = obj.optInt("combo", 1),
            elapsedSec = obj.optDouble("elapsedSec", 0.0).toFloat(),
            isGameOver = obj.optBoolean("isGameOver", false),
            hazards = hazards,
            orbs = orbs,
            nextEntityId = obj.optInt("nextId", 10),
        )
    }.getOrNull()
}
