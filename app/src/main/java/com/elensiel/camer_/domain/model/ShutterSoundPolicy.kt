package com.elensiel.camer_.domain.model

import android.annotation.SuppressLint
import android.content.res.Resources
import android.media.MediaActionSound
import android.os.Build

/**
 * Reports whether this device's region forces the camera shutter sound.
 *
 * Some regions (e.g. Japan, South Korea) legally require the shutter sound
 * to play, so the user must not be able to turn it off. [isForced] is the
 * single source of truth for that rule. Use it to override the user's
 * setting and to lock the settings switch.
 */
object ShutterSoundPolicy {

    /**
     * `true` if the shutter sound is region-locked and must always play.
     *
     * Evaluated once on first access and cached, since the value is a fixed
     * device property that never changes at runtime.
     *
     * - API 33+: uses the official [MediaActionSound.mustPlayShutterSound].
     * - API 26-32: no public API exists, so this reads the framework's
     *   `config_camera_sound_forced` resource. This is unofficial. It may be
     *   missing on some devices, in which case the result is `false` and the
     *   sound stays user-controllable.
     */
    val isForced: Boolean by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MediaActionSound.mustPlayShutterSound()
        } else {
            legacyForcedFlag()
        }
    }

    /**
     * Pre-33 fallback. Looks up the framework bool `config_camera_sound_forced`
     * via [Resources.getSystem]. The resource is not in the public `android.R`,
     * so it can only be reached by name. Any failure is treated as "not forced".
     */
    @SuppressLint("DiscouragedApi")
    private fun legacyForcedFlag(): Boolean = runCatching {
        val res = Resources.getSystem()
        val id = res.getIdentifier("config_camera_sound_forced", "bool", "android")
        id != 0 && res.getBoolean(id)
    }.getOrDefault(false)
}
