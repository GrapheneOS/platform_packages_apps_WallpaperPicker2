/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.picker.common.preview.data.repository

import com.android.wallpaper.picker.data.WallpaperModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * This application-scoped repository class enables the [WallpaperModel] used for preview to be
 * shared across activities. It needs to be cleaned up appropriately when it is no longer needed.
 */
@Singleton
class PersistentWallpaperModelRepository @Inject constructor() {
    /** This [WallpaperModel] represents the current selected wallpaper */
    private val _wallpaperModel = MutableStateFlow<WallpaperModel?>(null)
    val wallpaperModel: StateFlow<WallpaperModel?> = _wallpaperModel.asStateFlow()

    fun setWallpaperModel(wallpaperModel: WallpaperModel) {
        _wallpaperModel.value = wallpaperModel
    }

    fun cleanup() {
        _wallpaperModel.value = null
    }
}
