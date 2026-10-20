package com.elensiel.camer_.data.camera

import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.domain.model.FlashMode
import com.elensiel.camer_.domain.model.ImageFormat

internal class BuiltUseCases(
    val useCaseGroup: UseCaseGroup,
    val imageCapture: ImageCapture,
    val imageFormat: ImageFormat,
)

internal class CameraUseCaseBuilder(
    private val session: CameraSession,
    private val analyzer: FrameAnalyzer,
) {
    fun build(
        aspectRatio: CaptureAspectRatio,
        format: ImageFormat,
        flashMode: FlashMode,
    ): BuiltUseCases {
        // Shared so Preview and ImageCapture use the same ratio.
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    aspectRatio.toCameraXRatio(),
                    AspectRatioStrategy.FALLBACK_RULE_AUTO,
                )
            )
            .build()

        // No PreviewView: the SurfaceRequest is collected by the Compose
        // CameraXViewfinder, which supplies the surface.
        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also { it.setSurfaceProvider { request -> session.surfaceRequest.value = request } }

        // Flash mode is set here so it survives a rebind. Formats that need
        // transcoding are captured as JPEG (see toCameraXFormat).
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(resolutionSelector)
            .setOutputFormat(format.toCameraX())
            .build()
            .also { it.flashMode = flashMode.toCameraX() }

        // Makes Preview and ImageCapture crop to the same region, so the saved
        // photo matches the preview. This is how 1:1 works: the sensor streams
        // 4:3 and the ViewPort crops to a square. Only applies to grouped use cases.
        val viewPort = ViewPort.Builder(aspectRatio.toViewPortRational(), preview.targetRotation)
            .setScaleType(ViewPort.FILL_CENTER)
            .build()

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            aspectRatio.toCameraXRatio(),
                            AspectRatioStrategy.FALLBACK_RULE_AUTO,
                        )
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { it.setAnalyzer(analyzer.executor, analyzer) }

        val group = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(preview)
            .addUseCase(capture)
            .addUseCase(analysis)
            .build()

        return BuiltUseCases(group, capture, format)
    }
}
