package com.tomatorangers.detection.domain.model

import android.graphics.Color

/**
 * Single source of truth for per-class box colors (ARGB ints).
 * Index order must match assets/labels.txt.
 */
object ClassColors {
    private val palette = intArrayOf(
        Color.RED, // DAMAGED
        Color.GREEN, // RIPE
        Color.YELLOW, // UNRIPE
    )

    /** Text drawn on top of a class-colored label background. */
    const val LABEL_TEXT = Color.BLACK

    fun forClass(id: Int) = palette[id.mod(palette.size)]
}
