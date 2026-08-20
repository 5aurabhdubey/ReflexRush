package com.spaakkai.reflexgame.game

import kotlin.math.max
import kotlin.math.min

/**
 * Tunable snapshot describing how the game should currently behave.
 * Regenerated after every target resolution (hit / miss / decoy tap).
 */
data class DifficultyLevel(
    val spawnIntervalMs: Long,
    val targetTtlMs: Long,
    val targetSizeDp: Float,
    val decoyChance: Float,
    val label: String
)

/**
 * Adaptive difficulty controller.
 *
 * Design: rather than a fixed difficulty curve tied to score/time, this keeps a
 * rolling window of the player's last N reaction times + hit/miss outcomes and
 * recomputes difficulty from *actual measured performance*. A player who is
 * consistently fast and accurate gets smaller/faster/trickier targets (including
 * decoys they must avoid); a player who is struggling gets a gentler pace.
 *
 * This is intentionally a simple, explainable heuristic model (not a trained
 * ML model) — appropriate for a real-time on-device loop with zero latency
 * budget for inference, while still demonstrating adaptive/AI-driven behavior.
 */
class DifficultyEngine(
    private val windowSize: Int = 6
) {
    private val recentReactionTimesMs = ArrayDeque<Long>()
    private val recentHits = ArrayDeque<Boolean>()

    fun recordHit(reactionTimeMs: Long) {
        pushWindow(recentReactionTimesMs, reactionTimeMs)
        pushWindow(recentHits, true)
    }

    fun recordMiss() {
        pushWindow(recentHits, false)
        // A miss has no reaction time sample, but it still drags accuracy down,
        // which the level computation reacts to.
    }

    private fun <T> pushWindow(window: ArrayDeque<T>, value: T) {
        window.addLast(value)
        if (window.size > windowSize) window.removeFirst()
    }

    fun currentLevel(): DifficultyLevel {
        val avgReaction = if (recentReactionTimesMs.isEmpty()) {
            750L // neutral starting assumption
        } else {
            recentReactionTimesMs.average().toLong()
        }
        val accuracy = if (recentHits.isEmpty()) {
            1f
        } else {
            recentHits.count { it }.toFloat() / recentHits.size
        }

        // Skill score in [0,1]: fast reactions + high accuracy -> higher skill.
        // 250ms is near-elite, 900ms+ is treated as beginner pace.
        val speedScore = 1f - ((avgReaction - 250L).coerceIn(0L, 650L) / 650f)
        val skill = (0.6f * speedScore + 0.4f * accuracy).coerceIn(0f, 1f)

        val spawnInterval = lerpLong(1400L, 550L, skill)
        val ttl = lerpLong(1600L, 700L, skill)
        val size = lerpFloat(64f, 34f, skill)
        val decoyChance = lerpFloat(0f, 0.35f, skill)

        val label = when {
            skill < 0.25f -> "Warming Up"
            skill < 0.5f -> "Locked In"
            skill < 0.75f -> "Sharp"
            else -> "Reflex Machine"
        }

        return DifficultyLevel(
            spawnIntervalMs = spawnInterval,
            targetTtlMs = ttl,
            targetSizeDp = size,
            decoyChance = decoyChance,
            label = label
        )
    }

    fun averageReactionMs(): Long =
        if (recentReactionTimesMs.isEmpty()) 0L else recentReactionTimesMs.average().toLong()

    fun accuracy(): Float =
        if (recentHits.isEmpty()) 1f else recentHits.count { it }.toFloat() / recentHits.size

    private fun lerpLong(from: Long, to: Long, t: Float): Long =
        (from + (to - from) * t.coerceIn(0f, 1f)).toLong()

    private fun lerpFloat(from: Float, to: Float, t: Float): Float =
        from + (to - from) * t.coerceIn(0f, 1f)

    companion object {
        fun clampSkill(v: Float) = max(0f, min(1f, v))
    }
}
