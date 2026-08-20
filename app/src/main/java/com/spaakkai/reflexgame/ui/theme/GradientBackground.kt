package com.spaakkai.reflexgame.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

/**
 * Applies the app's signature dark gradient background to a screen.
 * Wrap the top-level Column/Box of each screen with this instead of a flat color.
 */
@Composable
fun GradientBackground(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgGradientTop, BgGradientBottom)
                )
            )
    ) {
        content()
    }
}
