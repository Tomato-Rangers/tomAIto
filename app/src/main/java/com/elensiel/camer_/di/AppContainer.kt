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
import com.tomatorangers.detection.data.ml.Detector
import com.tomatorangers.detection.data.repository.DetectorRepository
import com.tomatorangers.detection.domain.usecase.AnnotatePhotoUseCase
import com.tomatorangers.detection.domain.usecase.ObserveLiveDetectionUseCase

class AppContainer(context: Context) {
    val settingsRepo = DataStoreSettingsRepository(context)
    val cameraRepo = CameraXRepository(context)
    val mediaRepo = MediaStoreRepository(context)
    val lensRepo = LensCacheRepository()

    val detector = lazy { Detector.create(context) }
    val detectionRepo = DetectorRepository(detector)

    val annotatePhoto = AnnotatePhotoUseCase(detectionRepo)
    val capturePhoto = CapturePhotoUseCase(cameraRepo, settingsRepo, mediaRepo, annotatePhoto)
    val savePhoto = SavePhotoUseCase(mediaRepo, settingsRepo)
    val discardPhoto = DiscardPhotoUseCase(mediaRepo)
    val observeFormat = ObserveFormatOutputUseCase(settingsRepo, cameraRepo, lensRepo)
    val observeBindConfig = ObserveBindConfigUseCase(settingsRepo, lensRepo, observeFormat)
    val observeLiveDetection = ObserveLiveDetectionUseCase(detectionRepo)
}
