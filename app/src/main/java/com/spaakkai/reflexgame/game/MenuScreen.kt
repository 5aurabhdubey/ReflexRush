package com.spaakkai.reflexgame.game

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.spaakkai.reflexgame.ui.theme.Accent
import com.spaakkai.reflexgame.ui.theme.AccentSoft
import com.spaakkai.reflexgame.ui.theme.BgCard
import com.spaakkai.reflexgame.ui.theme.ComboColor
import com.spaakkai.reflexgame.ui.theme.GradientBackground
import com.spaakkai.reflexgame.ui.theme.TextPrimary
import com.spaakkai.reflexgame.ui.theme.TextSecondary

@Composable
fun MenuScreen(
    highScore: Int,
    bestCombo: Int,
    gamesPlayed: Int,
    onStart: () -> Unit
) {
    var showHowToPlay by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            RadarLogo()
            Spacer(Modifier.height(4.dp))
            GradientTitle()
            Spacer(Modifier.height(6.dp))
            Text(
                text = "TEST YOUR REFLEXES. BEAT YOUR BEST.",
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))
            StatsCard(highScore = highScore, bestCombo = bestCombo, gamesPlayed = gamesPlayed)

            Spacer(Modifier.height(24.dp))
            PrimaryGlowButton(label = "PLAY", trailingGlyph = "\u25B6", onClick = onStart)

            Spacer(Modifier.height(14.dp))
            SecondaryButton(label = "HOW TO PLAY", onClick = { showHowToPlay = true })

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showHowToPlay) {
        HowToPlayDialog(onDismiss = { showHowToPlay = false })
    }
}

@Composable
private fun RadarLogo() {
    val transition = rememberInfiniteTransition(label = "radarSpin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(6000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "radarRotation"
    )
    Box(
        modifier = Modifier.size(84.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            listOf(1f, 0.72f, 0.46f).forEachIndexed { i, frac ->
                drawCircle(
                    color = Accent.copy(alpha = 0.18f + i * 0.12f),
                    radius = maxRadius * frac,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            rotate(degrees = rotation, pivot = center) {
                drawLine(
                    color = Accent.copy(alpha = 0.6f),
                    start = center,
                    end = Offset(center.x, center.y - maxRadius * 0.95f),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
            drawCircle(color = Accent, radius = maxRadius * 0.12f, center = center)
        }
    }
}

@Composable
private fun GradientTitle() {
    Text(
        "REFLEX",
        fontSize = 36.sp,
        fontWeight = FontWeight.ExtraBold,
        color = TextPrimary
    )
    Text(
        "RUSH",
        fontSize = 36.sp,
        fontWeight = FontWeight.ExtraBold,
        style = TextStyle(
            brush = Brush.horizontalGradient(listOf(Accent, ComboColor))
        )
    )
}

@Composable
private fun StatsCard(highScore: Int, bestCombo: Int, gamesPlayed: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Text("BEST SCORE", fontSize = 11.sp, color = TextSecondary, letterSpacing = 1.sp)
        Text(
            text = "%,d".format(highScore),
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatMini(label = "HIGHEST COMBO", value = "\u00d7$bestCombo", modifier = Modifier.weight(1f))
            StatMini(label = "GAMES PLAYED", value = "$gamesPlayed", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = TextSecondary, letterSpacing = 0.5.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun PrimaryGlowButton(label: String, trailingGlyph: String?, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "primaryButtonScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Accent, AccentSoft)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (trailingGlyph != null) {
                Spacer(Modifier.width(8.dp))
                Text(trailingGlyph, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .border(BorderStroke(1.dp, Color(0x22FFFFFF)), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
    }
}

@Composable
private fun HowToPlayDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(BgCard)
                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Text("HOW TO PLAY", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(Modifier.height(16.dp))
            HowToStep(number = 1, title = "Tap green targets", body = "Tap green energy zones to score points.")
            HowToStep(number = 2, title = "Avoid red bombs", body = "Tapping a bomb costs a life.")
            HowToStep(number = 3, title = "Build combos", body = "Consecutive hits raise your score multiplier.")
            HowToStep(number = 4, title = "Beat your best", body = "React faster as difficulty ramps up.")
            Spacer(Modifier.height(8.dp))
            PrimaryGlowButton(label = "GOT IT", trailingGlyph = null, onClick = onDismiss)
        }
    }
}

@Composable
private fun HowToStep(number: Int, title: String, body: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(body, color = TextSecondary, fontSize = 12.sp)
        }
    }
}
