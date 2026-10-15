package com.elensiel.camer_.presentation.components

import androidx.annotation.DrawableRes
import com.elensiel.camer_.R
import com.elensiel.camer_.domain.model.FlashMode

@get:DrawableRes
val FlashMode.icon
    get() = when (this) {
        FlashMode.OFF -> R.drawable.flash_off
        FlashMode.ON -> R.drawable.flash_on
        FlashMode.AUTO -> R.drawable.flash_auto
    }
