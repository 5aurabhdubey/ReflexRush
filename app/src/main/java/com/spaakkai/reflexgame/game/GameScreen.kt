package com.spaakkai.reflexgame.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spaakkai.reflexgame.ui.theme.BombCore
import com.spaakkai.reflexgame.ui.theme.BombGlow
import com.spaakkai.reflexgame.ui.theme.ComboColor
import com.spaakkai.reflexgame.ui.theme.GradientBackground
import com.spaakkai.reflexgame.ui.theme.MissFlash
import com.spaakkai.reflexgame.ui.theme.TargetHit
import com.spaakkai.reflexgame.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Burst(val id: Long, val x: Dp, val y: Dp, val color: Color)
private data class ScorePopup(val id: Long, val x: Dp, val y: Dp, val text: String, val color: Color)

private val difficultyOrder = listOf("Warming Up", "Locked In", "Sharp", "Reflex Machine")

@Composable
fun GameScreen(
    state: GameUiState,
    onTargetTapped: (Target) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val bursts = remember { mutableStateListOf<Burst>() }
    val popups = remember { mutableStateListOf<ScorePopup>() }
    var effectId by remember { mutableStateOf(0L) }
    var lastTapPos by remember { mutableStateOf<Pair<Dp, Dp>?>(null) }
    var lastScore by remember { mutableStateOf(state.score) }
    var lastLives by remember { mutableStateOf(state.livesLeft) }
    var missFlashAlpha by remember { mutableStateOf(0f) }

    LaunchedEffect(state.score) {
        if (state.score > lastScore) {
            val delta = state.score - lastScore
            lastTapPos?.let { (x, y) ->
                popups.add(ScorePopup(effectId++, x, y, "+$delta", TargetHit))
            }
        }
        lastScore = state.score
    }

    LaunchedEffect(state.livesLeft) {
        if (state.livesLeft < lastLives) {
            missFlashAlpha = 1f
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastLives = state.livesLeft
    }
    LaunchedEffect(missFlashAlpha) {
        if (missFlashAlpha > 0f) {
            delay(50)
            missFlashAlpha = 0f
        }
    }
    val animatedFlash by animateFloatAsState(
        targetValue = missFlashAlpha,
        animationSpec = tween(350),
        label = "missFlash"
    )

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            HudRow(state)
            Spacer(Modifier.height(8.dp))
            if (state.combo >= 2) {
                ComboBadge(state.combo)
                Spacer(Modifier.height(8.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x14FFFFFF))
            ) {
                PlayArea(
                    state = state,
                    onTargetTapped = { target, xDp, yDp ->
                        lastTapPos = xDp to yDp
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        bursts.add(
                            Burst(
                                id = effectId++,
                                x = xDp,
                                y = yDp,
                                color = if (target.isDecoy) BombGlow else TargetHit
                            )
                        )
                        onTargetTapped(target)
                    }
                )

                for (burst in bursts.toList()) {
                    key(burst.id) {
                        BurstView(burst) { bursts.remove(burst) }
                    }
                }
                for (popup in popups.toList()) {
                    key(popup.id) {
                        ScorePopupView(popup) { popups.remove(popup) }
                    }
                }

                if (animatedFlash > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MissFlash.copy(alpha = MissFlash.alpha * animatedFlash))
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            DifficultyBar(state.difficultyLabel)
        }
    }
}

