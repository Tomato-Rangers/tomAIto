package com.elensiel.camer_.domain.model

sealed interface CaptureResult {
    data class Saved(val location: String) : CaptureResult
    data class Pending(val photo: CapturedPhoto) : CaptureResult
    data class Failed(val cause: Throwable) : CaptureResult
}
