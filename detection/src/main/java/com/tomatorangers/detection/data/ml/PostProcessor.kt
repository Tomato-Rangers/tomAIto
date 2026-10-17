package com.tomatorangers.detection.data.ml

import com.tomatorangers.detection.domain.BoundingBox
import com.tomatorangers.detection.domain.Detection
import org.tensorflow.lite.DataType
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class PostProcessor(
    private val model: LoadedModel,
    private val labels: List<String>,
    private val confidenceThreshold: Float = 0.25f,
    private val iouThreshold: Float = .45f,
) {

    init {
        require(labels.size == model.numClasses) {
            "Model has ${model.numClasses} classes but ${labels.size} labels were given."
        }
    }

    /**
     * Converts the raw model output into final detections: decodes candidates
     * above the confidence threshold, then removes duplicates with NMS.
     * [srcWidth] x [srcHeight] are the source image dimensions after rotation,
     * and the returned boxes are in that coordinate space.
     */
    fun process(
        output: ByteBuffer,
        letterBox: LetterBox,
        srcWidth: Float,
        srcHeight: Float,
    ): List<Detection> = nms(decode(output, letterBox, srcWidth, srcHeight))

    /**
     * Turns every candidate in [output] into a [Detection]. Keeps only the best
     * class per candidate if it passes the confidence threshold, and maps its
     * box from letterboxed model space back to source-image space, clamped to
     * the image bounds.
     */
    private fun decode(
        output: ByteBuffer,
        letterBox: LetterBox,
        srcWidth: Float,
        srcHeight: Float,
    ): List<Detection> {
        val results = mutableListOf<Detection>()

        val toSrcX = { n: Float ->
            ((n * model.inputWidth - letterBox.padX) / letterBox.scale).coerceIn(0f, srcWidth)
        }
        val toSrcY = { n: Float ->
            ((n * model.inputHeight - letterBox.padY) / letterBox.scale).coerceIn(0f, srcHeight)
        }

        for (i in 0 until model.numCandidates) {
            var bestClass = 0
            var bestScore = 0f
            for (c in 0 until model.numClasses) {
                val score = value(output, 4 + c, i)
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }
            if (bestScore < confidenceThreshold) continue

            // YOLOv8 TFLite exports emit normalized (0..1) xywh.
            val cx = value(output, 0, i)
            val cy = value(output, 1, i)
            val w = value(output, 2, i)
            val h = value(output, 3, i)

            val box = BoundingBox(
                left = toSrcX(cx - w / 2f),
                top = toSrcY(cy - h / 2f),
                right = toSrcX(cx + w / 2f),
                bottom = toSrcY(cy + h / 2f),
            )

            results += Detection(bestClass, labels[bestClass], bestScore, box)
        }
        return results
    }

    /**
     * Reads one number from the raw output buffer. [attribute] is the row
     * (0-3 box, 4+ class scores) and [candidate] is the prediction index.
     * Handles both tensor layouts (attributes-first or candidates-first) and
     * both float and quantized outputs.
     */
    private fun value(output: ByteBuffer, attribute: Int, candidate: Int): Float {
        val index =
            if (model.outputChannelsFirst) attribute * model.numCandidates + candidate
            else candidate * model.numAttributes + attribute

        return when (model.outputType) {
            DataType.FLOAT32 -> output.getFloat(index * 4)
            DataType.UINT8 -> dequantize(output.get(index).toInt() and 0xFF)
            else -> dequantize(output.get(index).toInt())
        }
    }

    /**
     * Converts a quantized integer output to a float using the model's scale and
     * zero point. Returns the raw value unchanged if the model isn't quantized.
     */
    private fun dequantize(raw: Int): Float {
        val q = model.outputQuantization ?: return raw.toFloat()
        return (raw - q.zeroPoint) * q.scale
    }

    /**
     * Non-maximum suppression. Keeps the highest-confidence box and drops any
     * same-class box overlapping a kept one by more than [iouThreshold], which
     * removes duplicate detections of the same object.
     */
    private fun nms(detections: List<Detection>): List<Detection> {
        val kept = mutableListOf<Detection>()
        for (d in detections.sortedByDescending { it.confidence }) {
            if (kept.none { it.classId == d.classId && iou(it.box, d.box) > iouThreshold }) {
                kept += d
            }
        }
        return kept
    }

    /** Intersection area divided by union area of two boxes; 0 if they don't overlap. */
    private fun iou(a: BoundingBox, b: BoundingBox): Float {
        val w = min(a.right, b.right) - max(a.left, b.left)
        val h = min(a.bottom, b.bottom) - max(a.top, b.top)
        if (w <= 0f || h <= 0f) return 0f
        val inter = w * h
        return inter / (a.width * a.height + b.width * b.height - inter)
    }

}
