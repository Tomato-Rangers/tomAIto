package com.tomatorangers.detection

import com.tomatorangers.detection.data.ml.LetterBox
import com.tomatorangers.detection.data.ml.LoadedModel
import com.tomatorangers.detection.data.ml.PostProcessor
import com.tomatorangers.detection.data.ml.Quantization
import com.tomatorangers.detection.data.ml.TensorLayout
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PostProcessorTest {

    private val labels = listOf("DAMAGED", "RIPE", "UNRIPE")
    private val inputSize = 640
    private val numCandidates = 8 // must exceed 4 + classes so channelsFirst is true

    private fun model(
        outputType: DataType = DataType.FLOAT32,
        quant: Quantization? = null,
        channelsFirst: Boolean = true,
    ) = LoadedModel(
        interpreter = mockk<Interpreter>(relaxed = true),
        inputShape = intArrayOf(1, inputSize, inputSize, 3),
        inputType = DataType.FLOAT32,
        inputLayout = TensorLayout.NHCW,
        inputQuantization = null,
        outputShape = if (channelsFirst) intArrayOf(1, 7, numCandidates)
        else intArrayOf(1, numCandidates, 7),
        outputType = outputType,
        outputQuantization = quant,
        outputChannelsFirst = channelsFirst,
    )

    /** Each candidate: [cx, cy, w, h, s0, s1, s2] (normalized). Unused slots stay zero. */
    private fun floatOutput(
        m: LoadedModel,
        vararg candidates: FloatArray,
    ): ByteBuffer {
        val buf = ByteBuffer.allocateDirect(7 * numCandidates * 4).order(ByteOrder.nativeOrder())
        candidates.forEachIndexed { i, c ->
            c.forEachIndexed { attr, v ->
                val idx = if (m.outputChannelsFirst) attr * numCandidates + i else i * 7 + attr
                buf.putFloat(idx * 4, v)
            }
        }
        return buf
    }

    /** Mirrors Preprocessor.drawLetterboxed math. */
    private fun letterBox(srcW: Float, srcH: Float): LetterBox {
        val scale = minOf(inputSize / srcW, inputSize / srcH)
        return LetterBox(scale, (inputSize - srcW * scale) / 2f, (inputSize - srcH * scale) / 2f)
    }

    /** Forward-maps a source-space box to a normalized candidate row. */
    private fun candidate(
        lb: LetterBox, l: Float, t: Float, r: Float, b: Float,
        scores: FloatArray,
    ): FloatArray {
        val nx1 = (l * lb.scale + lb.padX) / inputSize
        val nx2 = (r * lb.scale + lb.padX) / inputSize
        val ny1 = (t * lb.scale + lb.padY) / inputSize
        val ny2 = (b * lb.scale + lb.padY) / inputSize
        return floatArrayOf((nx1 + nx2) / 2, (ny1 + ny2) / 2, nx2 - nx1, ny2 - ny1) + scores
    }

    private fun processor(m: LoadedModel, conf: Float = 0.25f, iou: Float = 0.45f) =
        PostProcessor(m, labels, conf, iou)

    // ---------------- round trip ----------------

    private fun assertRoundTrip(srcW: Float, srcH: Float) {
        val m = model()
        val lb = letterBox(srcW, srcH)
        val (l, t, r, b) = listOf(srcW * 0.25f, srcH * 0.30f, srcW * 0.60f, srcH * 0.80f)
        val out = floatOutput(m, candidate(lb, l, t, r, b, floatArrayOf(0f, 0.9f, 0f)))

        val result = processor(m).process(out, lb, srcW, srcH)

        assertEquals(1, result.size)
        val box = result[0].box
        assertEquals(l, box.left, 0.5f)
        assertEquals(t, box.top, 0.5f)
        assertEquals(r, box.right, 0.5f)
        assertEquals(b, box.bottom, 0.5f)
        assertEquals("RIPE", result[0].label)
    }

    @Test
    fun roundTrip_landscape() = assertRoundTrip(1920f, 1080f)  // pads top/bottom

    @Test
    fun roundTrip_portrait() = assertRoundTrip(1080f, 1920f)   // pads left/right

    @Test
    fun roundTrip_square() = assertRoundTrip(640f, 640f)       // no padding

    @Test
    fun roundTrip_upscale() = assertRoundTrip(320f, 240f)      // scale > 1

    @Test
    fun roundTrip_rotated90_usesPostRotationDims() {
        // Sensor frame 1920x1080 rotated 90 -> detector reports in 1080x1920 space.
        assertRoundTrip(1080f, 1920f)
    }

    @Test
    fun roundTrip_channelsLast_layout() {
        val m = model(channelsFirst = false)
        val lb = letterBox(1920f, 1080f)
        val out = floatOutput(m, candidate(lb, 480f, 270f, 960f, 540f, floatArrayOf(0.8f, 0f, 0f)))
        val box = processor(m).process(out, lb, 1920f, 1080f).single().box
        assertEquals(480f, box.left, 0.5f)
        assertEquals(540f, box.bottom, 0.5f)
    }

    // ---------------- decode ----------------

    @Test
    fun boxesOutsideImage_areClampedToBounds() {
        val m = model()
        val lb = letterBox(1920f, 1080f)
        // spans the whole canvas including the gray padding
        val out = floatOutput(m, floatArrayOf(0.5f, 0.5f, 1f, 1f, 0.9f, 0f, 0f))
        val box = processor(m).process(out, lb, 1920f, 1080f).single().box
        assertEquals(0f, box.left, 0f)
        assertEquals(0f, box.top, 0f)
        assertEquals(1920f, box.right, 0f)
        assertEquals(1080f, box.bottom, 0f)
    }

    @Test
    fun belowConfidenceThreshold_isDropped() {
        val m = model()
        val lb = letterBox(640f, 640f)
        val out = floatOutput(m, floatArrayOf(0.5f, 0.5f, 0.2f, 0.2f, 0.1f, 0.24f, 0.2f))
        assertEquals(0, processor(m, conf = 0.25f).process(out, lb, 640f, 640f).size)
    }

    @Test
    fun picksHighestScoringClass() {
        val m = model()
        val lb = letterBox(640f, 640f)
        val out = floatOutput(m, floatArrayOf(0.5f, 0.5f, 0.2f, 0.2f, 0.3f, 0.4f, 0.7f))
        val d = processor(m).process(out, lb, 640f, 640f).single()
        assertEquals(2, d.classId)
        assertEquals("UNRIPE", d.label)
        assertEquals(0.7f, d.confidence, 1e-6f)
    }

    @Test
    fun quantizedUint8_isDequantized() {
        val m = model(
            outputType = DataType.UINT8, quant = Quantization(
                scale = 1f / 255f,
                zeroPoint = 0
            )
        )
        val lb = letterBox(640f, 640f)
        val buf = ByteBuffer.allocateDirect(7 * numCandidates).order(ByteOrder.nativeOrder())
        // candidate 0: cx=.5 cy=.5 w=.25 h=.25, class 1 score ~ 0.8
        val raw = intArrayOf(128, 128, 64, 64, 0, 204, 0)
        raw.forEachIndexed { attr, v -> buf.put(attr * numCandidates, v.toByte()) }

        val d = processor(m).process(buf, lb, 640f, 640f).single()
        assertEquals("RIPE", d.label)
        assertEquals(0.8f, d.confidence, 0.01f)
        assertEquals(128f / 255f * 640f, (d.box.left + d.box.right) / 2, 0.01f)
    }

    // ---------------- NMS / IoU ----------------

    private fun twoBoxes(
        a: FloatArray, b: FloatArray,
    ) = processor(model()).process(floatOutput(model(), a, b), letterBox(640f, 640f), 640f, 640f)

    @Test
    fun nms_suppressesOverlappingSameClass_keepsHigherConfidence() {
        val r = twoBoxes(
            floatArrayOf(0.5f, 0.5f, 0.4f, 0.4f, 0f, 0.6f, 0f),
            floatArrayOf(0.51f, 0.5f, 0.4f, 0.4f, 0f, 0.9f, 0f),
        )
        assertEquals(1, r.size)
        assertEquals(0.9f, r[0].confidence, 1e-6f)
    }

    @Test
    fun nms_keepsOverlappingBoxesOfDifferentClasses() {
        val r = twoBoxes(
            floatArrayOf(0.5f, 0.5f, 0.4f, 0.4f, 0f, 0.6f, 0f),
            floatArrayOf(0.5f, 0.5f, 0.4f, 0.4f, 0f, 0f, 0.9f),
        )
        assertEquals(2, r.size)
    }

    @Test
    fun nms_keepsDisjointBoxesOfSameClass() {
        val r = twoBoxes(
            floatArrayOf(0.2f, 0.2f, 0.2f, 0.2f, 0f, 0.6f, 0f),
            floatArrayOf(0.8f, 0.8f, 0.2f, 0.2f, 0f, 0.9f, 0f),
        )
        assertEquals(2, r.size)
    }

    @Test
    fun nms_keepsEdgeTouchingBoxes_zeroIntersection() {
        // box A spans x 0.1..0.3, box B spans x 0.3..0.5 -> intersection width 0
        val r = twoBoxes(
            floatArrayOf(0.2f, 0.5f, 0.2f, 0.2f, 0f, 0.6f, 0f),
            floatArrayOf(0.4f, 0.5f, 0.2f, 0.2f, 0f, 0.9f, 0f),
        )
        assertEquals(2, r.size)
    }

    @Test
    fun nms_iouAtThreshold_isKept_strictlyGreaterSuppresses() {
        // Same height, widths 0.2; B shifted so IoU = 1/3 (< 0.45) -> kept.
        val kept = twoBoxes(
            floatArrayOf(0.5f, 0.5f, 0.2f, 0.2f, 0f, 0.6f, 0f),
            floatArrayOf(0.6f, 0.5f, 0.2f, 0.2f, 0f, 0.9f, 0f),
        )
        assertEquals(2, kept.size)
        // Shift 0.04 -> IoU ~0.667 (> 0.45) -> suppressed.
        val suppressed = twoBoxes(
            floatArrayOf(0.5f, 0.5f, 0.2f, 0.2f, 0f, 0.6f, 0f),
            floatArrayOf(0.54f, 0.5f, 0.2f, 0.2f, 0f, 0.9f, 0f),
        )
        assertEquals(1, suppressed.size)
    }

    // ---------------- init ----------------

    @Test
    fun init_rejectsLabelCountMismatch() {
        assertThrows(IllegalArgumentException::class.java) {
            PostProcessor(model(), listOf("A", "B"))
        }
    }
}
