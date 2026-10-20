package com.elensiel.camer_.domain.usecase

import android.util.Log
import com.elensiel.camer_.domain.model.CaptureResult
import com.elensiel.camer_.domain.repository.CameraRepository
import com.elensiel.camer_.domain.repository.MediaRepository
import com.elensiel.camer_.domain.repository.SettingsRepository
import com.tomatorangers.detection.domain.usecase.AnnotatePhotoUseCase
import kotlinx.coroutines.flow.first

class CapturePhotoUseCase(
    private val cameraRepo: CameraRepository,
    private val settingsRepo: SettingsRepository,
    private val mediaRepo: MediaRepository,
    private val annotatePhoto: AnnotatePhotoUseCase,
) {
    /**
     * Takes a photo, then either saves it right away (Always Save on) or
     * returns it as [CaptureResult.Pending] for the user to review.
     */
    suspend operator fun invoke(): CaptureResult {
        val settings = settingsRepo.settings.first()

        val photo = runCatching { cameraRepo.takePhoto() }
            .getOrElse { return CaptureResult.Failed(it) }

        annotatePhoto(photo.file)
            .onFailure { Log.e("Capture", "Annotate failed.", it) }

        if (!settings.alwaysSaveEnabled) return CaptureResult.Pending(photo)

        return runCatching { mediaRepo.save(photo.file, photo.format, settings.saveLocation) }
            .fold(
                onSuccess = { CaptureResult.Saved(it) },
                onFailure = {
                    mediaRepo.discard(photo.file)
                    CaptureResult.Failed(it)
                },
            )
    }
}
