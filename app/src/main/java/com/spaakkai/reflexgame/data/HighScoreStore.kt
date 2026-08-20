package com.spaakkai.reflexgame.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "reflex_rush_prefs")

/**
 * Small persistence wrapper around Jetpack DataStore.
 * Keeps the highest score and the best (lowest) average reaction time ever recorded,
 * so the game-over screen can show "new best" state.
 */
class HighScoreStore(private val context: Context) {

    private object Keys {
        val HIGH_SCORE = intPreferencesKey("high_score")
        val BEST_AVG_REACTION_MS = longPreferencesKey("best_avg_reaction_ms")
        val GAMES_PLAYED = intPreferencesKey("games_played")
        val BEST_COMBO = intPreferencesKey("best_combo")
    }

    val highScoreFlow: Flow<Int> =
        context.dataStore.data.map { it[Keys.HIGH_SCORE] ?: 0 }

    val bestAvgReactionFlow: Flow<Long> =
        context.dataStore.data.map { it[Keys.BEST_AVG_REACTION_MS] ?: Long.MAX_VALUE }

    val gamesPlayedFlow: Flow<Int> =
        context.dataStore.data.map { it[Keys.GAMES_PLAYED] ?: 0 }

    val bestComboFlow: Flow<Int> =
        context.dataStore.data.map { it[Keys.BEST_COMBO] ?: 0 }

    suspend fun submitScore(score: Int, avgReactionMs: Long, peakCombo: Int): Boolean {
        var isNewHighScore = false
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.HIGH_SCORE] ?: 0
            if (score > current) {
                prefs[Keys.HIGH_SCORE] = score
                isNewHighScore = true
            }
            val currentBestReaction = prefs[Keys.BEST_AVG_REACTION_MS] ?: Long.MAX_VALUE
            if (avgReactionMs in 1 until currentBestReaction) {
                prefs[Keys.BEST_AVG_REACTION_MS] = avgReactionMs
            }
            val currentBestCombo = prefs[Keys.BEST_COMBO] ?: 0
            if (peakCombo > currentBestCombo) {
                prefs[Keys.BEST_COMBO] = peakCombo
            }
            prefs[Keys.GAMES_PLAYED] = (prefs[Keys.GAMES_PLAYED] ?: 0) + 1
        }
        return isNewHighScore
    }
}
