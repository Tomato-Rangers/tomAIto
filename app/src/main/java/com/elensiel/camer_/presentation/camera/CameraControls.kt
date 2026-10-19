package com.elensiel.camer_.presentation.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elensiel.camer_.R
import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.presentation.components.ActionButton
import com.elensiel.camer_.presentation.components.PillOption
import com.elensiel.camer_.presentation.components.ZoomButtons
import com.elensiel.camer_.presentation.components.glassPill
import com.elensiel.camer_.presentation.components.icon

private val EdgePadding = 24.dp

// ---------------------------------------------------------------
// Screen entry point
// ---------------------------------------------------------------

@Composable
fun CameraControls(
    modifier: Modifier = Modifier,
    state: CameraUiState,
    onCaptureClick: () -> Unit,
    onFlipClick: () -> Unit,
    onFlashClick: () -> Unit,
    onTorchClick: () -> Unit,
    onAspectRatioClick: (CaptureAspectRatio) -> Unit,
    onZoomPreset: (Float) -> Unit,
    onGalleryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDetectionClick: () -> Unit,
) {
    Box(modifier = modifier) {

        // -- Top-left: flash & torch (only if the device has a flash unit) --

        if (state.hasFlashUnit) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = EdgePadding, top = EdgePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionButton(
                    onClick = onFlashClick,
                    icon = painterResource(state.flashMode.icon),
                    contentDescription = "Flash",
                )

                ActionButton(
                    onClick = onTorchClick,
                    icon = painterResource(
                        if (state.torchEnabled) R.drawable.flashlight_on
                        else R.drawable.flashlight_off
                    ),
                    contentDescription = "Torch",
                )
            }
        }

        // -- top-center: live detection --
        ActionButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = EdgePadding),
            onClick = onDetectionClick,
            icon =
                if (state.detectionEnabled) painterResource(R.drawable.detection_and_zone)
                else painterResource(R.drawable.detection_and_zone_off),
            contentDescription = "Live detection",
        )

        // -- Top-right: settings dropdown menu --

        ActionButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = EdgePadding, end = EdgePadding),
            onClick = onSettingsClick,
            icon = painterResource(R.drawable.settings),
            contentDescription = "Settings",
        )

        // -- Bottom: zoom presets, capture row, aspect ratio selector --

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = EdgePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZoomButtons(
                zoomRatio = state.zoomRatio,
                minZoomRatio = state.minZoomRatio,
                maxZoomRatio = state.maxZoomRatio,
                onPreset = onZoomPreset,
            )

            CaptureRow(
                onCaptureClick = onCaptureClick,
                onGalleryClick = onGalleryClick,
                onFlipClick = onFlipClick,
            )

            AspectRatioSelector(
                selected = state.aspectRatio,
                onSelect = onAspectRatioClick
            )
        }
    }
}


// ---------------------------------------------------------------
// Sections
// ---------------------------------------------------------------

@Composable
private fun CaptureRow(
    modifier: Modifier = Modifier,
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFlipClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .glassPill()
            .padding(horizontal = 32.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ActionButton(
            onClick = onGalleryClick,
            icon = painterResource(R.drawable.photo_library),
            contentDescription = "Gallery",
        )

        ActionButton(
            onClick = onCaptureClick,
            icon = painterResource(R.drawable.camera),
            contentDescription = "Take photo",
        )

        ActionButton(
            onClick = onFlipClick,
            icon = painterResource(R.drawable.cameraswitch),
            contentDescription = "Flip camera",
        )
    }
}

@Composable
private fun AspectRatioSelector(
    modifier: Modifier = Modifier,
    selected: CaptureAspectRatio,
    onSelect: (CaptureAspectRatio) -> Unit,
) {
    Row(modifier = modifier.glassPill()) {
        CaptureAspectRatio.entries.forEach { ratio ->
            PillOption(
                text = ratio.label,
                isActive = selected == ratio,
                onClick = { onSelect(ratio) },
            )
        }
    }
}
