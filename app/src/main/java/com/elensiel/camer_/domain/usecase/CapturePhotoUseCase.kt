package com.elensiel.camer_.domain.usecase

import com.elensiel.camer_.domain.model.CaptureResult
import com.elensiel.camer_.domain.repository.CameraRepository
import com.elensiel.camer_.domain.repository.MediaRepository
import com.elensiel.camer_.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class CapturePhotoUseCase(
    private val cameraRepo: CameraRepository,
    private val settingsRepo: SettingsRepository,
    private val mediaRepo: MediaRepository,
) {
    suspend operator fun invoke(): CaptureResult {
        val s = settingsRepo.settings.first()

        val photo = runCatching { cameraRepo.takePhoto() }
            .getOrElse { return CaptureResult.Failed(it) }

        if (!s.alwaysSaveEnabled) return CaptureResult.Pending(photo)

        return runCatching { mediaRepo.save(photo.file, photo.format, s.saveLocation) }
            .fold(
                onSuccess = { CaptureResult.Saved(it) },
                onFailure = { CaptureResult.Failed(it) },
            )
    }
}
