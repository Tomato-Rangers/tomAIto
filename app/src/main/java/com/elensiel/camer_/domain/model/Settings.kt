package com.elensiel.camer_.domain.model

import com.tomatorangers.detection.domain.model.DetectionDefaults

data class Settings(
    val alwaysSaveEnabled: Boolean = false,
    val aspectRatio: CaptureAspectRatio = CaptureAspectRatio.RATIO_4_3,
    val flashMode: FlashMode = FlashMode.OFF,
    val imageFormat: ImageFormat = ImageFormat.JPEG,
    val liveDetectionEnabled: Boolean = true,
    val saveLocation: String = "DCIM/camer-",
    val shutterSoundEnabled: Boolean = false,
    val shutterSoundLocked: Boolean = false,
    val volumeKeyShutter: VolumeKeyShutter = VolumeKeyShutter.OFF,
    val confidenceThreshold: Float = DetectionDefaults.CONFIDENCE_THRESHOLD,
)
