package com.elensiel.camer_.domain.model

import androidx.annotation.DrawableRes
import com.elensiel.camer_.R

enum class FlashMode(
    @DrawableRes val icon: Int,
) {
    OFF(R.drawable.flash_off),
    ON(R.drawable.flash_on),
    AUTO(R.drawable.flash_auto);

    fun next(): FlashMode {
        return entries[(ordinal + 1) % entries.size]
    }
}
