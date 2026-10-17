package com.tomatorangers.detection.data.ml

import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable

enum class TensorLayout { NHCW, NCHW }

data class Quantization(val scale: Float, val zeroPoint: Int)

class LoadedModel(
    val interpreter: Interpreter,
    val inputShape: IntArray,
    val inputType: DataType,
    val inputLayout: TensorLayout,
    val inputQuantization: Quantization?,
    val outputShape: IntArray,
    val outputType: DataType,
    val outputQuantization: Quantization?,
    val outputChannelsFirst: Boolean,
) : Closeable {
    val nhwc = inputLayout == TensorLayout.NHCW

    val inputWidth get() = if (nhwc) inputShape[1] else inputShape[2]
    val inputHeight get() = if (nhwc) inputShape[2] else inputShape[3]
    val inputChannels get() = if (nhwc) inputShape[3] else inputShape[1]

    val numAttributes get() = if (outputChannelsFirst) outputShape[1] else outputShape[2]
    val numCandidates get() = if (outputChannelsFirst) outputShape[2] else outputShape[1]

    // Assumes the YOLOv8 head: 4 box values, no objectness score
    val numClasses get() = numAttributes - 4

    override fun close() = interpreter.close()
}
