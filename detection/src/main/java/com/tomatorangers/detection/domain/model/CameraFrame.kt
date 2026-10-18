package com.tomatorangers.detection.domain.model

import android.graphics.Bitmap

data class CameraFrame(
    val bitmap: Bitmap,
    val rotationDegrees: Int,
)