@Composable
private fun HudRow(state: GameUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Score", fontSize = 12.sp, color = TextSecondary)
            Text("${state.score}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Lives", fontSize = 12.sp, color = TextSecondary)
            HeartsRow(state.livesLeft)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Avg reaction", fontSize = 12.sp, color = TextSecondary)
            Text(
                if (state.avgReactionMs > 0) "${state.avgReactionMs} ms" else "-",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HeartsRow(lives: Int) {
    Row {
        repeat(3) { i ->
            val filled = i < lives
            val scale = remember(filled) { Animatable(1f) }
            LaunchedEffect(filled) {
                if (!filled) {
                    scale.animateTo(1.3f, tween(80))
                    scale.animateTo(1f, tween(120))
                }
            }
            Text(
                text = if (filled) "\u2665" else "\u2661",
                fontSize = 18.sp,
                color = if (filled) BombGlow else TextSecondary,
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .scale(scale.value)
                    .alpha(if (filled) 1f else 0.5f)
            )
        }
    }
}

@Composable
private fun ColumnScope.ComboBadge(combo: Int) {
    val scale = remember(combo) { Animatable(1.4f) }
    LaunchedEffect(combo) {
        scale.animateTo(1f, tween(180))
    }
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .scale(scale.value)
            .clip(RoundedCornerShape(12.dp))
            .background(ComboColor.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, ComboColor.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text(
            "COMBO \u00d7$combo",
            color = ComboColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DifficultyBar(currentLabel: String) {
    val currentIndex = difficultyOrder.indexOf(currentLabel).coerceAtLeast(0)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "DIFFICULTY: ${currentLabel.uppercase()}",
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            difficultyOrder.forEachIndexed { index, _ ->
                val active = index <= currentIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (active) ComboColor else Color(0x22FFFFFF))
                )
            }
        }
    }
}

@Composable
private fun PlayArea(
    state: GameUiState,
    onTargetTapped: (Target, Dp, Dp) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val areaWidth = maxWidth
        val areaHeight = maxHeight
        for (target in state.targets) {
            key(target.id) {
                val xDp = areaWidth * target.xFraction
                val yDp = areaHeight * target.yFraction
                if (target.isDecoy) {
                    BombTarget(
                        target = target,
                        xOffset = xDp,
                        yOffset = yDp,
                        onTap = { onTargetTapped(target, xDp, yDp) }
                    )
                } else {
                    TargetBubble(
                        target = target,
                        xOffset = xDp,
                        yOffset = yDp,
                        onTap = { onTargetTapped(target, xDp, yDp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetBubble(
    target: Target,
    xOffset: Dp,
    yOffset: Dp,
    onTap: () -> Unit
) {
    val ringProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = target.ttlMs.toInt(), easing = LinearEasing),
        label = "ttlProgress"
    )
    val spawnScale = remember { Animatable(0.4f) }
    LaunchedEffect(target.id) {
        spawnScale.animateTo(1f, tween(160))
    }

    Box(
        modifier = Modifier
            .offset(x = xOffset - target.sizeDp.dp / 2, y = yOffset - target.sizeDp.dp / 2)
            .size(target.sizeDp.dp)
            .scale(spawnScale.value)
            .pointerInput(target.id) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(TargetHit.copy(alpha = 0.85f))
                .border(
                    BorderStroke((2 + (1f - ringProgress) * 2).dp, Color.White.copy(alpha = 0.6f)),
                    CircleShape
                )
        )
    }
}

/** Bomb-styled decoy: dark core, red glow ring, pulsing warning ring, small fuse spark. */
@Composable
private fun BombTarget(
    target: Target,
    xOffset: Dp,
    yOffset: Dp,
    onTap: () -> Unit
) {
    val spawnScale = remember { Animatable(0.4f) }
    LaunchedEffect(target.id) {
        spawnScale.animateTo(1f, tween(160))
    }

    val pulseTransition = rememberInfiniteTransition(label = "bombPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bombPulseAlpha"
    )

    Box(
        modifier = Modifier
            .offset(x = xOffset - target.sizeDp.dp / 2, y = yOffset - target.sizeDp.dp / 2)
            .size(target.sizeDp.dp)
            .scale(spawnScale.value)
            .pointerInput(target.id) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer pulsing danger glow
            drawCircle(
                color = BombGlow.copy(alpha = 0.25f * pulse),
                radius = radius * 1.35f,
                center = center
            )
            // Warning ring
            drawCircle(
                color = BombGlow.copy(alpha = 0.6f + 0.4f * pulse),
                radius = radius * 1.05f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.12f)
            )
            // Dark metallic core
            drawCircle(
                color = BombCore,
                radius = radius * 0.78f,
                center = center
            )
            // Inner highlight
            drawCircle(
                color = BombGlow.copy(alpha = 0.35f),
                radius = radius * 0.5f,
                center = Offset(center.x - radius * 0.15f, center.y - radius * 0.15f)
            )
            // Fuse
            val fuseTop = Offset(center.x, center.y - radius * 0.78f)
            val fuseEnd = Offset(center.x + radius * 0.25f, center.y - radius * 1.15f)
            drawLine(
                color = Color(0xFFD9A441),
                start = fuseTop,
                end = fuseEnd,
                strokeWidth = radius * 0.1f
            )
            // Spark at fuse tip
            drawCircle(
                color = Color(0xFFFFE066).copy(alpha = 0.6f + 0.4f * pulse),
                radius = radius * 0.14f,
                center = fuseEnd
            )
        }
    }
}

@Composable
private fun BurstView(burst: Burst, onFinished: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0.9f) }
    LaunchedEffect(burst.id) {
        launch { scale.animateTo(2.2f, tween(380, easing = LinearEasing)) }
        alpha.animateTo(0f, tween(380, easing = LinearEasing))
        onFinished()
    }
    Box(
        modifier = Modifier
            .offset(x = burst.x - 28.dp, y = burst.y - 28.dp)
            .size(56.dp)
            .scale(scale.value)
            .alpha(alpha.value)
            .clip(CircleShape)
            .border(BorderStroke(3.dp, burst.color), CircleShape)
    )
}

@Composable
private fun ScorePopupView(popup: ScorePopup, onFinished: () -> Unit) {
    val riseOffset = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(popup.id) {
        launch { riseOffset.animateTo(-40f, tween(600, easing = LinearEasing)) }
        delay(300)
        alpha.animateTo(0f, tween(300))
        onFinished()
    }
    Box(
        modifier = Modifier.offset(x = popup.x - 16.dp, y = popup.y + riseOffset.value.dp - 12.dp)
    ) {
        Text(
            text = popup.text,
            color = popup.color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alpha(alpha.value)
        )
    }
}
