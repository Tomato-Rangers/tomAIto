package com.elensiel.camer_.presentation.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow

@Composable
fun ShutterAnimator(
    captureStarted: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    val color = remember { Animatable(Color.Transparent) }

    LaunchedEffect(captureStarted) {
        captureStarted.collect {
            color.animateTo(Color.Black, tween(150))
            color.animateTo(Color.Transparent, tween(150))
        }
    }

    Box(
        modifier = modifier.drawBehind { drawRect(color.value) }
    )
}
