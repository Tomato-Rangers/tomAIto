package com.tomatorangers.detection.domain.model

import android.graphics.Bitmap


/**
 * A single camera frame queued for detection.
 *
 * @property bitmap The frame as delivered by the camera, not yet rotated.
 * @property rotationDegrees Clockwise rotation (0, 90, 180, 270) needed to make
 *   [bitmap] upright. The detector applies it, so detections come back in the
 *   rotated coordinate space.
 */
data class CameraFrame(
    val bitmap: Bitmap,
    val rotationDegrees: Int,
)
