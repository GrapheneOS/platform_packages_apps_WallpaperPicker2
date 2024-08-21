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

import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.picker.category.client.LiveWallpapersClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeLiveWallpaperClientImpl @Inject constructor() : LiveWallpapersClient {
    override fun getAll(excludedPackageNames: Set<String?>?): List<WallpaperInfo> {
        val attributions: MutableList<String> = ArrayList()
        attributions.add("Title")
        attributions.add("Subtitle 1")
        attributions.add("Subtitle 2")

        val mTestLiveWallpaper = TestLiveWallpaperInfo(TestStaticWallpaperInfo.COLOR_DEFAULT)
        mTestLiveWallpaper.setAttributions(attributions)
        mTestLiveWallpaper.collectionId = "collectionLive"
        mTestLiveWallpaper.wallpaperId = "wallpaperLive"
        return listOf(mTestLiveWallpaper)
    }
}
