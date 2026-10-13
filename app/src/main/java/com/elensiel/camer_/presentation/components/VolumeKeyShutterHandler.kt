package com.elensiel.camer_.presentation.components

import android.view.KeyEvent
import android.view.Window
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.elensiel.camer_.domain.model.VolumeKeyShutter

@Composable
fun VolumeKeyShutterHandler(
    mode: VolumeKeyShutter,
    onShutter: () -> Unit,
) {
    val activity = LocalActivity.current ?: return
    val currentMode by rememberUpdatedState(mode)
    val currentOnShutter by rememberUpdatedState(onShutter)

    DisposableEffect(activity) {
        val window = activity.window
        val original = window.callback

        window.callback = object : Window.Callback by original {
            override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                val e = event ?: return original.dispatchKeyEvent(null)
                if (!currentMode.matches(e.keyCode)) return original.dispatchKeyEvent(e)

                if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) {
                    currentOnShutter()
                }

                return true
            }
        }

        onDispose { window.callback = original }
    }
}

private fun VolumeKeyShutter.matches(keyCode: Int) = when (this) {
    VolumeKeyShutter.OFF -> false
    VolumeKeyShutter.VOLUME_UP -> keyCode == KeyEvent.KEYCODE_VOLUME_UP
    VolumeKeyShutter.VOLUME_DOWN -> keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
    VolumeKeyShutter.BOTH -> keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
}
