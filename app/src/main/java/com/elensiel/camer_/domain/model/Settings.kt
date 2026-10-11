package com.elensiel.camer_.domain.model

data class Settings(
    val alwaysSaveEnabled: Boolean = false,
    val aspectRatio: CaptureAspectRatio = CaptureAspectRatio.RATIO_4_3,
    val flashEnabled: Boolean = false,
    val imageFormat: ImageFormat = ImageFormat.JPEG,
    val shutterSoundEnabled: Boolean = false,
)
