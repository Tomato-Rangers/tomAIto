package com.elensiel.camer_.data.camera

import android.content.Context
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asFlow
import com.elensiel.camer_.domain.model.CameraState
import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.domain.model.CapturedPhoto
import com.elensiel.camer_.domain.model.FlashMode
import com.elensiel.camer_.domain.model.ImageFormat
import com.elensiel.camer_.domain.model.LensFacing
import com.elensiel.camer_.domain.repository.CameraRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CameraXRepository(private val context: Context) : CameraRepository {

    private val session = CameraSession()
    private val formats = SupportedFormats(context)
    private val useCaseGroup = CameraUseCaseBuilder(session)
    private val controller = CameraController(session)

    private val _captureStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val capturer = PhotoCapturer(context, session) { _captureStarted.tryEmit(Unit) }

    override val surfaceRequest: StateFlow<SurfaceRequest?> = session.surfaceRequest.asStateFlow()
    override val state: StateFlow<CameraState> = session.state.asStateFlow()
    override val captureStarted = _captureStarted.asSharedFlow()

    /**
     * Binds the camera to [lifecycleOwner] and starts the preview.
     *
     * Suspends until the calling coroutine is cancelled (it collects zoom
     * state forever). Cancellation happens when the LaunchedEffect keys
     * change (lens, aspect ratio, format). Every call is a full unbind/rebind.
     *
     * @param format requested output format. Falls back to JPEG if the
     *   camera can't produce it.
     */
    override suspend fun bind(
        lifecycleOwner: LifecycleOwner,
        lensFacing: LensFacing,
        aspectRatio: CaptureAspectRatio,
        format: ImageFormat,
    ) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val effectiveFormat =
            if (format in getSupportedFormats(lensFacing)) format
            else ImageFormat.JPEG

        val built = useCaseGroup.build(aspectRatio, effectiveFormat, controller.flashMode)


        provider.unbindAll()
        val cam = provider.bindToLifecycle(
            lifecycleOwner,
            lensFacing.toCameraSelector(),
            built.useCaseGroup,
        )
        session.attach(cam, built.imageCapture, built.imageFormat)

        session.state.update { it.copy(hasFlashUnit = cam.cameraInfo.hasFlashUnit()) }
        cam.cameraControl.enableTorch(session.state.value.torchEnabled)

        try {
            cam.cameraInfo.zoomState.asFlow().collect { z ->
                session.state.update {
                    it.copy(
                        zoomRatio = z.zoomRatio,
                        minZoomRatio = z.minZoomRatio,
                        maxZoomRatio = z.maxZoomRatio,
                    )
                }
            }
        } finally {
            session.detach(cam)
        }
    }


    override suspend fun getSupportedFormats(lensFacing: LensFacing): Set<ImageFormat> =
        formats.get(lensFacing)

    override suspend fun takePhoto(): CapturedPhoto = capturer.takePhoto()

    override fun focusOn(x: Float, y: Float) = controller.focusOn(x, y)
    override fun setFlashMode(mode: FlashMode) = controller.setFlashMode(mode)
    override fun setTorch(enabled: Boolean) = controller.setTorch(enabled)
    override fun setZoom(ratio: Float) = controller.setZoom(ratio)
    override fun zoomBy(factor: Float) = controller.zoomBy(factor)
    override fun setShutterSound(enabled: Boolean) {
        capturer.shutterSoundEnabled = enabled
    }
}
