package com.elensiel.camer_.presentation.camera

import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.domain.model.FlashMode
import com.elensiel.camer_.domain.model.VolumeKeyShutter
import java.io.File

data class CameraUiState(
    val capturedPhotoFile: File? = null,
    val aspectRatio: CaptureAspectRatio = CaptureAspectRatio.RATIO_4_3,
    val flashMode: FlashMode = FlashMode.OFF,
    val hasFlashUnit: Boolean = true,
    val torchEnabled: Boolean = false,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val volumeKeyShutter: VolumeKeyShutter = VolumeKeyShutter.OFF,
)
