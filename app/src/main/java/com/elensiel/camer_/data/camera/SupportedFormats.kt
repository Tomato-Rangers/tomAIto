package com.elensiel.camer_.data.camera

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import com.elensiel.camer_.domain.model.ImageFormat
import com.elensiel.camer_.domain.model.LensFacing
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SupportedFormats(private val context: Context) {
    private val cache = mutableMapOf<LensFacing, Set<ImageFormat>>()
    private val lock = Mutex()

    suspend fun get(lensFacing: LensFacing): Set<ImageFormat> =
        lock.withLock {
            cache.getOrPut(lensFacing) {
                val provider = ProcessCameraProvider.awaitInstance(context)
                val selector = lensFacing.toCameraSelector()
                val native = ImageCapture
                    .getImageCaptureCapabilities(provider.getCameraInfo(selector))
                    .supportedOutputFormats
                    .mapNotNull { it.toImageFormat() }
                    .toSet()

                // Transcoded formats are always available: they're converted from JPEG.
                native + ImageFormat.entries.filter { it.needsTranscoding }
            }
        }
}
