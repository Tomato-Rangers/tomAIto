package com.tomatorangers.detection.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomatorangers.detection.domain.model.DetectionResult
import kotlin.math.roundToInt

private val ClassColors = listOf(
    Color.Red, // DAMAGED
    Color.Green, // RIPE
    Color.Yellow, // UNRIPE
)

@Composable
fun DetectionOverlay(
    result: DetectionResult?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    if (result == null || result.imageWidth <= 0 || result.imageHeight <= 0) return

    val measurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val src = Size(result.imageWidth.toFloat(), result.imageHeight.toFloat())
        val scale = contentScale.computeScaleFactor(src, size).scaleX
        val offset = Offset(
            (size.width - src.width * scale) / 2f,
            (size.height - src.height * scale) / 2f,
        )

        result.detections.forEach { d ->
            val color = ClassColors[d.classId % ClassColors.size]
            val topLeft = Offset(d.box.left * scale, d.box.top * scale) + offset

            drawRect(
                color = color,
                topLeft = topLeft,
                size = Size(d.box.width * scale, d.box.height * scale),
                style = Stroke(3.dp.toPx()),
            )
            drawText(
                textMeasurer = measurer,
                text = "${d.label} ${(d.confidence * 100).roundToInt()}%",
                topLeft = topLeft,
                style = TextStyle(
                    color = Color.Black,
                    background = color,
                    fontSize = 12.sp,
                ),
            )
        }
    }

}
