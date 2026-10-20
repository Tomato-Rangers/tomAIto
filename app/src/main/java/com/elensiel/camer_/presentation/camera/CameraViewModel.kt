package com.elensiel.camer_.presentation.camera

import android.util.Log
import androidx.camera.core.SurfaceRequest
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.elensiel.camer_.CamApp
import com.elensiel.camer_.domain.model.BindConfig
import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.domain.model.CaptureResult
import com.elensiel.camer_.domain.model.CapturedPhoto
import com.elensiel.camer_.domain.model.FlashMode
import com.elensiel.camer_.domain.model.LensFacing
import com.elensiel.camer_.domain.repository.CameraRepository
import com.elensiel.camer_.domain.repository.LensRepository
import com.elensiel.camer_.domain.repository.SettingsRepository
import com.elensiel.camer_.domain.usecase.CapturePhotoUseCase
import com.elensiel.camer_.domain.usecase.DiscardPhotoUseCase
import com.elensiel.camer_.domain.usecase.ObserveBindConfigUseCase
import com.elensiel.camer_.domain.usecase.SavePhotoUseCase
import com.tomatorangers.detection.domain.model.DetectionResult
import com.tomatorangers.detection.domain.usecase.ObserveLiveDetectionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CameraViewModel(
    private val cameraRepo: CameraRepository,
    private val settingsRepo: SettingsRepository,
    private val lensRepo: LensRepository,
    private val capturePhoto: CapturePhotoUseCase,
    private val savePhoto: SavePhotoUseCase,
    private val discardPhoto: DiscardPhotoUseCase,
    observeBindConfig: ObserveBindConfigUseCase,
    observeLiveDetection: ObserveLiveDetectionUseCase,
) : ViewModel() {

    // Must be declared before uiState, which reads it during initialization.
    private val capturedPhoto = MutableStateFlow<CapturedPhoto?>(null)
    private var saveJob: Job? = null

    val uiState: StateFlow<CameraUiState> = combine(
        settingsRepo.settings,
        cameraRepo.state,
        capturedPhoto,
    ) { settings, cam, photo ->
        CameraUiState(
            aspectRatio = settings.aspectRatio,
            flashMode = settings.flashMode,
            torchEnabled = cam.torchEnabled,
            hasFlashUnit = cam.hasFlashUnit,
            zoomRatio = cam.zoomRatio,
            minZoomRatio = cam.minZoomRatio,
            maxZoomRatio = cam.maxZoomRatio,
            capturedPhotoFile = photo?.file,
            volumeKeyShutter = settings.volumeKeyShutter,
            detectionEnabled = settings.liveDetectionEnabled,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CameraUiState(),
    )

    val bindConfig: StateFlow<BindConfig?> = observeBindConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val surfaceRequest: StateFlow<SurfaceRequest?> = cameraRepo.surfaceRequest
    val captureStarted: SharedFlow<Unit> = cameraRepo.captureStarted

    /**
     * Live detection result for the camera preview, or `null` when there is nothing to draw
     * (live detection is off, or no frame has been processed yet).
     *
     * Boxes are in the coordinate space of the analyzed frame after rotation, and are mirrored
     * for the front lens so they line up with the mirrored preview.
     *
     * Shared via [SharingStarted.WhileSubscribed] with a 5 s grace period, so detection stops
     * shortly after the UI stops collecting (e.g. app backgrounded or settings opened), and a
     * quick return such as a configuration change doesn't restart the pipeline.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val detection: StateFlow<DetectionResult?> = settingsRepo.settings
        .map { it.liveDetectionEnabled }
        .distinctUntilChanged()
        // Restarts the inner flow whenever the setting changes:
        //  - off: emits a single null, which clears any boxes still on screen.
        //  - on: runs detection on camera frames and mirrors the boxes for the front lens.
        // While off, nothing collects cameraRepo.frames, so the analyzer skips bitmap
        // conversion and the model never runs.
        .flatMapLatest { enabled ->
            if (!enabled) flowOf(null)
            else combine(
                observeLiveDetection(cameraRepo.frames),
                lensRepo.lensFacing,
            ) { result, lens ->
                if (lens == LensFacing.FRONT) result.mirrored()
                else result
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            settingsRepo.settings.map { it.flashMode }.distinctUntilChanged()
                .collect(cameraRepo::setFlashMode)
        }
        viewModelScope.launch {
            settingsRepo.settings.map { it.shutterSoundEnabled }.distinctUntilChanged()
                .collect(cameraRepo::setShutterSound)
        }
        viewModelScope.launch {
            settingsRepo.settings.map { it.liveDetectionEnabled }.distinctUntilChanged()
                .collect(cameraRepo::setAnalysisEnabled)
        }
    }

    suspend fun bindCamera(owner: LifecycleOwner, config: BindConfig) =
        cameraRepo.bind(owner, config.lensFacing, config.aspectRatio, config.imageFormat)

    // ---------------------------------------------------------------
    // Capture and review
    // ---------------------------------------------------------------

    fun onCapture() {
        if (capturedPhoto.value != null) return

        viewModelScope.launch {
            when (val result = capturePhoto()) {
                is CaptureResult.Pending ->
                    // A second tap can finish after another photo is already under review.
                    if (capturedPhoto.value == null) capturedPhoto.value = result.photo
                    else discardPhoto(result.photo)

                is CaptureResult.Saved -> Unit
                is CaptureResult.Failed -> Log.e(TAG, "Capture failed.", result.cause)
            }
        }
    }

    fun onSave() {
        if (saveJob?.isActive == true) return // double save guard
        val photo = capturedPhoto.value ?: return
        saveJob = viewModelScope.launch {
            savePhoto(photo)
                .onSuccess { capturedPhoto.value = null }
                .onFailure { Log.e(TAG, "Save failed.", it) }
        }
    }

    fun onDiscard() {
        val photo = capturedPhoto.value ?: return
        capturedPhoto.value = null
        viewModelScope.launch { discardPhoto(photo) }
    }

    // ---------------------------------------------------------------
    // Camera controls
    // ---------------------------------------------------------------

    fun onAspectRatio(ratio: CaptureAspectRatio) {
        viewModelScope.launch { settingsRepo.setAspectRatio(ratio) }
    }

    fun onFlashMode(mode: FlashMode) {
        cameraRepo.setFlashMode(mode)
        viewModelScope.launch { settingsRepo.setFlashMode(mode) }
    }

    fun onToggleTorch() = cameraRepo.setTorch(!uiState.value.torchEnabled)

    fun onFlip() = lensRepo.flip()
    fun onFocus(point: Offset) = cameraRepo.focusOn(point.x, point.y)
    fun onZoomBy(factor: Float) = cameraRepo.zoomBy(factor)
    fun onZoomTo(ratio: Float) = cameraRepo.setZoom(ratio)

    fun onToggleDetection() {
        viewModelScope.launch { settingsRepo.setLiveDetectionEnabled(!uiState.value.detectionEnabled) }
    }

    // ---------------------------------------------------------------
    // Factory
    // ---------------------------------------------------------------

    companion object {
        // used for logs
        private const val TAG = "Camera"

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as CamApp).container
                CameraViewModel(
                    c.cameraRepo,
                    c.settingsRepo,
                    c.lensRepo,
                    c.capturePhoto,
                    c.savePhoto,
                    c.discardPhoto,
                    c.observeBindConfig,
                    c.observeLiveDetection,
                )
            }
        }
    }
}
