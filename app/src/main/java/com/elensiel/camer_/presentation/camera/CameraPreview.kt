package com.elensiel.camer_.presentation.camera

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.elensiel.camer_.domain.model.CaptureAspectRatio
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    surfaceRequest: SurfaceRequest,
    aspectRatio: CaptureAspectRatio,
    captureStarted: Flow<Unit>,
    onTapFocus: (Offset) -> Unit,
    onZoom: (Float) -> Unit,
) {
    val coordinateTransformer = remember { MutableCoordinateTransformer() }

    // Gesture handlers are keyed on surfaceRequest only; this keeps them
    // calling the latest callbacks without restarting.
    val currentOnTapFocus by rememberUpdatedState(onTapFocus)
    val currentOnZoom by rememberUpdatedState(onZoom)

    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusAlpha by animateFloatAsState(
        targetValue = if (focusPoint == null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "focusAlpha",
    )

    val shutterFlash = remember { Animatable(0f) }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(800.milliseconds)
            focusPoint = null
        }
    }

    LaunchedEffect(captureStarted) {
        captureStarted.collectLatest {
            shutterFlash.animateTo(1f, tween(150))
            shutterFlash.animateTo(0f, tween(150))
        }
    }

    Box(modifier = modifier.aspectRatio(aspectRatio.floatValue)) {
        CameraXViewfinder(
            surfaceRequest = surfaceRequest,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(surfaceRequest) {
                    coroutineScope {
                        launch {
                            detectTapGestures { offset ->
                                // Metering needs surface coordinates; the dot is drawn at the raw offset.
                                currentOnTapFocus(with(coordinateTransformer) { offset.transform() })
                                focusPoint = offset
                            }
                        }

                        launch {
                            detectTransformGestures { _, _, zoom, _ -> currentOnZoom(zoom) }
                        }
                    }
                }
                .drawWithContent {
                    drawContent()

                    focusPoint?.let {
                        drawCircle(
                            color = Color.Yellow.copy(alpha = focusAlpha.coerceAtLeast(0.5f)),
                            radius = 45f,
                            center = it,
                            style = Stroke(width = 3f),
                        )
                    }
                },
            coordinateTransformer = coordinateTransformer,
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind { drawRect(Color.Black, alpha = shutterFlash.value) }
        )
    }
}
