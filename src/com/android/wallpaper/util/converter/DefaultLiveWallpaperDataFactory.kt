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
import android.app.WallpaperManager.FLAG_LOCK
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.Context
import com.android.wallpaper.model.CreativeWallpaperInfo
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.wallpaper.CreativeWallpaperData
import com.android.wallpaper.model.wallpaper.LiveWallpaperData

/**
 * This class is responsible for creating various data classes for LiveWallpaperModel. It achieves
 * this in two ways:
 *
 * A. By utilizing instances of different types of WallpaperInfo classes, which provide attributes
 * common to both new and old classes.
 *
 * B. By directly passing the necessary attributes required to construct the objects.
 *
 * The methods in this class should only be used to create an instance of LiveWallpaperModel.
 */
open class DefaultLiveWallpaperDataFactory {

    /**
     * Creates an instance of CreativeWallpaperData object.
     *
     * @param wallpaperInfo reference to object of CreativeWallpaperInfo that is used for attributes
     *   common to the old and new model class.
     */
    fun getCreativeWallpaperData(wallpaperInfo: LiveWallpaperInfo): CreativeWallpaperData? {
        return (wallpaperInfo as? CreativeWallpaperInfo)?.let {
            CreativeWallpaperData(
                configPreviewUri = it.configPreviewUri,
                cleanPreviewUri = it.cleanPreviewUri,
                deleteUri = it.deleteUri,
                thumbnailUri = it.thumbnailUri,
                shareUri = it.shareUri,
                author = it.author ?: "",
                description = it.description ?: "",
                contentDescription = it.contentDescription,
                isCurrent = it.isCurrent.toString() // Convert boolean to String
            )
        }
    }

    /**
     * Creates an instance of LiveWallpaperData object.
     *
     * @param wallpaperInfo: Reference to object of LiveWallpaperInfo that is used for attributes
     *   common to the old and new model class.
     */
    open fun getLiveWallpaperData(
        wallpaperInfo: LiveWallpaperInfo,
        context: Context
    ): LiveWallpaperData {
        val groupNameOfWallpaper = (wallpaperInfo as? CreativeWallpaperInfo)?.groupName ?: ""
        val wallpaperManager = WallpaperManager.getInstance(context)
        val currentHomeWallpaper = wallpaperManager.getWallpaperInfo(FLAG_SYSTEM)
        val currentLockWallpaper = wallpaperManager.getWallpaperInfo(FLAG_LOCK)
        return LiveWallpaperData(
            groupName = groupNameOfWallpaper,
            isDownloadable = false,
            systemWallpaperInfo = wallpaperInfo.info,
            isTitleVisible = wallpaperInfo.isVisibleTitle,
            isApplied = wallpaperInfo.isApplied(currentHomeWallpaper, currentLockWallpaper),
            effectNames = wallpaperInfo.effectNames,
        )
    }
}
