package com.tomatorangers.detection.data.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.graphics.createBitmap
import org.tensorflow.lite.DataType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.roundToInt

class LetterBox(
    val scale: Float,
    val padX: Float,
    val padY: Float,
)

class PreprocessResult(
    val buffer: ByteBuffer,
    val letterBox: LetterBox,
)

class Preprocessor(private val model: LoadedModel) {
    private val channelsFirst = model.inputLayout == TensorLayout.NCHW
    private val height = model.inputShape[if (channelsFirst) 2 else 1]
    private val width = model.inputShape[if (channelsFirst) 3 else 2]
    private val isFloat = model.inputType == DataType.FLOAT32

    private val buffer = ByteBuffer
        .allocateDirect(width * height * 3 * if (isFloat) 4 else 1)
        .order(ByteOrder.nativeOrder())
    private val resized = createBitmap(width, height)
    private val canvas = Canvas(resized)
    private val pixels = IntArray(width * height)

    fun process(source: Bitmap, rotationDegrees: Int = 0): PreprocessResult {
        val letterBox = drawLetterboxed(source, rotationDegrees)
        resized.getPixels(pixels, 0, width, 0, 0, width, height)
        writePixels()
        return PreprocessResult(buffer, letterBox)
    }

    // Resize keeping aspect ratio, centered on a gray canvas (YOLO's "letterbox").
    private fun drawLetterboxed(source: Bitmap, rotationDegrees: Int): LetterBox {
        val rotated = rotationDegrees % 180 != 0
        val srcWidth = if (rotated) source.height else source.width
        val srcHeight = if (rotated) source.width else source.height
        val scale = min(width / srcWidth.toFloat(), height / srcHeight.toFloat())

        val matrix = Matrix().apply {
            postTranslate(-source.width / 2f, -source.height / 2f)
            postRotate(rotationDegrees.toFloat())
            postScale(scale, scale)
            postTranslate(width / 2f, height / 2f)
        }
        canvas.drawColor(Color.rgb(114, 114, 114))
        canvas.drawBitmap(source, matrix, null)

        return LetterBox(
            scale = scale,
            padX = (width - srcWidth * scale) / 2f,
            padY = (height - srcHeight * scale) / 2f,
        )
    }

    private fun writePixels() {
        val pixelCount = width * height
        for (i in 0 until pixelCount) {
            val pixel = pixels[i]
            val rgb = intArrayOf((pixel shr 16) and 0xFF, (pixel shr 8) and 0xFF, pixel and 0xFF)
            for (c in 0..2) {
                // NCHW stores all R, then all G, then all B. NHWC stores R,G,B per pixel.
                val index = if (channelsFirst) c * pixelCount + i else i * 3 + c
                write(index, rgb[c])
            }
        }
        buffer.rewind()
    }

    private fun write(index: Int, value: Int) {
        if (isFloat) {
            buffer.putFloat(index * 4, value / 255f)
        } else {
            val quant = model.inputQuantization
            val scale = quant?.scale ?: (1f / 255f)
            val zeroPoint = quant?.zeroPoint ?: 0
            val range = if (model.inputType == DataType.UINT8) 0..255 else -128..127
            val quantized = ((value / 255f / scale).roundToInt() + zeroPoint).coerceIn(range)
            buffer.put(index, quantized.toByte())
        }
    }
}
