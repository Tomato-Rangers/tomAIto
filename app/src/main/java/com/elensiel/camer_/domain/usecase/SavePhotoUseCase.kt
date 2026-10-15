package com.elensiel.camer_.domain.usecase

import com.elensiel.camer_.domain.model.CapturedPhoto
import com.elensiel.camer_.domain.repository.MediaRepository
import com.elensiel.camer_.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class SavePhotoUseCase(
    private val mediaRepo: MediaRepository,
    private val settingsRepo: SettingsRepository,
) {
    /** Saves a photo that was held for review, using the current save location. */
    suspend operator fun invoke(photo: CapturedPhoto): Result<String> = runCatching {
        mediaRepo.save(
            photo.file,
            photo.format,
            settingsRepo.settings.first().saveLocation
        )
    }
}
