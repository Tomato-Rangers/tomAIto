package com.tomatorangers.detection.domain.usecase

import com.tomatorangers.detection.domain.model.CameraFrame
import com.tomatorangers.detection.domain.model.DetectionResult
import com.tomatorangers.detection.domain.repository.DetectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

class ObserveLiveDetectionUseCase(private val detectionRepo: DetectionRepository) {
    operator fun invoke(frames: Flow<CameraFrame>): Flow<DetectionResult> =
        frames
            .conflate()
            .map { f ->
                val swapped = f.rotationDegrees % 180 != 0
                DetectionResult(
                    detections = detectionRepo.detect(f.bitmap, f.rotationDegrees),
                    imageWidth = if (swapped) f.bitmap.height else f.bitmap.width,
                    imageHeight = if (swapped) f.bitmap.width else f.bitmap.height,
                )
            }
}
