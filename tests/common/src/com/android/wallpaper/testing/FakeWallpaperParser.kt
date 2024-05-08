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

import android.content.res.XmlResourceParser
import com.android.wallpaper.model.SystemStaticWallpaperInfo
import com.android.wallpaper.model.WallpaperCategory
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.util.WallpaperParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeWallpaperParser @Inject constructor() : WallpaperParser {
    var wallpapers: List<WallpaperInfo> = emptyList()

    override fun parseSystemCategories(parser: XmlResourceParser): List<WallpaperCategory> {
        val wallpapers = listOf(fakeSystemStaticWallpaperInfo)
        return listOf(
            WallpaperCategory(
                /* title= */ "sample-title-1",
                /* collectionId= */ "sample-collection-id",
                wallpapers,
                1
            )
        )
    }

    override fun parsePartnerWallpaperInfoResources(): List<WallpaperInfo> {
        return wallpapers
    }

    fun setPartnerWallpapers(wallpapers: List<WallpaperInfo>) {
        this.wallpapers = wallpapers
    }

    companion object FakeWallpaperData {
        val fakeSystemStaticWallpaperInfo =
            SystemStaticWallpaperInfo(
                "fake_package_name",
                "fake_wallpaper_1",
                "fake_category_id",
                0, // drawableResId
                0, // titleResId
                0, // subtitle1ResId
                0, // subtitle2ResId
                0, // actionTypeResId
                0, // actionUrlResId
                0 // thumbnailResId
            )
    }
}
