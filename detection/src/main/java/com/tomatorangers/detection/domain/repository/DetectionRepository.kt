package com.tomatorangers.detection.domain.repository

import android.graphics.Bitmap
import com.tomatorangers.detection.domain.model.Detection

interface DetectionRepository {
    fun setConfidenceThreshold(value: Float)
    suspend fun detect(bitmap: Bitmap, rotationDegrees: Int = 0): List<Detection>
}
