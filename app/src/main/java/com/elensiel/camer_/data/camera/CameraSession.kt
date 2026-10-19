package com.elensiel.camer_.data.camera

import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceRequest
import com.elensiel.camer_.domain.model.CameraState
import com.elensiel.camer_.domain.model.ImageFormat
import kotlinx.coroutines.flow.MutableStateFlow

internal class CameraSession {
    var camera: Camera? = null
    var imageCapture: ImageCapture? = null
    var imageFormat: ImageFormat? = null

    val state = MutableStateFlow(CameraState())
    val surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)

    fun attach(camera: Camera, imageCapture: ImageCapture, imageFormat: ImageFormat) {
        this@CameraSession.camera = camera
        this@CameraSession.imageCapture = imageCapture
        this@CameraSession.imageFormat = imageFormat
    }

    fun detach(camera: Camera) {
        if (this@CameraSession.camera === camera) {
            this@CameraSession.camera = null
            imageCapture = null
            imageFormat = null
        }
    }
}
