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

package com.android.wallpaper.picker.preview.ui.util

import android.app.WallpaperInfo
import android.app.wallpaper.WallpaperDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.wallpaper.WallpaperService
import android.util.Log
import com.android.wallpaper.picker.data.LiveWallpaperData
import com.android.wallpaper.picker.data.WallpaperId
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils.Companion.isExtendedEffectWallpaper
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.xmlpull.v1.XmlPullParserException

object ContentHandlingUtil {

    private const val TAG = "ContentHandlingUtil"

    /**
     * Updates the current preview using the WallpaperDescription returned with the Intent if any.
     *
     * For creative wallpaper and extended effect wallpaper. Use [ImageEffectsRepositoryImpl] for
     * updating effect wallpaper preview.
     */
    fun updatePreview(
        context: Context,
        wallpaperModel: WallpaperModel,
        wallpaperDescription: WallpaperDescription? = null,
        onNewModel: suspend (LiveWallpaperModel) -> Unit,
    ) {
        if (
            wallpaperDescription == null ||
                (wallpaperModel !is LiveWallpaperModel && wallpaperDescription.component == null)
        )
            return

        val description =
            if (wallpaperDescription.component != null) {
                wallpaperDescription
            } else {
                // Live wallpaper services can't provide their component name, so set it here
                wallpaperDescription
                    .toBuilder()
                    .setComponent(
                        (wallpaperModel as LiveWallpaperModel)
                            .liveWallpaperData
                            .systemWallpaperInfo
                            .component
                    )
                    .build()
            }

        val newWallpaperModel: LiveWallpaperModel? =
            if (wallpaperModel is LiveWallpaperModel) {
                wallpaperModel
            } else {
                (wallpaperModel as StaticWallpaperModel).toLiveWallpaperModel(
                    context = context,
                    componentName = checkNotNull(description.component),
                    assetId = description.id,
                    isEffectWallpaper =
                        isExtendedEffectWallpaper(context, checkNotNull(description.component)),
                )
            }

        newWallpaperModel?.let {
            val sourceLiveData = it.liveWallpaperData
            val updatedLiveData =
                LiveWallpaperData(
                    sourceLiveData.groupName,
                    sourceLiveData.systemWallpaperInfo,
                    sourceLiveData.isTitleVisible,
                    sourceLiveData.isApplied,
                    sourceLiveData.isEffectWallpaper,
                    sourceLiveData.effectNames,
                    sourceLiveData.contextDescription,
                    description,
                )
            val updatedWallpaper =
                LiveWallpaperModel(
                    it.commonWallpaperData,
                    updatedLiveData,
                    it.creativeWallpaperData,
                    it.internalLiveWallpaperData,
                )
            runBlocking { onNewModel(updatedWallpaper) }
        }
    }

    /** Queries the wallpaper info of a given live wallpaper component name. */
    fun queryLiveWallpaperInfo(context: Context, componentName: ComponentName): WallpaperInfo? {
        val resolveInfos =
            Intent(WallpaperService.SERVICE_INTERFACE)
                .apply { setClassName(componentName.packageName, componentName.className) }
                .let {
                    context.packageManager.queryIntentServices(it, PackageManager.GET_META_DATA)
                }
        if (resolveInfos.isEmpty()) {
            Log.w(TAG, "Couldn't find live wallpaper for " + componentName.className)
            return null
        }

        try {
            return WallpaperInfo(context, resolveInfos[0])
        } catch (e: XmlPullParserException) {
            Log.w(TAG, "Skipping wallpaper " + resolveInfos[0].serviceInfo, e)
            return null
        } catch (e: IOException) {
            Log.w(TAG, "Skipping wallpaper " + resolveInfos[0].serviceInfo, e)
            return null
        }
    }

    /**
     * Transforms a static wallpaper model to a live wallpaper model. Used by effect wallpapers when
     * the static image becomes animated.
     */
    fun StaticWallpaperModel.toLiveWallpaperModel(
        context: Context,
        componentName: ComponentName,
        assetId: String? = null,
        isEffectWallpaper: Boolean = false,
        effectNames: String? = null,
        description: WallpaperDescription? = null,
    ): LiveWallpaperModel? {
        return queryLiveWallpaperInfo(context, componentName)?.let {
            val commonWallpaperData =
                commonWallpaperData.copy(
                    id =
                        WallpaperId(
                            componentName = componentName,
                            // TODO(b/390630367) Remove assetId from uniqueId
                            uniqueId =
                                if (assetId != null) "${it.serviceName}_$assetId"
                                else it.serviceName,
                            collectionId = commonWallpaperData.id.collectionId,
                        )
                )
            val liveWallpaperData =
                LiveWallpaperData(
                    groupName = "",
                    systemWallpaperInfo = it,
                    isTitleVisible = false,
                    isApplied = false,
                    isEffectWallpaper = isEffectWallpaper,
                    effectNames = effectNames,
                    description =
                        description
                            ?: WallpaperDescription.Builder().setComponent(componentName).build(),
                    supportsMultipleEngines = true,
                )
            return LiveWallpaperModel(
                commonWallpaperData = commonWallpaperData,
                liveWallpaperData = liveWallpaperData,
                creativeWallpaperData = null,
                internalLiveWallpaperData = null,
            )
        }
    }
}
