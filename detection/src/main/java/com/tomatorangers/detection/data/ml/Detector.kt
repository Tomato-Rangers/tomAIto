package com.tomatorangers.detection.data.ml

import android.content.Context
import android.graphics.Bitmap
import com.tomatorangers.detection.domain.model.Detection
import org.tensorflow.lite.DataType
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Detector(
    private val model: LoadedModel,
    private val labels: List<String>,
    private val iouThreshold: Float = .45f,
) : Closeable {


    private val preprocessor = Preprocessor(model)
    private val postProcessor = PostProcessor(model, labels, iouThreshold)
    private val isFloat = model.outputType == DataType.FLOAT32
    private val output = ByteBuffer
        .allocateDirect(model.outputShape.fold(1, Int::times) * if (isFloat) 4 else 1)
        .order(ByteOrder.nativeOrder())

    /**
     * Runs the full pipeline (preprocess, inference, postprocess) on one image.
     * Boxes are in the coordinate space of [bitmap] after rotating it by
     * [rotationDegrees]. Synchronized because the preprocessor and output
     * buffer are reused between calls.
     */
    @Synchronized
    fun detect(
        bitmap: Bitmap,
        rotationDegrees: Int,
        confidenceThreshold: Float,
    ): List<Detection> {
        val pre = preprocessor.process(bitmap, rotationDegrees)

        output.rewind()
        model.interpreter.run(pre.buffer, output)
        output.rewind()

        val swapped = rotationDegrees % 180 != 0
        val srcWidth = (if (swapped) bitmap.height else bitmap.width).toFloat()
        val srcHeight = (if (swapped) bitmap.width else bitmap.height).toFloat()

        return postProcessor.process(
            output,
            pre.letterBox,
            srcWidth,
            srcHeight,
            confidenceThreshold,
        )
    }

    override fun close() = model.close()

    companion object {
        fun create(context: Context): Detector {
            val labels = context.assets.open("labels.txt").bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
            return Detector(ModelLoader(context).load(), labels)
        }
    }
}
