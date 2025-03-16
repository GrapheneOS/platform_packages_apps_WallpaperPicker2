/*
 * Copyright (C) 2025 The Android Open Source Project
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

import com.android.wallpaper.picker.category.domain.interactor.OnDeviceWallpapersInteractor
import com.android.wallpaper.picker.data.WallpaperModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class FakeOnDeviceWallpapersInteractor @Inject constructor() : OnDeviceWallpapersInteractor {
    private val _defaultWallpapers = MutableStateFlow(fakeOnDeviceWallpapers)

    override val defaultWallpapers: Flow<List<WallpaperModel>>
        get() = _defaultWallpapers

    fun setDefaultWallpapers(newWallpapers: List<WallpaperModel.StaticWallpaperModel>) {
        _defaultWallpapers.value = newWallpapers
    }

    companion object {
        val fakeOnDeviceWallpapers =
            listOf(
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId1",
                    collectionId = "testCollection1",
                    title = "onDeviceTitle1",
                ),
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId2",
                    collectionId = "testCollection3",
                    title = "onDeviceTitle2",
                ),
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId3",
                    collectionId = "testCollection3",
                    title = "onDeviceTitle3",
                ),
            )
    }
}
