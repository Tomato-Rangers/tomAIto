package com.elensiel.camer_.domain.usecase

import android.net.Uri
import com.elensiel.camer_.domain.model.CapturedPhoto
import com.elensiel.camer_.domain.repository.MediaRepository
import com.elensiel.camer_.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class SavePhotoUseCase(
    private val mediaRepo: MediaRepository,
    private val settingsRepo: SettingsRepository,
) {
    suspend operator fun invoke(photo: CapturedPhoto): Result<Uri> = runCatching {
        mediaRepo.save(
            photo.file,
            photo.format,
            settingsRepo.settings.first().saveLocation
        )
    }
}
