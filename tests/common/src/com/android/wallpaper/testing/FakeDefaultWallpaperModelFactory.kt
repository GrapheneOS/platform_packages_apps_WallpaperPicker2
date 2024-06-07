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

package com.android.wallpaper.testing

import android.content.Context
import android.net.Uri
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.util.converter.WallpaperModelFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDefaultWallpaperModelFactory @Inject constructor() : WallpaperModelFactory {

    private var staticWallpaperModel: WallpaperModel? = null
    private var liveWallpaperModel: WallpaperModel? = null

    override fun getWallpaperModel(context: Context, wallpaperInfo: WallpaperInfo): WallpaperModel {
        return when (wallpaperInfo) {
            is LiveWallpaperInfo -> {
                liveWallpaperModel
                    ?: WallpaperModelUtils.getLiveWallpaperModel(
                            wallpaperId = "testWallpaperId",
                            collectionId = "testCollectionId",
                            systemWallpaperInfo = wallpaperInfo.info,
                        )
                        .also { liveWallpaperModel = it }
            }
            else -> {
                staticWallpaperModel
                    ?: WallpaperModelUtils.getStaticWallpaperModel(
                            wallpaperId = "testWallpaperId",
                            collectionId = "testCollection",
                            imageWallpaperUri = Uri.parse("content://com.test/image")
                        )
                        .also { staticWallpaperModel = it }
            }
        }
    }

    fun getStaticWallpaperModel(): WallpaperModel? {
        return staticWallpaperModel
    }

    fun setStaticWallpaperModel(model: WallpaperModel) {
        staticWallpaperModel = model
    }

    fun getLiveWallpaperModel(): WallpaperModel? {
        return liveWallpaperModel
    }

    fun setLiveWallpaperModel(model: WallpaperModel) {
        liveWallpaperModel = model
    }
}
