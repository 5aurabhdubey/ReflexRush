package com.spaakkai.reflexgame.game

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spaakkai.reflexgame.ui.theme.Accent
import com.spaakkai.reflexgame.ui.theme.BgCard
import com.spaakkai.reflexgame.ui.theme.GlowAccent
import com.spaakkai.reflexgame.ui.theme.GradientBackground
import com.spaakkai.reflexgame.ui.theme.TargetHit
import com.spaakkai.reflexgame.ui.theme.TextPrimary
import com.spaakkai.reflexgame.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun GameOverScreen(state: GameUiState, onPlayAgain: () -> Unit, onMenu: () -> Unit) {
    val context = LocalContext.current
    var showTitle by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showTitle = true
        delay(250)
        showStats = true
        delay(300)
        showButtons = true
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(visible = showTitle, enter = fadeIn(tween(300))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TrophyGlyph()
                    Spacer(Modifier.height(8.dp))
                    Text("GAME OVER", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text(
                        text = if (state.isNewHighScore) "You just set a new personal best." else "Better luck next time.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    if (state.isNewHighScore) {
                        Spacer(Modifier.height(6.dp))
                        NewRecordBadge()
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(
                visible = showStats,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 }
            ) {
                ResultsCard(state)
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(visible = showButtons, enter = fadeIn(tween(250))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onPlayAgain,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        modifier = Modifier.fillMaxWidth(0.7f).height(52.dp)
                    ) {
                        Text("PLAY AGAIN", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextButton(label = "HOME", modifier = Modifier.width(110.dp), onClick = onMenu)
                        OutlinedTextButton(
                            label = "SHARE",
                            modifier = Modifier.width(110.dp),
                            onClick = {
                                val shareText = "I scored ${state.score} in Reflex Rush! " +
                                    "Best combo x${state.peakCombo}, accuracy ${state.accuracyPercent}%. Can you beat me?"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share your score"))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrophyGlyph() {
    val transition = rememberInfiniteTransition(label = "trophyBounce")
    val scale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyScale"
    )
    Text(
        "\uD83C\uDFC6",
        fontSize = 44.sp,
        modifier = Modifier.scale(scale)
    )
}

@Composable
private fun NewRecordBadge() {
    val transition = rememberInfiniteTransition(label = "recordGlow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recordGlowAlpha"
    )
    Text(
        "NEW HIGH SCORE!",
        fontSize = 14.sp,
        color = TargetHit,
        fontWeight = FontWeight.Bold,
        style = androidx.compose.ui.text.TextStyle(
            shadow = Shadow(color = GlowAccent.copy(alpha = glowAlpha), blurRadius = 24f)
        )
    )
}

@Composable
private fun ResultsCard(state: GameUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        StatRow("Score", "${state.score}")
        StatRow("Best score", "${state.highScore}")
        StatRow("Highest combo", "\u00d7${state.peakCombo}")
        StatRow("Avg reaction time", "${state.avgReactionMs} ms")
        StatRow("Accuracy", "${state.accuracyPercent}%")
        StatRow("Peak difficulty reached", state.difficultyLabel, isLast = true)
    }
}

@Composable
private fun StatRow(label: String, value: String, isLast: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
    }
}

@Composable
private fun OutlinedTextButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
