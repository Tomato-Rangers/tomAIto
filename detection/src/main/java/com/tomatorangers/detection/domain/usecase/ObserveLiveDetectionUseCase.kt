package com.tomatorangers.detection.domain.usecase

import com.tomatorangers.detection.domain.model.CameraFrame
import com.tomatorangers.detection.domain.model.Detection
import com.tomatorangers.detection.domain.repository.DetectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

class ObserveLiveDetectionUseCase(private val detectionRepo: DetectionRepository) {
    operator fun invoke(frames: Flow<CameraFrame>): Flow<List<Detection>> =
        frames
            .conflate()
            .map { detectionRepo.detect(it.bitmap, it.rotationDegrees) }
}
