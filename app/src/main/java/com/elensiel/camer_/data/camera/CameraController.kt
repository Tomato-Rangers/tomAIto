package com.elensiel.camer_.data.camera

import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import com.elensiel.camer_.domain.model.FlashMode
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit

internal class CameraController(private val session: CameraSession) {
    var flashMode = FlashMode.OFF
        private set

    fun focusOn(x: Float, y: Float) {
        val res = session.surfaceRequest.value?.resolution ?: return

        // camera is null while unbound or rebinding; skip the whole request then.
        session.camera?.cameraControl?.let {
            val point = SurfaceOrientedMeteringPointFactory(
                res.width.toFloat(),
                res.height.toFloat(),
            ).createPoint(x, y)

            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            )
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()

            it.startFocusAndMetering(action)
        }
    }

    fun setFlashMode(mode: FlashMode) {
        flashMode = mode
        session.imageCapture?.flashMode = mode.toCameraX()
    }

    fun setTorch(enabled: Boolean) {
        if (!session.state.value.hasFlashUnit) return
        session.state.update { it.copy(torchEnabled = enabled) }
        session.camera?.cameraControl?.enableTorch(enabled)
    }

    fun setZoom(ratio: Float) {
        // camera is null while unbound or rebinding.
        session.camera?.cameraControl?.let {
            val s = session.state.value
            it.setZoomRatio(ratio.coerceIn(s.minZoomRatio, s.maxZoomRatio))
        }
    }

    fun zoomBy(factor: Float) = setZoom(session.state.value.zoomRatio * factor)

}
