package com.elensiel.camer_.domain.model

enum class FlashMode {
    OFF, ON, AUTO;

    fun next() = entries[(ordinal + 1) % entries.size]
}
