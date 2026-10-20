package com.tomatorangers.detection.data.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import java.io.FileInputStream
import java.nio.channels.FileChannel

class ModelLoader(private val context: Context) {
    fun load(
        assetName: String = "model.tflite",
        numThreads: Int = 4,
    ): LoadedModel {
        val buffer = context.assets.openFd(assetName).use {
            FileInputStream(it.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY, it.startOffset, it.declaredLength,
            )
        }
        val interpreter = Interpreter(buffer, Interpreter.Options().setNumThreads(numThreads))

        val input = interpreter.getInputTensor(0)
        val output = interpreter.getOutputTensor(0)
        val outputShape = output.shape()
        require(outputShape.size == 3) { "Unsupported output shape: ${outputShape.contentToString()}" }

        return LoadedModel(
            interpreter = interpreter,
            inputShape = input.shape(),
            inputType = input.dataType(),
            inputLayout = detectLayout(input.shape()),
            inputQuantization = input.quantization(),
            outputShape = outputShape,
            outputType = output.dataType(),
            outputQuantization = output.quantization(),
            outputChannelsFirst = outputShape[1] < outputShape[2],
        )
    }

    private fun detectLayout(shape: IntArray): TensorLayout {
        require(shape.size == 4) { "Unsupported input shape: ${shape.contentToString()}" }
        val isChannels = { n: Int -> n in 1..4 }
        val layout =
            if (isChannels(shape[1]) && !isChannels(shape[3])) TensorLayout.NCHW
            else TensorLayout.NHCW
        return layout
    }

    private fun Tensor.quantization(): Quantization? =
        quantizationParams().takeIf { it.scale != 0f }?.let { Quantization(it.scale, it.zeroPoint) }
}
