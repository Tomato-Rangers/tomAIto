package com.elensiel.camer_.data.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.tomatorangers.detection.domain.model.CameraFrame
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class FrameAnalyzer : ImageAnalysis.Analyzer {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()

    @Volatile
    var enabled = true

    private val _frames = MutableSharedFlow<CameraFrame>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val frames = _frames.asSharedFlow()

    override fun analyze(image: ImageProxy) {
        image.use {
            if (!enabled || _frames.subscriptionCount.value == 0) return

            val full = it.toBitmap()
            // ViewPort crop: toBitmap() ignores it, so apply manually.
            val rect = it.cropRect
            val bitmap =
                if (rect.width() == full.width && rect.height() == full.height) full
                else Bitmap.createBitmap(
                    full,
                    rect.left,
                    rect.top,
                    rect.width(),
                    rect.height(),
                )
            _frames.tryEmit(CameraFrame(bitmap, it.imageInfo.rotationDegrees))
        }
    }
}
