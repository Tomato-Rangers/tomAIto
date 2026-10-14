package com.elensiel.camer_.domain.model

data class Settings(
    val alwaysSaveEnabled: Boolean = false,
    val aspectRatio: CaptureAspectRatio = CaptureAspectRatio.RATIO_4_3,
    val flashMode: FlashMode = FlashMode.OFF,
    val imageFormat: ImageFormat = ImageFormat.JPEG,
    val saveLocation: String = "DCIM/camer-",
    val shutterSoundEnabled: Boolean = false,
    val volumeKeyShutter: VolumeKeyShutter = VolumeKeyShutter.OFF,
)
