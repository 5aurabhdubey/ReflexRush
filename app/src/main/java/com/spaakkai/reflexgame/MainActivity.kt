package com.spaakkai.reflexgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spaakkai.reflexgame.data.HighScoreStore
import com.spaakkai.reflexgame.game.GamePhase
import com.spaakkai.reflexgame.game.GameOverScreen
import com.spaakkai.reflexgame.game.GameScreen
import com.spaakkai.reflexgame.game.GameViewModel
import com.spaakkai.reflexgame.game.GameViewModelFactory
import com.spaakkai.reflexgame.game.MenuScreen
import com.spaakkai.reflexgame.ui.theme.ReflexRushTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels {
        GameViewModelFactory(HighScoreStore(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReflexRushTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReflexRushApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun ReflexRushApp(viewModel: GameViewModel) {
    val state = viewModel.uiState
    when (state.phase) {
        GamePhase.MENU -> MenuScreen(
            highScore = state.highScore,
            bestCombo = state.bestCombo,
            gamesPlayed = state.gamesPlayed,
            onStart = viewModel::startGame
        )
        GamePhase.PLAYING -> GameScreen(
            state = state,
            onTargetTapped = viewModel::onTargetTapped
        )
        GamePhase.GAME_OVER -> GameOverScreen(
            state = state,
            onPlayAgain = viewModel::startGame,
            onMenu = viewModel::returnToMenu
        )
    }
}
