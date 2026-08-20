package com.spaakkai.reflexgame.game

/**
 * A single tappable target on screen.
 *
 * @param id unique id so Compose can key it in a list
 * @param xFraction, yFraction position as a fraction (0f..1f) of the play area,
 *        so layout is resolution-independent
 * @param spawnTimeMs elapsed-time timestamp (System.nanoTime()/1e6) when it appeared,
 *        used to compute reaction time and to expire the target if unhandled
 * @param ttlMs how long the target stays alive before it's counted as a miss
 * @param sizeDp visual size — shrinks as difficulty increases
 */
data class Target(
    val id: Long,
    val xFraction: Float,
    val yFraction: Float,
    val spawnTimeMs: Long,
    val ttlMs: Long,
    val sizeDp: Float,
    val isDecoy: Boolean = false
)
