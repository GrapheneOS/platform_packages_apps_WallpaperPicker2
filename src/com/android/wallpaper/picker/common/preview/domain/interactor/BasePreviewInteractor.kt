/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.wallpaper.picker.common.preview.domain.interactor

import com.android.wallpaper.model.WallpaperModelsPair
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

// Based on WallpaperPreviewInteractor, except cleaned up to only bind wallpaper and workspace
// (workspace binding to be added). Also included the ability to preview current wallpapers when no
// previewing wallpaper is set.
@ActivityRetainedScoped
class BasePreviewInteractor
@Inject
constructor(
    wallpaperPreviewRepository: WallpaperPreviewRepository,
    wallpaperRepository: WallpaperRepository,
) {
    private val previewingWallpaper: StateFlow<WallpaperModel?> =
        wallpaperPreviewRepository.wallpaperModel
    private val currentWallpapers: Flow<WallpaperModelsPair> =
        wallpaperRepository.currentWallpaperModels

    val wallpapers: Flow<WallpaperModelsPair> =
        combine(previewingWallpaper, currentWallpapers) { previewingWallpaper, currentWallpapers ->
            if (previewingWallpaper != null) {
                // Preview wallpaper on both the home and lock screens if set.
                WallpaperModelsPair(previewingWallpaper, null)
            } else {
                currentWallpapers
            }
        }
}
