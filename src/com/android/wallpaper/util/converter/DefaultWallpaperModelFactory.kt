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
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.model.wallpaper.WallpaperModel

/**
 * This class is responsible for creating an instance of Live/Static WallpaperModel from the given
 * wallpaperInfo object.
 */
open class DefaultWallpaperModelFactory {

    private val defaultLiveWallpaperDataFactory = DefaultLiveWallpaperDataFactory()
    private val defaultStaticWallpaperDataFactory = DefaultStaticWallpaperDataFactory()

    /**
     * Composes an instance of LiveWallpaperModel depending on the type of WallpaperInfo object.
     *
     * @param context: context of the application
     * @param wallpaperInfo: wallpaperInfo object for the wallpaper being converted to the new model
     *   class
     */
    open fun getWallpaperModel(
        context: Context,
        wallpaperInfo: LiveWallpaperInfo
    ): WallpaperModel.LiveWallpaperModel {
        return WallpaperModel.LiveWallpaperModel(
            commonWallpaperData =
                CommonWallpaperDataFactory.getCommonWallpaperData(wallpaperInfo, context),
            liveWallpaperData =
                defaultLiveWallpaperDataFactory.getLiveWallpaperData(wallpaperInfo, context),
            creativeWallpaperData =
                defaultLiveWallpaperDataFactory.getCreativeWallpaperData(wallpaperInfo),
            internalLiveWallpaperData = null
        )
    }

    /**
     * Composes an instance of StaticWallpaperModel depending the type of WallpaperInfo object. This
     * will throw an exception in case you pass any type of LiveWallpaperInfo object to this method.
     *
     * @param context: context of the application
     * @param wallpaperInfo: wallpaperInfo object for the wallpaper being converted to the new model
     *   class
     */
    open fun getWallpaperModel(
        context: Context,
        wallpaperInfo: WallpaperInfo
    ): WallpaperModel.StaticWallpaperModel {
        return WallpaperModel.StaticWallpaperModel(
            commonWallpaperData =
                CommonWallpaperDataFactory.getCommonWallpaperData(wallpaperInfo, context),
            staticWallpaperData =
                defaultStaticWallpaperDataFactory.getStaticWallpaperData(wallpaperInfo, context),
            imageWallpaperData =
                defaultStaticWallpaperDataFactory.getImageWallpaperData(wallpaperInfo),
            networkWallpaperData = null
        )
    }
}
