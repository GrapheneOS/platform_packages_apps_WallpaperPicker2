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

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.android.wallpaper.model.CurrentWallpaperInfo
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.model.wallpaper.ColorInfo
import com.android.wallpaper.model.wallpaper.CommonWallpaperData
import com.android.wallpaper.model.wallpaper.Destination
import com.android.wallpaper.model.wallpaper.WallpaperId

/**
 * This class is responsible for creating CommonWallpaperData object. It achieves this in two ways:
 *
 * A. By utilizing instances of wallpaperInfo class, which provide attributes common to both new and
 * old classes.
 *
 * B. By directly passing the other attributes required to construct the CommonWallpaperData object.
 */
class CommonWallpaperDataFactory {

    companion object {
        const val TAG = "CommonWallpaperDataFactory"
        private const val STATIC_WALLPAPER_PACKAGE = "StaticWallpaperPackage"
        private const val STATIC_WALLPAPER_CLASS = "StaticWallpaperClass"

        fun getCommonWallpaperData(
            wallpaperInfo: WallpaperInfo,
            context: Context
        ): CommonWallpaperData {

            var wallpaperDestination = Destination.NOT_APPLIED
            if (wallpaperInfo is CurrentWallpaperInfo) {
                wallpaperDestination =
                    when (wallpaperInfo.wallpaperManagerFlag) {
                        WallpaperManager.FLAG_SYSTEM -> Destination.APPLIED_TO_SYSTEM
                        WallpaperManager.FLAG_LOCK -> Destination.APPLIED_TO_LOCK
                        WallpaperManager.FLAG_LOCK and WallpaperManager.FLAG_SYSTEM ->
                            Destination.APPLIED_TO_SYSTEM_LOCK
                        else -> {
                            Log.w(
                                TAG,
                                "Invalid value for wallpaperManagerFlag: " +
                                    wallpaperInfo.wallpaperManagerFlag
                            )
                            Destination.NOT_APPLIED
                        }
                    }
            }

            // componentName is a valid value for liveWallpapers, for other types of wallpapers
            // (which are static) we can have a constant value
            val componentNameForWallpaper =
                if (wallpaperInfo is LiveWallpaperInfo) {
                    wallpaperInfo.wallpaperComponent.component
                } else {
                    ComponentName(STATIC_WALLPAPER_PACKAGE, STATIC_WALLPAPER_CLASS)
                }

            val uniqueWallpaperId =
                WallpaperId(
                    componentName = componentNameForWallpaper,
                    uniqueId = wallpaperInfo.wallpaperId,
                    collectionId = wallpaperInfo.getCollectionId(context)
                )

            val attributions =
                mutableListOf<Pair<String, String>>(
                    Pair(
                        DefaultLiveWallpaperDataFactory.ACTION_URL,
                        wallpaperInfo.getActionUrl(context)
                    ),
                )

            val colorInfoOfWallpaper =
                ColorInfo(
                    wallpaperInfo.colorInfo.wallpaperColors,
                    wallpaperInfo.colorInfo.placeholderColor
                )

            return CommonWallpaperData(
                id = uniqueWallpaperId,
                title = wallpaperInfo.getTitle(context),
                collectionId = wallpaperInfo.getCollectionId(context),
                attributions = attributions,
                thumbAsset = wallpaperInfo.getThumbAsset(context),
                placeholderColorInfo = colorInfoOfWallpaper,
                destination = wallpaperDestination
            )
        }
    }
}
