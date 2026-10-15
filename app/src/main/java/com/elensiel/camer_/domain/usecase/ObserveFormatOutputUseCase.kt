package com.elensiel.camer_.domain.usecase

import com.elensiel.camer_.domain.model.FormatOutput
import com.elensiel.camer_.domain.model.ImageFormat
import com.elensiel.camer_.domain.repository.CameraRepository
import com.elensiel.camer_.domain.repository.LensRepository
import com.elensiel.camer_.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveFormatOutputUseCase(
    private val settingsRepo: SettingsRepository,
    private val cameraRepo: CameraRepository,
    private val lensRepo: LensRepository,
) {
    /**
     * Formats the current lens can produce, plus the one to actually use.
     * Re-emits on lens flips because support differs per lens.
     */
    operator fun invoke(): Flow<FormatOutput> = combine(
        settingsRepo.settings.map { it.imageFormat }.distinctUntilChanged(),
        lensRepo.lensFacing,
    ) { requested, lens ->
        val supported = cameraRepo.getSupportedFormats(lens)

        FormatOutput(
            // Enum order keeps the settings dropdown order stable (the set has none).
            available = ImageFormat.entries.filter { it in supported },
            // The saved format may not exist on this lens (e.g. RAW on the front camera).
            selected = if (requested in supported) requested else ImageFormat.JPEG,
        )
    }.distinctUntilChanged()
}
