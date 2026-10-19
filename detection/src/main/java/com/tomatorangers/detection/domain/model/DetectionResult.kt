package com.tomatorangers.detection.domain.model

data class DetectionResult(
    val detections: List<Detection>,
    val imageWidth: Int,
    val imageHeight: Int,
)
