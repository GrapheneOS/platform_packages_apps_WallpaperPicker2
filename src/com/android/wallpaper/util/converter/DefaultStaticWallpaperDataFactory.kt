/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.wallpaper.util.converter

import android.content.Context
import com.android.wallpaper.model.AppResourceWallpaperInfo
import com.android.wallpaper.model.CurrentWallpaperInfo
import com.android.wallpaper.model.DefaultWallpaperInfo
import com.android.wallpaper.model.ImageWallpaperInfo
import com.android.wallpaper.model.LegacyPartnerWallpaperInfo
import com.android.wallpaper.model.SystemStaticWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.model.wallpaper.ImageWallpaperData
import com.android.wallpaper.model.wallpaper.StaticWallpaperData

/**
 * This class is responsible for creating various data classes for LiveWallpaperModel. It achieves
 * this in two ways:
 *
 * A. By utilizing instances of different types of WallpaperInfo classes, which provide attributes
 * common to both new and old classes.
 *
 * B. By directly passing the necessary attributes required to construct the objects.
 *
 * The methods in this class should only be used to create an instance of StaticWallpaperModel.
 */
open class DefaultStaticWallpaperDataFactory {

    /**
     * Creates an instance of StaticWallpaperData.
     *
     * @param wallpaperAsset: Refers to the asset that the static wallpaper data class would
     *   contain.
     */
    open fun getStaticWallpaperData(
        wallpaperInfo: WallpaperInfo,
        context: Context
    ): StaticWallpaperData {
        return if (
            wallpaperInfo is AppResourceWallpaperInfo ||
                wallpaperInfo is LegacyPartnerWallpaperInfo ||
                wallpaperInfo is SystemStaticWallpaperInfo ||
                wallpaperInfo is DefaultWallpaperInfo ||
                wallpaperInfo is CurrentWallpaperInfo ||
                wallpaperInfo is ImageWallpaperInfo
        ) {
            StaticWallpaperData(asset = wallpaperInfo.getAsset(context))
        } else {
            throw IllegalArgumentException(
                "Invalid wallpaperInfo type: ${wallpaperInfo::class.simpleName}"
            )
        }
    }

    /**
     * Creates an instance of ImageWallpaperData.
     *
     * @param wallpaperInfo: Refers to the object of ImageWallpaperInfo that is used for creating an
     *   object of type ImageWallpaperData.
     */
    fun getImageWallpaperData(wallpaperInfo: WallpaperInfo): ImageWallpaperData? {
        return if (wallpaperInfo is ImageWallpaperInfo) {
            ImageWallpaperData(wallpaperInfo.uri)
        } else null
    }
}
