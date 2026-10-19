package com.tomatorangers.detection.domain.model

/**
 * A single detected object.
 *
 * @property classId Index of the predicted class, matching the line order in `labels.txt`.
 * @property label Human-readable class name for [classId].
 * @property confidence Model confidence score in the range 0..1.
 * @property box Location of the object in the coordinate space of the analyzed image
 *   (after rotation), in pixels.
 */
data class Detection(
    val classId: Int,
    val label: String,
    val confidence: Float,
    val box: BoundingBox,
)
