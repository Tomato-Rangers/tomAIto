package com.elensiel.camer_.domain.model

import android.net.Uri

sealed interface CaptureResult {
    data class Saved(val uri: Uri) : CaptureResult
    data class Pending(val photo: CapturedPhoto) : CaptureResult
    data class Failed(val cause: Throwable) : CaptureResult
}
