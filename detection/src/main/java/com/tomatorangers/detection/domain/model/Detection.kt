package com.tomatorangers.detection.domain.model

data class Detection(
    val classId: Int,
    val label: String,
    val confidence: Float,
    val box: BoundingBox,
)
