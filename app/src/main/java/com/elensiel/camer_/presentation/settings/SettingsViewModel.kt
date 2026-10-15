package com.elensiel.camer_.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.elensiel.camer_.CamApp
import com.elensiel.camer_.data.ShutterSoundPolicy
import com.elensiel.camer_.domain.model.ImageFormat
import com.elensiel.camer_.domain.model.VolumeKeyShutter
import com.elensiel.camer_.domain.repository.SettingsRepository
import com.elensiel.camer_.domain.usecase.ObserveFormatOutputUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    observeFormat: ObserveFormatOutputUseCase,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepo.settings,
        observeFormat(),
    ) { s, f ->
        SettingsUiState(
            alwaysSaveEnabled = s.alwaysSaveEnabled,
            imageFormat = f.selected,
            availableImageFormats = f.available,
            saveLocation = s.saveLocation,
            shutterSoundEnabled = s.shutterSoundEnabled,
            shutterSoundLocked = ShutterSoundPolicy.isForced,
            volumeKeyShutter = s.volumeKeyShutter,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState()
    )

    fun onAlwaysSaveEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAlwaysSaveEnabled(enabled) }
    }

    fun onImageFormat(format: ImageFormat) {
        viewModelScope.launch { settingsRepo.setImageFormat(format) }
    }

    fun onSaveLocation(location: String) {
        viewModelScope.launch { settingsRepo.setSaveLocation(location) }
    }

    fun onShutterSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setShutterSoundEnabled(enabled) }
    }

    fun onVolumeKeyShutter(mode: VolumeKeyShutter) {
        viewModelScope.launch { settingsRepo.setVolumeKeyShutter(mode) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as CamApp).container
                SettingsViewModel(c.settingsRepo, c.observeFormat)
            }
        }
    }
}
