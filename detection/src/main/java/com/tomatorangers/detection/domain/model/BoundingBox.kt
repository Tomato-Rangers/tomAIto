package com.tomatorangers.detection.domain.model

/**
 * Axis-aligned rectangle in image pixel coordinates, origin at the top-left.
 * Expected to satisfy `left <= right` and `top <= bottom`.
 *
 * @property left X of the left edge.
 * @property top Y of the top edge.
 * @property right X of the right edge.
 * @property bottom Y of the bottom edge.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width get() = right - left
    val height get() = bottom - top
}
