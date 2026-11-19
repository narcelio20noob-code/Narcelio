package com.example.games

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

data class Micro2048State(
    val board: List<Int> = List(9) { 0 },
    val score: Int = 0,
    val moveCount: Int = 0,
    val isGameOver: Boolean = false,
    val reachedTarget256: Boolean = false,
    val lastMergedIndices: Set<Int> = emptySet(),
    val lastSpawnedIndex: Int? = null,
) {
    val bestTile: Int
        get() = board.maxOrNull() ?: 0
}

object Micro2048Engine {

    fun createNewGame(random: Random = Random.Default): Micro2048State {
        val cells = MutableList(9) { 0 }
        val first = spawnTile(cells, random)
        val second = spawnTile(cells, random)
        return Micro2048State(
            board = cells.toList(),
            score = 0,
            moveCount = 0,
            isGameOver = false,
            reachedTarget256 = false,
            lastSpawnedIndex = second ?: first,
        )
    }

    fun move(
        state: Micro2048State,
        direction: SwipeDirection,
        random: Random = Random.Default,
    ): Pair<Micro2048State, Boolean> {
        if (state.isGameOver) return state to false

        val working = state.board.toMutableList()
        var scoreGain = 0
        var changed = false
        val mergedGlobalIndices = mutableSetOf<Int>()

        for (lineIdx in 0 until 3) {
            val indices = lineIndices(lineIdx, direction)
            val lineValues = indices.map { working[it] }
            val result = compressAndMergeLine(lineValues)
            if (result.values != lineValues) {
                changed = true
            }
            scoreGain += result.scoreGain
            indices.forEachIndexed { pos, globalIdx ->
                working[globalIdx] = result.values[pos]
                if (pos in result.mergedPositions) {
                    mergedGlobalIndices.add(globalIdx)
                }
            }
        }

        if (!changed) return state to false

        val spawnedIdx = spawnTile(working, random)
        val newBoard = working.toList()
        val gameOver = !hasAvailableMove(newBoard)
        val newState = state.copy(
            board = newBoard,
            score = state.score + scoreGain,
            moveCount = state.moveCount + 1,
            isGameOver = gameOver,
            reachedTarget256 = state.reachedTarget256 || newBoard.any { it >= 256 },
            lastMergedIndices = mergedGlobalIndices,
            lastSpawnedIndex = spawnedIdx,
        )
        return newState to true
    }

    private fun lineIndices(lineIdx: Int, direction: SwipeDirection): List<Int> = when (direction) {
        SwipeDirection.LEFT -> listOf(lineIdx * 3, lineIdx * 3 + 1, lineIdx * 3 + 2)
        SwipeDirection.RIGHT -> listOf(lineIdx * 3 + 2, lineIdx * 3 + 1, lineIdx * 3)
        SwipeDirection.UP -> listOf(lineIdx, lineIdx + 3, lineIdx + 6)
        SwipeDirection.DOWN -> listOf(lineIdx + 6, lineIdx + 3, lineIdx)
    }

    private data class LineMergeResult(
        val values: List<Int>,
        val scoreGain: Int,
        val mergedPositions: Set<Int>,
    )

    private fun compressAndMergeLine(line: List<Int>): LineMergeResult {
        val nonZero = line.filter { it != 0 }
        val output = mutableListOf<Int>()
        val mergedPositions = mutableSetOf<Int>()
        var scoreGain = 0
        var i = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val merged = nonZero[i] * 2
                mergedPositions.add(output.size)
                output.add(merged)
                scoreGain += merged
                i += 2
            } else {
                output.add(nonZero[i])
                i += 1
            }
        }
        while (output.size < 3) {
            output.add(0)
        }
        return LineMergeResult(output, scoreGain, mergedPositions)
    }

    private fun spawnTile(cells: MutableList<Int>, random: Random): Int? {
        val emptyIndices = cells.indices.filter { cells[it] == 0 }
        if (emptyIndices.isEmpty()) return null
        val chosen = emptyIndices[random.nextInt(emptyIndices.size)]
        cells[chosen] = if (random.nextFloat() < 0.9f) 2 else 4
        return chosen
    }

    fun hasAvailableMove(board: List<Int>): Boolean {
        if (board.any { it == 0 }) return true
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val idx = r * 3 + c
                val value = board[idx]
                if (c < 2 && board[idx + 1] == value) return true
                if (r < 2 && board[idx + 3] == value) return true
            }
        }
        return false
    }

    fun toJson(state: Micro2048State): String {
        val arr = JSONArray()
        state.board.forEach { arr.put(it) }
        return JSONObject().apply {
            put("board", arr)
            put("score", state.score)
            put("moveCount", state.moveCount)
            put("isGameOver", state.isGameOver)
            put("reachedTarget256", state.reachedTarget256)
        }.toString()
    }

    fun fromJson(json: String): Micro2048State? = runCatching {
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("board")
        if (arr.length() != 9) return null
        val board = List(9) { i -> arr.getInt(i) }
        Micro2048State(
            board = board,
            score = obj.optInt("score", 0),
            moveCount = obj.optInt("moveCount", 0),
            isGameOver = obj.optBoolean("isGameOver", false),
            reachedTarget256 = obj.optBoolean("reachedTarget256", false),
        )
    }.getOrNull()
}
