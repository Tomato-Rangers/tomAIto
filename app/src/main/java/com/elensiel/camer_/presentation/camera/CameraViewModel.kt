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
import com.elensiel.camer_.domain.repository.CameraRepository
import com.elensiel.camer_.domain.repository.LensRepository
import com.elensiel.camer_.domain.repository.SettingsRepository
import com.elensiel.camer_.domain.usecase.CapturePhotoUseCase
import com.elensiel.camer_.domain.usecase.DiscardPhotoUseCase
import com.elensiel.camer_.domain.usecase.ObserveBindConfigUseCase
import com.elensiel.camer_.domain.usecase.SavePhotoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
) : ViewModel() {
    private val capturedPhoto = MutableStateFlow<CapturedPhoto?>(null)

    val surfaceRequest: StateFlow<SurfaceRequest?> = cameraRepo.surfaceRequest
    val captureStarted: SharedFlow<Unit> = cameraRepo.captureStarted
    val bindConfig: StateFlow<BindConfig?> = observeBindConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CameraUiState()
    )

    // push settings changes into the camera
    init {
        viewModelScope.launch {
            settingsRepo.settings.map { it.flashMode }.distinctUntilChanged()
                .collect(cameraRepo::setFlashMode)
        }
        viewModelScope.launch {
            settingsRepo.settings.map { it.shutterSoundEnabled }.distinctUntilChanged()
                .collect(cameraRepo::setShutterSound)
        }
    }

    suspend fun bindCamera(
        owner: LifecycleOwner,
        config: BindConfig,
    ) = cameraRepo.bind(
        owner, config.lensFacing, config.aspectRatio, config.imageFormat
    )

    // ---------------------------------------------------------------
    // Capture and review
    // ---------------------------------------------------------------

    fun onCapture() {
        if (capturedPhoto.value != null) return

        viewModelScope.launch {
            when (val result = capturePhoto()) {
                is CaptureResult.Pending ->
                    if (capturedPhoto.value == null) capturedPhoto.value = result.photo
                    else discardPhoto(result.photo)

                is CaptureResult.Saved -> Unit
                is CaptureResult.Failed -> Log.e("Camera", "Capture failed.", result.cause)
            }
        }
    }

    fun onSave() {
        val photo = capturedPhoto.value ?: return
        viewModelScope.launch {
            savePhoto(photo)
                .onSuccess { capturedPhoto.value = null }
                .onFailure { Log.e("Camera", "Save failed.", it) }
        }
    }

    fun onDiscard() {
        val photo = capturedPhoto.value ?: return
        capturedPhoto.value = null
        viewModelScope.launch { discardPhoto(photo) }
    }

    // ---------------------------------------------------------------
    // Live camera controls
    // ---------------------------------------------------------------

    fun onFlip() = lensRepo.flip()
    fun onToggleTorch() = cameraRepo.setTorch(!uiState.value.torchEnabled)
    fun onFocus(point: Offset) = cameraRepo.focusOn(point.x, point.y)
    fun onZoomBy(factor: Float) = cameraRepo.zoomBy(factor)
    fun onZoomTo(ratio: Float) = cameraRepo.setZoom(ratio)

    // ---------------------------------------------------------------
    // Persisted settings
    // ---------------------------------------------------------------

    fun onAspectRatio(ratio: CaptureAspectRatio) {
        viewModelScope.launch { settingsRepo.setAspectRatio(ratio) }
    }

    fun onCycleFlash() {
        viewModelScope.launch { settingsRepo.setFlashMode(uiState.value.flashMode.next()) }
    }

    companion object {
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
                )
            }
        }
    }
}
