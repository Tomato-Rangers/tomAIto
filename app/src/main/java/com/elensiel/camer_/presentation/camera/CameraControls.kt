package com.elensiel.camer_.presentation.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elensiel.camer_.R
import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.domain.model.FlashMode
import com.elensiel.camer_.presentation.components.ActionButton
import com.elensiel.camer_.presentation.components.PillOption
import com.elensiel.camer_.presentation.components.ZoomButtons
import com.elensiel.camer_.presentation.components.glassPill
import com.elensiel.camer_.presentation.components.icon
import com.tomatorangers.detection.presentation.components.ConfidenceThresholdDialog

private val EdgePadding = 24.dp
private val MenuArrangement = Arrangement.spacedBy(12.dp)

@Composable
fun CameraControls(
    modifier: Modifier = Modifier,
    state: CameraUiState,
    onCaptureClick: () -> Unit,
    onFlipClick: () -> Unit,
    onFlashModeSelect: (FlashMode) -> Unit,
    onTorchClick: () -> Unit,
    onAspectRatioClick: (CaptureAspectRatio) -> Unit,
    onZoomPreset: (Float) -> Unit,
    onGalleryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDetectionClick: () -> Unit,
    onConfidenceChange: (Float) -> Unit,
    onConfidenceCommit: (Float) -> Unit,
) {
    Box(modifier = modifier) {
        TopControls(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
            onFlashModeSelect = onFlashModeSelect,
            onTorchClick = onTorchClick,
            onDetectionClick = onDetectionClick,
            onConfidenceChange = onConfidenceChange,
            onConfidenceCommit = onConfidenceCommit,
            onSettingsClick = onSettingsClick,
        )

        BottomControls(
            modifier = Modifier.align(Alignment.BottomCenter),
            state = state,
            onCaptureClick = onCaptureClick,
            onFlipClick = onFlipClick,
            onAspectRatioClick = onAspectRatioClick,
            onZoomPreset = onZoomPreset,
            onGalleryClick = onGalleryClick,
        )
    }
}

// ---------------------------------------------------------------
// Groups
// ---------------------------------------------------------------

/** Top row: flash & torch (left), live detection (center), settings (right). */
@Composable
private fun TopControls(
    modifier: Modifier = Modifier,
    state: CameraUiState,
    onFlashModeSelect: (FlashMode) -> Unit,
    onTorchClick: () -> Unit,
    onDetectionClick: () -> Unit,
    onConfidenceChange: (Float) -> Unit,
    onConfidenceCommit: (Float) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = EdgePadding, end = EdgePadding, top = EdgePadding),
    ) {
        // Only if the device has a flash unit.
        if (state.hasFlashUnit) {
            FlashMenu(
                modifier = Modifier.align(Alignment.TopStart),
                flashMode = state.flashMode,
                torchEnabled = state.torchEnabled,
                onFlashModeSelect = onFlashModeSelect,
                onTorchClick = onTorchClick,
            )
        }

        ActionButton(
            modifier = Modifier.align(Alignment.TopCenter),
            onClick = onDetectionClick,
            icon =
                if (state.detectionEnabled) painterResource(R.drawable.detection_and_zone)
                else painterResource(R.drawable.detection_and_zone_off),
            contentDescription = "Live detection",
        )

        SettingsMenu(
            modifier = Modifier.align(Alignment.TopEnd),
            confidenceThreshold = state.confidenceThreshold,
            onConfidenceChange = onConfidenceChange,
            onConfidenceCommit = onConfidenceCommit,
            onMoreSettingsClick = onSettingsClick,
        )
    }
}

@Composable
private fun FlashMenu(
    modifier: Modifier = Modifier,
    flashMode: FlashMode,
    torchEnabled: Boolean,
    onFlashModeSelect: (FlashMode) -> Unit,
    onTorchClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = MenuArrangement,
    ) {
        ActionButton(
            onClick = { expanded = !expanded },
            icon = painterResource(
                if (torchEnabled) R.drawable.flashlight_on
                else flashMode.icon
            ),
            contentDescription = "Flash options"
        )

        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = MenuArrangement,
            ) {
                FlashMode.entries.forEach { mode ->
                    ActionButton(
                        onClick = {
                            onFlashModeSelect(mode)
                            expanded = false
                        },
                        icon = painterResource(mode.icon),
                        contentDescription = "Flash ${mode.name.lowercase()}"
                    )
                }

                ActionButton(
                    onClick = {
                        onTorchClick()
                        expanded = false
                    },
                    icon = painterResource(
                        if (torchEnabled) R.drawable.flashlight_on
                        else R.drawable.flashlight_off
                    ),
                    contentDescription = "Torch"
                )
            }
        }
    }
}

@Composable
private fun SettingsMenu(
    modifier: Modifier = Modifier,
    confidenceThreshold: Float,
    onConfidenceChange: (Float) -> Unit,
    onConfidenceCommit: (Float) -> Unit,
    onMoreSettingsClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showSlider by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = MenuArrangement,
    ) {
        ActionButton(
            onClick = { expanded = !expanded },
            icon = painterResource(R.drawable.settings),
            contentDescription = "Flash options"
        )

        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = MenuArrangement,
            ) {
                ActionButton(
                    onClick = {
                        expanded = false
                        showSlider = true
                    },
                    icon = painterResource(R.drawable.tune),
                    contentDescription = "Confidence threshold",
                )

                ActionButton(
                    onClick = onMoreSettingsClick,
                    icon = painterResource(R.drawable.more_horiz),
                    contentDescription = "More settings",
                )
            }
        }
    }

    if (showSlider) {
        ConfidenceThresholdDialog(
            initial = confidenceThreshold,
            onChange = onConfidenceChange,
            onCommit = onConfidenceCommit,
            onDismiss = { showSlider = false },
        )
    }
}

/** Bottom stack: zoom presets, capture row, aspect ratio selector. */
@Composable
private fun BottomControls(
    modifier: Modifier = Modifier,
    state: CameraUiState,
    onCaptureClick: () -> Unit,
    onFlipClick: () -> Unit,
    onAspectRatioClick: (CaptureAspectRatio) -> Unit,
    onZoomPreset: (Float) -> Unit,
    onGalleryClick: () -> Unit,
) {
    Column(
        modifier = modifier.padding(bottom = EdgePadding),
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
            onSelect = onAspectRatioClick,
        )
    }
}

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
