package com.elensiel.camer_.presentation.settings

import com.elensiel.camer_.domain.model.ImageFormat
import com.elensiel.camer_.domain.model.VolumeKeyShutter

data class SettingsUiState(
    val alwaysSaveEnabled: Boolean = false,
    val imageFormat: ImageFormat = ImageFormat.JPEG,
    val availableImageFormats: List<ImageFormat> = listOf(ImageFormat.JPEG),
    val saveLocation: String = "DCIM/camer-",
    val shutterSoundEnabled: Boolean = false,
    val shutterSoundLocked: Boolean = false,
    val volumeKeyShutter: VolumeKeyShutter = VolumeKeyShutter.OFF,
)
