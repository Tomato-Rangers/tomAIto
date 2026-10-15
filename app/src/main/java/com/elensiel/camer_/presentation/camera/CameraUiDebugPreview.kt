package com.elensiel.camer_.presentation.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.presentation.theme.CamerTheme

// Viewfinder replaced by a black box.
@Composable
private fun CameraUiDebug(initial: CameraUiState) {
    var state by remember { mutableStateOf(initial) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CameraControls(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 24.dp), // approximate system bar insets
            state = state,
            onCaptureClick = {},
            onFlipClick = {},
            onFlashClick = { state = state.copy(flashMode = state.flashMode) },
            onTorchClick = { state = state.copy(torchEnabled = !state.torchEnabled) },
            onAspectRatioClick = { state = state.copy(aspectRatio = it) },
            onZoomPreset = { state = state.copy(zoomRatio = it) },
            onGalleryClick = {},
            onSettingsClick = {},
        )
    }
}

@Preview(name = "Camera UI - Back Cam", showBackground = true)
@Composable
private fun CameraUiFullPreview() {
    CamerTheme {
        CameraUiDebug(
            CameraUiState(
                aspectRatio = CaptureAspectRatio.RATIO_4_3,
                hasFlashUnit = true,
                zoomRatio = 1f,
                minZoomRatio = 0.5f,
                maxZoomRatio = 40f,
            )
        )
    }
}

@Preview(name = "Camera UI - Front Cam", showBackground = true)
@Composable
private fun CameraUiFrontPreview() {
    CamerTheme {
        CameraUiDebug(
            CameraUiState(
                aspectRatio = CaptureAspectRatio.RATIO_16_9,
                hasFlashUnit = false,
            )
        )
    }
}
