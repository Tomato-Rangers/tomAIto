package com.elensiel.camer_.domain.repository

import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.domain.model.FlashMode
import com.elensiel.camer_.domain.model.ImageFormat
import com.elensiel.camer_.domain.model.Settings
import com.elensiel.camer_.domain.model.VolumeKeyShutter
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun setAlwaysSaveEnabled(enabled: Boolean)
    suspend fun setAspectRatio(ratio: CaptureAspectRatio)
    suspend fun setFlashMode(mode: FlashMode)
    suspend fun setImageFormat(format: ImageFormat)
    suspend fun setLiveDetectionEnabled(enabled: Boolean)
    suspend fun setSaveLocation(location: String)
    suspend fun setShutterSoundEnabled(enabled: Boolean)
    suspend fun setVolumeKeyShutter(mode: VolumeKeyShutter)
}
