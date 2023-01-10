/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 */

package com.android.wallpaper.picker.customization.data.content

import android.graphics.Bitmap
import com.android.wallpaper.picker.customization.shared.model.WallpaperModel
import kotlin.math.min
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWallpaperClient : WallpaperClient {

    private val _recentWallpapers = MutableStateFlow(INITIAL_RECENT_WALLPAPERS)
    private var isPaused = false
    private var deferred: (suspend () -> Unit)? = null

    fun setRecentWallpapers(recentWallpapers: List<WallpaperModel>) {
        _recentWallpapers.value = recentWallpapers
    }

    fun pause() {
        isPaused = true
    }

    suspend fun unpause() {
        isPaused = false
        deferred?.invoke()
        deferred = null
    }

    override fun recentWallpapers(
        limit: Int,
    ): Flow<List<WallpaperModel>> {
        return _recentWallpapers.map { wallpapers ->
            if (wallpapers.size > limit) {
                wallpapers.subList(0, min(limit, wallpapers.size))
            } else {
                wallpapers
            }
        }
    }

    override suspend fun getCurrentWallpaper(): WallpaperModel {
        return _recentWallpapers.value[0]
    }

    override suspend fun setWallpaper(wallpaperId: String, onDone: () -> Unit) {
        if (isPaused) {
            deferred = { setWallpaper(wallpaperId, onDone) }
        } else {
            _recentWallpapers.value =
                _recentWallpapers.value.sortedBy { it.wallpaperId != wallpaperId }
            onDone.invoke()
        }
    }

    override suspend fun loadThumbnail(wallpaperId: String): Bitmap? {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    companion object {
        val INITIAL_RECENT_WALLPAPERS =
            listOf(
                WallpaperModel(
                    wallpaperId = "zero",
                    placeholderColor = 0,
                ),
                WallpaperModel(
                    wallpaperId = "one",
                    placeholderColor = 1,
                ),
                WallpaperModel(
                    wallpaperId = "two",
                    placeholderColor = 2,
                ),
            )
    }
}
