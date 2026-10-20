package com.tomatorangers.detection.domain.model

/**
 * Output of a single object-detection inference pass.
 *
 * @property detections Objects found in the image. Empty if nothing was detected.
 * @property imageWidth Width in pixels of the image the detections were computed on.
 * @property imageHeight Height in pixels of the image the detections were computed on.
 */
data class DetectionResult(
    val detections: List<Detection>,
    val imageWidth: Int,
    val imageHeight: Int,
) {
    /** Flips boxes horizontally, e.g. for a mirrored front-camera preview. */
    fun mirrored() = copy(
        detections = detections.map {
            it.copy(
                box = it.box.copy(
                    left = imageWidth - it.box.right,
                    right = imageWidth - it.box.left,
                )
            )
        }
    )
}
