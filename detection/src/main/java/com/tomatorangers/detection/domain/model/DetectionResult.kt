package com.tomatorangers.detection.domain.model

data class DetectionResult(
    val detections: List<Detection>,
    val imageWidth: Int,
    val imageHeight: Int,
) {
    fun mirrored() = copy(
        detections = detections.map {
            it.copy(
                box = it.box.copy(
                    left = imageWidth - it.box.left,
                    right = imageWidth - it.box.right,
                )
            )
        }
    )
}
