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
 */

package com.android.wallpaper.testing

import android.app.WallpaperColors
import android.app.WallpaperInfo
import android.content.ComponentName
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.picker.data.ColorInfo
import com.android.wallpaper.picker.data.CommonWallpaperData
import com.android.wallpaper.picker.data.CreativeWallpaperData
import com.android.wallpaper.picker.data.Destination
import com.android.wallpaper.picker.data.DownloadableWallpaperData
import com.android.wallpaper.picker.data.ImageWallpaperData
import com.android.wallpaper.picker.data.LiveWallpaperData
import com.android.wallpaper.picker.data.StaticWallpaperData
import com.android.wallpaper.picker.data.WallpaperId
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.util.converter.WallpaperModelFactory

class WallpaperModelUtils {
    companion object {
        const val SAMPLE_TITLE1 = "wallpaper-1"
        const val SAMPLE_TITLE2 = "wallpaper-2"
        const val DEFAULT_PLACEHOLDER_COLOR = 1200
        const val DEFAULT_ACTION_URL = "http://www.bogus.com"
        val DEFAULT_COLORS =
            WallpaperColors(
                Color.valueOf(Color.RED),
                Color.valueOf(Color.GREEN),
                Color.valueOf(Color.BLUE)
            )
        val DEFAULT_ASSET = TestAsset(TestStaticWallpaperInfo.COLOR_DEFAULT, false)
        const val DEFAULT_GROUP_NAME = "group name"

        fun getStaticWallpaperModel(
            wallpaperId: String,
            collectionId: String,
            placeholderColor: Int = DEFAULT_PLACEHOLDER_COLOR,
            attribution: List<String>? = emptyList(),
            actionUrl: String? = DEFAULT_ACTION_URL,
            colors: WallpaperColors = DEFAULT_COLORS,
            asset: Asset = DEFAULT_ASSET,
            imageWallpaperUri: Uri = Uri.EMPTY,
            downloadableWallpaperData: DownloadableWallpaperData? = null,
            cropHints: Map<Point, Rect> = emptyMap(),
        ): WallpaperModel.StaticWallpaperModel {
            return WallpaperModel.StaticWallpaperModel(
                commonWallpaperData =
                    CommonWallpaperData(
                        id =
                            WallpaperId(
                                ComponentName(
                                    WallpaperModelFactory.STATIC_WALLPAPER_PACKAGE,
                                    WallpaperModelFactory.STATIC_WALLPAPER_CLASS
                                ),
                                wallpaperId,
                                collectionId,
                            ),
                        title = SAMPLE_TITLE1,
                        attributions = attribution,
                        exploreActionUrl = actionUrl,
                        thumbAsset = asset,
                        placeholderColorInfo =
                            ColorInfo(
                                colors,
                                placeholderColor,
                            ),
                        destination = Destination.NOT_APPLIED,
                    ),
                staticWallpaperData =
                    StaticWallpaperData(
                        asset,
                        cropHints,
                    ),
                imageWallpaperData = ImageWallpaperData(imageWallpaperUri),
                networkWallpaperData = null,
                downloadableWallpaperData = downloadableWallpaperData,
            )
        }

        fun getLiveWallpaperModel(
            wallpaperId: String,
            collectionId: String,
            placeholderColor: Int = DEFAULT_PLACEHOLDER_COLOR,
            attribution: List<String>? = emptyList(),
            actionUrl: String? = DEFAULT_ACTION_URL,
            colors: WallpaperColors = DEFAULT_COLORS,
            asset: Asset = DEFAULT_ASSET,
            groupName: String = DEFAULT_GROUP_NAME,
            systemWallpaperInfo: WallpaperInfo,
            isTitleVisible: Boolean = true,
            isApplied: Boolean = true,
            effectNames: String? = null,
            creativeWallpaperData: CreativeWallpaperData? = null,
        ): WallpaperModel.LiveWallpaperModel {
            return WallpaperModel.LiveWallpaperModel(
                commonWallpaperData =
                    CommonWallpaperData(
                        id =
                            WallpaperId(
                                systemWallpaperInfo.component,
                                wallpaperId,
                                collectionId,
                            ),
                        title = SAMPLE_TITLE2,
                        attributions = attribution,
                        exploreActionUrl = actionUrl,
                        thumbAsset = asset,
                        placeholderColorInfo =
                            ColorInfo(
                                colors,
                                placeholderColor,
                            ),
                        destination = Destination.NOT_APPLIED,
                    ),
                liveWallpaperData =
                    LiveWallpaperData(
                        groupName,
                        systemWallpaperInfo,
                        isTitleVisible,
                        isApplied,
                        effectNames != null,
                        effectNames
                    ),
                creativeWallpaperData = creativeWallpaperData,
                internalLiveWallpaperData = null,
            )
        }
    }
}
