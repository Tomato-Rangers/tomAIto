package com.elensiel.camer_.domain.repository

import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.LifecycleOwner
import com.elensiel.camer_.domain.model.CameraState
import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.domain.model.CapturedPhoto
import com.elensiel.camer_.domain.model.FlashMode
import com.elensiel.camer_.domain.model.ImageFormat
import com.elensiel.camer_.domain.model.LensFacing
import com.tomatorangers.detection.domain.model.CameraFrame
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface CameraRepository {
    val surfaceRequest: StateFlow<SurfaceRequest?>
    val state: StateFlow<CameraState>
    val captureStarted: SharedFlow<Unit>
    val frames: SharedFlow<CameraFrame>

    suspend fun bind(
        lifecycleOwner: LifecycleOwner,
        lensFacing: LensFacing,
        aspectRatio: CaptureAspectRatio,
        format: ImageFormat,
    )

    suspend fun getSupportedFormats(lensFacing: LensFacing): Set<ImageFormat>
    suspend fun takePhoto(): CapturedPhoto
    fun focusOn(x: Float, y: Float)
    fun setFlashMode(mode: FlashMode)
    fun setTorch(enabled: Boolean)
    fun setShutterSound(enabled: Boolean)
    fun setZoom(ratio: Float)
    fun zoomBy(factor: Float)
}
