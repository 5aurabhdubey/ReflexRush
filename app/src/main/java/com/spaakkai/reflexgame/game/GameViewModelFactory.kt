package com.spaakkai.reflexgame.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.spaakkai.reflexgame.data.HighScoreStore

class GameViewModelFactory(
    private val highScoreStore: HighScoreStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return GameViewModel(highScoreStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
