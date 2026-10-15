package com.elensiel.camer_.data

import com.elensiel.camer_.domain.model.LensFacing
import com.elensiel.camer_.domain.repository.LensRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LensCacheRepository : LensRepository {
    private val _lensFacing = MutableStateFlow(LensFacing.BACK)
    override val lensFacing = _lensFacing.asStateFlow()
    override fun flip() = _lensFacing.update { it.flip() }
}
