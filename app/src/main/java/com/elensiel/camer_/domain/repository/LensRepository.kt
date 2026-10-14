package com.elensiel.camer_.domain.repository

import com.elensiel.camer_.domain.model.LensFacing
import kotlinx.coroutines.flow.StateFlow

interface LensRepository {
    val lensFacing: StateFlow<LensFacing>
    fun flip()
}
