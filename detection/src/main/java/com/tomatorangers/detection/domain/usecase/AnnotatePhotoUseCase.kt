package com.tomatorangers.detection.domain.usecase

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.tomatorangers.detection.domain.model.Detection
import com.tomatorangers.detection.domain.repository.DetectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private val ClassColors = intArrayOf(
    Color.RED, // UNRIPE
    Color.GREEN, // RIPE
    Color.YELLOW, // UNRIPE
)

class AnnotatePhotoUseCase(
    private val detectionRepo: DetectionRepository,
) {
    /** Detects on [file], draws boxes, and overwrites [file] as JPEG. Returns detection count. */
    suspend operator fun invoke(file: File): Result<Int> = withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = decodePhoto(file).toMutableCopy()
            try {
                // EXIF orientation is already applied by decodePhoto, so rotation = 0.
                val detections = detectionRepo.detect(bitmap, 0)
                if (detections.isNotEmpty()) {
                    draw(bitmap, detections)
                    bitmap.saveAsJpeg(file)
                }
                detections.size
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun Bitmap.toMutableCopy(): Bitmap =
        copy(Bitmap.Config.ARGB_8888, true).also { recycle() }

    /** Writes to a temp file first so a failed encode never corrupts the original. */
    private fun Bitmap.saveAsJpeg(file: File) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.outputStream().use {
            check(compress(Bitmap.CompressFormat.JPEG, 100, it)) { "Bitmap compression failed" }
        }
        check(tmp.renameTo(file)) { "Failed to replace ${file.name}" }
    }

    private fun draw(bitmap: Bitmap, detections: List<Detection>) {
        val canvas = Canvas(bitmap)
        val unit = maxOf(bitmap.width, bitmap.height) / 640f // scales sizes with resolution
        val pad = 3f * unit

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * unit
        }
        val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f * unit
        }
        val metrics = labelTextPaint.fontMetrics
        val labelHeight = metrics.descent - metrics.ascent + pad * 2

        detections.forEach { d ->
            val color = ClassColors[d.classId % ClassColors.size]
            boxPaint.color = color
            labelBgPaint.color = color

            canvas.drawRect(
                d.box.left, d.box.top,
                d.box.right, d.box.bottom,
                boxPaint,
            )

            val label = "${d.label} ${(d.confidence * 100).roundToInt()}%"
            val labelWidth = labelTextPaint.measureText(label) + pad * 2
            val top = (d.box.top - labelHeight).coerceAtLeast(0f)
            val labelRect = RectF(d.box.left, top, d.box.left + labelWidth, top + labelHeight)

            canvas.drawRect(labelRect, labelBgPaint)
            canvas.drawText(
                label,
                labelRect.left + pad,
                labelRect.top + pad - metrics.ascent,
                labelTextPaint
            )
        }
    }
}
