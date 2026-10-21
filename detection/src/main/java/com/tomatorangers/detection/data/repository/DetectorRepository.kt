package com.tomatorangers.detection.data.repository

import android.graphics.Bitmap
import com.tomatorangers.detection.data.ml.Detector
import com.tomatorangers.detection.domain.model.Detection
import com.tomatorangers.detection.domain.model.DetectionDefaults
import com.tomatorangers.detection.domain.repository.DetectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DetectorRepository(
    private val detector: Lazy<Detector>,
) : DetectionRepository {
    private var confidenceThreshold = DetectionDefaults.CONFIDENCE_THRESHOLD

    override fun setConfidenceThreshold(value: Float) {
        confidenceThreshold = value
    }

    override suspend fun detect(bitmap: Bitmap, rotationDegrees: Int): List<Detection> =
        withContext(Dispatchers.Default) {
            detector.value.detect(
                bitmap,
                rotationDegrees,
                confidenceThreshold,
            )
        }
}
