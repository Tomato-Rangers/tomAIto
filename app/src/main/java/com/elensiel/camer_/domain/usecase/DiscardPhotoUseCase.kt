package com.elensiel.camer_.domain.usecase

import com.elensiel.camer_.domain.model.CapturedPhoto
import com.elensiel.camer_.domain.repository.MediaRepository

class DiscardPhotoUseCase(private val mediaRepo: MediaRepository) {
    suspend operator fun invoke(photo: CapturedPhoto) = mediaRepo.discard(photo.file)
}
