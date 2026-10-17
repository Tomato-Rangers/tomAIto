package com.elensiel.camer_.di

import android.content.Context
import com.elensiel.camer_.data.DataStoreSettingsRepository
import com.elensiel.camer_.data.LensCacheRepository
import com.elensiel.camer_.data.MediaStoreRepository
import com.elensiel.camer_.data.camera.CameraXRepository
import com.elensiel.camer_.domain.usecase.CapturePhotoUseCase
import com.elensiel.camer_.domain.usecase.DiscardPhotoUseCase
import com.elensiel.camer_.domain.usecase.ObserveBindConfigUseCase
import com.elensiel.camer_.domain.usecase.ObserveFormatOutputUseCase
import com.elensiel.camer_.domain.usecase.SavePhotoUseCase

class AppContainer(context: Context) {
    val settingsRepo = DataStoreSettingsRepository(context)
    val cameraRepo = CameraXRepository(context)
    val mediaRepo = MediaStoreRepository(context)
    val lensRepo = LensCacheRepository()

    val capturePhoto = CapturePhotoUseCase(cameraRepo, settingsRepo, mediaRepo)
    val savePhoto = SavePhotoUseCase(mediaRepo, settingsRepo)
    val discardPhoto = DiscardPhotoUseCase(mediaRepo)
    val observeFormat = ObserveFormatOutputUseCase(settingsRepo, cameraRepo, lensRepo)
    val observeBindConfig = ObserveBindConfigUseCase(settingsRepo, lensRepo, observeFormat)
}
