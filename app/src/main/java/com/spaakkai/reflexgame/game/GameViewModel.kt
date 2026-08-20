package com.spaakkai.reflexgame.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spaakkai.reflexgame.data.HighScoreStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class GamePhase { MENU, PLAYING, GAME_OVER }

data class GameUiState(
    val phase: GamePhase = GamePhase.MENU,
    val score: Int = 0,
    val misses: Int = 0,
    val livesLeft: Int = 3,
    val combo: Int = 0,
    val peakCombo: Int = 0,
    val targets: List<Target> = emptyList(),
    val difficultyLabel: String = "Warming Up",
    val avgReactionMs: Long = 0,
    val accuracyPercent: Int = 100,
    val highScore: Int = 0,
    val isNewHighScore: Boolean = false,
    val bestCombo: Int = 0,
    val gamesPlayed: Int = 0
)

class GameViewModel(
    private val highScoreStore: HighScoreStore
) : ViewModel() {

    var uiState by mutableStateOf(GameUiState())
        private set

    private val difficultyEngine = DifficultyEngine()
    private var spawnLoopJob: Job? = null
    private var nextTargetId = 0L
    private val startingLives = 3

    private val _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    init {
        viewModelScope.launch {
            highScoreStore.highScoreFlow.collect { hs ->
                _highScore.value = hs
                uiState = uiState.copy(highScore = hs)
            }
        }
        viewModelScope.launch {
            highScoreStore.bestComboFlow.collect { bc ->
                uiState = uiState.copy(bestCombo = bc)
            }
        }
        viewModelScope.launch {
            highScoreStore.gamesPlayedFlow.collect { gp ->
                uiState = uiState.copy(gamesPlayed = gp)
            }
        }
    }

    fun startGame() {
        uiState = GameUiState(
            phase = GamePhase.PLAYING,
            livesLeft = startingLives,
            highScore = _highScore.value,
            bestCombo = uiState.bestCombo,
            gamesPlayed = uiState.gamesPlayed
        )
        nextTargetId = 0L
        spawnLoopJob?.cancel()
        spawnLoopJob = viewModelScope.launch {
            while (uiState.phase == GamePhase.PLAYING) {
                spawnTarget()
                val level = difficultyEngine.currentLevel()
                delay(level.spawnIntervalMs)
                expireOverdueTargets()
            }
        }
    }

    private fun spawnTarget() {
        val level = difficultyEngine.currentLevel()
        val isDecoy = Random.nextFloat() < level.decoyChance
        val target = Target(
            id = nextTargetId++,
            xFraction = Random.nextFloat().coerceIn(0.08f, 0.92f),
            yFraction = Random.nextFloat().coerceIn(0.08f, 0.85f),
            spawnTimeMs = nowMs(),
            ttlMs = level.targetTtlMs,
            sizeDp = level.targetSizeDp,
            isDecoy = isDecoy
        )
        uiState = uiState.copy(
            targets = uiState.targets + target,
            difficultyLabel = level.label
        )
    }

    private fun expireOverdueTargets() {
        val now = nowMs()
        val stillAlive = mutableListOf<Target>()
        var missesThisTick = 0
        var livesLost = 0

        for (t in uiState.targets) {
            val age = now - t.spawnTimeMs
            if (age >= t.ttlMs) {
                if (!t.isDecoy) {
                    // Real target expired unhit -> counts as a miss.
                    difficultyEngine.recordMiss()
                    missesThisTick++
                    livesLost++
                }
                // Decoys expiring naturally is fine — that's the "correct" outcome.
            } else {
                stillAlive.add(t)
            }
        }

        if (missesThisTick > 0 || stillAlive.size != uiState.targets.size) {
            val newLives = (uiState.livesLeft - livesLost).coerceAtLeast(0)
            uiState = uiState.copy(
                targets = stillAlive,
                misses = uiState.misses + missesThisTick,
                livesLeft = newLives,
                combo = if (missesThisTick > 0) 0 else uiState.combo,
                avgReactionMs = difficultyEngine.averageReactionMs(),
                accuracyPercent = (difficultyEngine.accuracy() * 100).toInt()
            )
            if (newLives <= 0) endGame()
        }
    }

    fun onTargetTapped(target: Target) {
        if (uiState.phase != GamePhase.PLAYING) return
        val reactionTime = nowMs() - target.spawnTimeMs
        val remaining = uiState.targets.filterNot { it.id == target.id }

        if (target.isDecoy) {
            // Tapping a decoy is a mistake — costs a life, resets combo, no score.
            difficultyEngine.recordMiss()
            val newLives = (uiState.livesLeft - 1).coerceAtLeast(0)
            uiState = uiState.copy(
                targets = remaining,
                livesLeft = newLives,
                combo = 0,
                avgReactionMs = difficultyEngine.averageReactionMs(),
                accuracyPercent = (difficultyEngine.accuracy() * 100).toInt()
            )
            if (newLives <= 0) endGame()
        } else {
            difficultyEngine.recordHit(reactionTime)
            val newCombo = uiState.combo + 1
            uiState = uiState.copy(
                targets = remaining,
                score = uiState.score + scoreForReaction(reactionTime, newCombo),
                combo = newCombo,
                peakCombo = maxOf(uiState.peakCombo, newCombo),
                avgReactionMs = difficultyEngine.averageReactionMs(),
                accuracyPercent = (difficultyEngine.accuracy() * 100).toInt()
            )
        }
    }

    private fun scoreForReaction(reactionTimeMs: Long, combo: Int): Int {
        // Faster taps are worth more: up to 150 pts for a near-instant tap,
        // floor of 10 pts for a slow-but-successful hit.
        val base = (150 - (reactionTimeMs / 8)).toInt().coerceIn(10, 150)
        // Combo multiplier: +10% per consecutive hit, capped at 3x so it doesn't
        // dominate the reaction-speed skill signal.
        val multiplier = (1f + (combo - 1).coerceAtLeast(0) * 0.1f).coerceAtMost(3f)
        return (base * multiplier).toInt()
    }

    private fun endGame() {
        spawnLoopJob?.cancel()
        val finalAvgReaction = difficultyEngine.averageReactionMs()
        viewModelScope.launch {
            val isNew = highScoreStore.submitScore(uiState.score, finalAvgReaction, uiState.peakCombo)
            uiState = uiState.copy(
                phase = GamePhase.GAME_OVER,
                targets = emptyList(),
                isNewHighScore = isNew,
                highScore = maxOf(uiState.highScore, uiState.score)
            )
        }
    }

    fun returnToMenu() {
        spawnLoopJob?.cancel()
        uiState = uiState.copy(phase = GamePhase.MENU, targets = emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        spawnLoopJob?.cancel()
    }

    private fun nowMs(): Long = System.nanoTime() / 1_000_000
}
