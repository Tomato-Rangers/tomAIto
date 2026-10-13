package com.elensiel.camer_.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.elensiel.camer_.domain.model.CaptureAspectRatio
import com.elensiel.camer_.domain.model.ImageFormat
import com.elensiel.camer_.domain.model.Settings
import com.elensiel.camer_.domain.model.ShutterSoundPolicy
import com.elensiel.camer_.domain.model.VolumeKeyShutter
import com.elensiel.camer_.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")


class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    private val alwaysSaveEnabled = booleanPreferencesKey("always_save_enabled")
    private val aspectKey = stringPreferencesKey("aspect_ratio")
    private val flashKey = booleanPreferencesKey("flash_enabled")
    private val imageFormatKey = stringPreferencesKey("image_format")
    private val shutterSoundKey = booleanPreferencesKey("shutter_sound_enabled")
    private val volumeKeyShutterKey = stringPreferencesKey("volume_key_shutter_mode")

    override val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            alwaysSaveEnabled = p[alwaysSaveEnabled] ?: false,

            aspectRatio = p[aspectKey]
                ?.let { runCatching { CaptureAspectRatio.valueOf(it) }.getOrNull() }
                ?: CaptureAspectRatio.RATIO_4_3,

            flashEnabled = p[flashKey] ?: false,

            imageFormat = p[imageFormatKey]
                ?.let { runCatching { ImageFormat.valueOf(it) }.getOrNull() }
                ?: ImageFormat.JPEG,

            shutterSoundEnabled = ShutterSoundPolicy.isForced || (p[shutterSoundKey] ?: false),

            volumeKeyShutter = p[volumeKeyShutterKey]
                ?.let { runCatching { VolumeKeyShutter.valueOf(it) }.getOrNull() }
                ?: VolumeKeyShutter.OFF,
        )
    }

    override suspend fun setAlwaysSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { it[alwaysSaveEnabled] = enabled }
    }

    override suspend fun setAspectRatio(ratio: CaptureAspectRatio) {
        context.dataStore.edit { it[aspectKey] = ratio.name }
    }

    override suspend fun setFlashEnabled(enabled: Boolean) {
        context.dataStore.edit { it[flashKey] = enabled }
    }

    override suspend fun setImageFormat(format: ImageFormat) {
        context.dataStore.edit { it[imageFormatKey] = format.name }
    }

    override suspend fun setShutterSoundEnabled(enabled: Boolean) {
        if (ShutterSoundPolicy.isForced) return
        context.dataStore.edit { it[shutterSoundKey] = enabled }
    }

    override suspend fun setVolumeKeyShutter(mode: VolumeKeyShutter) {
        context.dataStore.edit { it[volumeKeyShutterKey] = mode.name }
    }
}
