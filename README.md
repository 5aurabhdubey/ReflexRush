# Reflex Rush

A reflex/reaction Android game built with Kotlin + Jetpack Compose (MVVM), featuring
AI-driven adaptive difficulty.

## What it does

Targets spawn at random positions on screen. Tap green targets quickly for points;
avoid red decoy targets (tapping one costs a life). A rolling window of your last
6 reaction times + hit/miss outcomes feeds a difficulty engine that continuously
adjusts spawn rate, target lifetime, target size, and decoy frequency — so the game
gets harder in real time as you get better, and eases off if you're struggling.

## Architecture

- **MVVM**: `GameViewModel` owns all game state (`GameUiState`) and the spawn loop;
  Compose screens are pure functions of that state.
- **`DifficultyEngine`** (`game/DifficultyEngine.kt`): the adaptive-difficulty logic.
  Computes a 0–1 "skill" score from average reaction time + accuracy over a rolling
  window, then interpolates spawn interval, target TTL, target size, and decoy
  chance from that score. Documented as a heuristic model — deliberately simple and
  explainable rather than a trained model, since it needs to run every spawn tick
  with zero inference latency.
- **Persistence**: `HighScoreStore` (`data/HighScoreStore.kt`) uses Jetpack DataStore
  to persist high score and best average reaction time across sessions.
- **UI**: three Compose screens — `MenuScreen`, `GameScreen`, `GameOverScreen` —
  switched via a simple `GamePhase` enum, no navigation library needed for this scope.

## Project structure

```
app/src/main/java/com/spaakkai/reflexgame/
├── MainActivity.kt              # Entry point, wires ViewModel to screens
├── data/
│   └── HighScoreStore.kt        # DataStore persistence
├── game/
│   ├── Target.kt                # Target data model
│   ├── DifficultyEngine.kt      # Adaptive difficulty logic
│   ├── GameViewModel.kt         # Game loop + state (MVVM)
│   ├── GameViewModelFactory.kt
│   ├── MenuScreen.kt
│   ├── GameScreen.kt
│   └── GameOverScreen.kt
└── ui/theme/
    ├── Color.kt
    └── Theme.kt
```

## Running it

1. Open the project root in Android Studio (Koala or newer recommended).
2. Let Gradle sync (uses AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06.00).
3. Run on a device or emulator running API 24+.

## Building a release APK

```
./gradlew assembleRelease
```
Output APK will be at `app/build/outputs/apk/release/app-release.apk`.

