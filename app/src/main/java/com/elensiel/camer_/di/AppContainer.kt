package com.elensiel.camer_.di

import android.content.Context
import com.elensiel.camer_.data.DataStoreSettingsRepository
import com.elensiel.camer_.data.MediaStoreRepository
import com.elensiel.camer_.data.camera.CameraXRepository

class AppContainer(context: Context) {
    val settingsRepository = DataStoreSettingsRepository(context)
    val cameraRepository = CameraXRepository(context)
    val mediaRepository = MediaStoreRepository(context)
}
