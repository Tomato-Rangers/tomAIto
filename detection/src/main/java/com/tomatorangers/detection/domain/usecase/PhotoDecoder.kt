package com.tomatorangers.detection.domain.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.os.Build
import java.io.File
import java.io.IOException

internal fun decodePhoto(file: File): Bitmap =
    if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        decodeLegacy(file)
    }

// API 26-27: BitmapFactory + manual EXIF orientation
private fun decodeLegacy(src: File): Bitmap {
    val decoded = BitmapFactory.decodeFile(src.absolutePath)
        ?: throw IOException("Failed to decode ${src.name}")

    val matrix = Matrix()
    when (ExifInterface(src.absolutePath).getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
    )) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(1f, -1f)
        }
    }

    if (matrix.isIdentity) return decoded

    val rotated =
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    decoded.recycle()
    return rotated
}
