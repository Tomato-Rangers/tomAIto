package com.elensiel.camer_.data.camera

import android.content.Context
import android.media.MediaActionSound
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asFlow
import coil3.Bitmap
import com.elensiel.camer_.domain.model.CameraState
import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.domain.model.CapturedPhoto
import com.elensiel.camer_.domain.model.FlashMode
import com.elensiel.camer_.domain.model.ImageFormat
import com.elensiel.camer_.domain.model.LensFacing
import com.elensiel.camer_.domain.repository.CameraRepository
import com.tomatorangers.detection.domain.model.CameraFrame
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraXRepository(private val context: Context) : CameraRepository {

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var imageFormat: ImageFormat? = null

    private var flashMode = FlashMode.OFF
    private var shutterSoundEnabled = false
    private val shutterSound = MediaActionSound().apply {
        load(MediaActionSound.SHUTTER_CLICK)
    }

    private val formatCache = mutableMapOf<LensFacing, Set<ImageFormat>>()
    private val formatCacheLock = Mutex()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    override val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    private val _state = MutableStateFlow(CameraState())
    override val state = _state.asStateFlow()

    private val _captureStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val captureStarted = _captureStarted.asSharedFlow()

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val _frames = MutableSharedFlow<CameraFrame>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val frames: SharedFlow<CameraFrame> = _frames.asSharedFlow()


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
        val cameraSelector = lensFacing.toCameraSelector()
        val effectiveFormat =
            if (format in getSupportedFormats(lensFacing)) format
            else ImageFormat.JPEG

        // Shared so Preview and ImageCapture use the same ratio.
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    aspectRatio.toCameraXRatio(),
                    AspectRatioStrategy.FALLBACK_RULE_AUTO,
                )
            )
            .build()

        // No PreviewView: the SurfaceRequest is collected by the Compose
        // CameraXViewfinder, which supplies the surface.
        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also { it.setSurfaceProvider { request -> _surfaceRequest.value = request } }

        // Flash mode is set here so it survives a rebind. Formats that need
        // transcoding are captured as JPEG (see toCameraXFormat).
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(resolutionSelector)
            .setOutputFormat(effectiveFormat.toCameraX())
            .build()
            .also { it.flashMode = flashMode.toCameraX() }

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            aspectRatio.toCameraXRatio(),
                            AspectRatioStrategy.FALLBACK_RULE_AUTO,
                        )
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also { a ->
                a.setAnalyzer(analysisExecutor) { proxy ->
                    proxy.use {
                        if (_frames.subscriptionCount.value == 0) return@use
                        val full = it.toBitmap()

                        // ViewPort crop: toBitmap() ignores it, so apply manually.
                        val rect = it.cropRect
                        val bitmap =
                            if (rect.width() == full.width && rect.height() == full.height) full
                            else Bitmap.createBitmap(
                                full,
                                rect.left,
                                rect.top,
                                rect.width(),
                                rect.height(),
                            )
                        _frames.tryEmit(CameraFrame(bitmap, it.imageInfo.rotationDegrees))
                    }
                }
            }

        // Makes Preview and ImageCapture crop to the same region, so the saved
        // photo matches the preview. This is how 1:1 works: the sensor streams
        // 4:3 and the ViewPort crops to a square. Only applies to grouped use cases.
        val viewPort = ViewPort.Builder(aspectRatio.toViewPortRational(), preview.targetRotation)
            .setScaleType(ViewPort.FILL_CENTER)
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(preview)
            .addUseCase(capture)
            .addUseCase(analysis)
            .build()

        provider.unbindAll()
        val cam = provider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)

        camera = cam
        imageCapture = capture
        imageFormat = effectiveFormat

        _state.update { it.copy(hasFlashUnit = cam.cameraInfo.hasFlashUnit()) }

        // unbindAll() turned the torch off.
        cam.cameraControl.enableTorch(_state.value.torchEnabled)

        try {
            cam.cameraInfo.zoomState.asFlow().collect { z ->
                _state.update {
                    it.copy(
                        zoomRatio = z.zoomRatio,
                        minZoomRatio = z.minZoomRatio,
                        maxZoomRatio = z.maxZoomRatio,
                    )
                }
            }
        } finally {
            // Identity check: an old, cancelled bind() must not wipe the
            // handles of a newer bind() that already replaced them.
            if (camera === cam) {
                camera = null
                imageCapture = null
                imageFormat = null
            }
        }
    }

    override suspend fun getSupportedFormats(lensFacing: LensFacing): Set<ImageFormat> =
        formatCacheLock.withLock {
            formatCache.getOrPut(lensFacing) {
                val provider = ProcessCameraProvider.awaitInstance(context)
                val selector = lensFacing.toCameraSelector()

                val native = ImageCapture
                    .getImageCaptureCapabilities(provider.getCameraInfo(selector))
                    .supportedOutputFormats
                    .mapNotNull { it.toImageFormat() }
                    .toSet()

                // Transcoded formats are always available: they're converted from JPEG.
                native + ImageFormat.entries.filter { it.needsTranscoding }
            }
        }

    override suspend fun takePhoto(): CapturedPhoto = suspendCancellableCoroutine { cont ->
        val capture = imageCapture
        val format = imageFormat

        if (capture == null || format == null) {
            cont.resumeWithException(IllegalStateException("Camera not bound."))
            return@suspendCancellableCoroutine
        }

        val photoFile = newCacheFile(format)

        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onCaptureStarted() {
                    _captureStarted.tryEmit(Unit)
                    if (shutterSoundEnabled) shutterSound.play(MediaActionSound.SHUTTER_CLICK)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    cont.resume(CapturedPhoto(photoFile, format))
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            }
        )
    }

    override fun focusOn(x: Float, y: Float) {
        val res = _surfaceRequest.value?.resolution ?: return

        // camera is null while unbound or rebinding; skip the whole request then.
        camera?.cameraControl?.let {
            val point = SurfaceOrientedMeteringPointFactory(
                res.width.toFloat(),
                res.height.toFloat(),
            ).createPoint(x, y)

            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            )
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()

            it.startFocusAndMetering(action)
        }
    }

    override fun setFlashMode(mode: FlashMode) {
        flashMode = mode
        imageCapture?.flashMode = mode.toCameraX()
    }

    override fun setTorch(enabled: Boolean) {
        if (!_state.value.hasFlashUnit) return
        _state.update { it.copy(torchEnabled = enabled) }
        camera?.cameraControl?.enableTorch(enabled)
    }

    override fun setShutterSound(enabled: Boolean) {
        shutterSoundEnabled = enabled
    }

    override fun setZoom(ratio: Float) {
        // camera is null while unbound or rebinding.
        camera?.cameraControl?.let {
            val s = _state.value
            it.setZoomRatio(ratio.coerceIn(s.minZoomRatio, s.maxZoomRatio))
        }
    }

    override fun zoomBy(factor: Float) = setZoom(_state.value.zoomRatio * factor)

    private fun newCacheFile(format: ImageFormat): File {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
            .format(System.currentTimeMillis())

        // Transcoded formats are captured as JPEG first; MediaRepository converts on save.
        val extension = if (format.needsTranscoding) "jpg" else format.fileExtension
        return File(context.cacheDir, "$name.$extension")
    }
}
