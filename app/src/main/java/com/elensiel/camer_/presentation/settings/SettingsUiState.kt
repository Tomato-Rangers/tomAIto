package com.elensiel.camer_.presentation.settings

import com.elensiel.camer_.domain.model.ImageFormat

data class SettingsUiState(
    val alwaysSaveEnabled: Boolean = false,
    val imageFormat: ImageFormat = ImageFormat.JPEG,
    val availableImageFormats: List<ImageFormat> = listOf(ImageFormat.JPEG),
    val shutterSoundEnabled: Boolean = false,
    val shutterSoundLocked: Boolean = false,
)
