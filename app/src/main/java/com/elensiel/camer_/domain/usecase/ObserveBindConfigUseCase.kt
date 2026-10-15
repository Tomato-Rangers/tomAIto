package com.elensiel.camer_.domain.usecase

import com.elensiel.camer_.domain.model.BindConfig
import com.elensiel.camer_.domain.repository.LensRepository
import com.elensiel.camer_.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveBindConfigUseCase(
    private val settingsRepo: SettingsRepository,
    private val lensRepo: LensRepository,
    private val observeFormat: ObserveFormatOutputUseCase,
) {
    operator fun invoke(): Flow<BindConfig> = combine(
        settingsRepo.settings.map { it.aspectRatio }.distinctUntilChanged(),
        lensRepo.lensFacing,
        observeFormat(),
    ) { ratio, lens, format ->
        BindConfig(lens, ratio, format.selected)
    }
        // Every emission causes a full camera unbind/rebind, so only emit real changes.
        .distinctUntilChanged()
}
