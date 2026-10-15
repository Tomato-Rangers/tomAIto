package com.elensiel.camer_.domain.repository

import com.elensiel.camer_.domain.model.ImageFormat
import java.io.File

interface MediaRepository {
    suspend fun save(photoFile: File, format: ImageFormat, saveDirectory: String): String
    suspend fun discard(photoFile: File)
}
