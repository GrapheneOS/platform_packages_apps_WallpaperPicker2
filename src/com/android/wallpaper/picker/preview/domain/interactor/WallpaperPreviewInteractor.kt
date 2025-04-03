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

package com.android.wallpaper.picker.preview.domain.interactor

import android.app.Flags.liveWallpaperContentHandling
import android.app.WallpaperColors
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.ClockSize.Companion.getPreferredClockSize
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.model.CreativeCategory
import com.android.wallpaper.model.CreativeWallpaperInfo
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination.Companion.toDestinationInt
import com.android.wallpaper.picker.data.LiveWallpaperData
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.util.converter.WallpaperModelFactory.Companion.getCommonWallpaperData
import com.android.wallpaper.util.converter.WallpaperModelFactory.Companion.getCreativeWallpaperData
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

@ActivityRetainedScoped
class WallpaperPreviewInteractor
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val wallpaperPreviewRepository: WallpaperPreviewRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val wallpaperConnectionUtils: WallpaperConnectionUtils,
) {
    val wallpaperModel: StateFlow<WallpaperModel?> = wallpaperPreviewRepository.wallpaperModel

    val hasSmallPreviewTooltipBeenShown: StateFlow<Boolean> =
        wallpaperPreviewRepository.hasSmallPreviewTooltipBeenShown

    /** The preferred clock size of the current preview wallpaper, if any */
    val preferredClockSize: Flow<ClockSize?> =
        wallpaperModel.map {
            (it as? LiveWallpaperModel)
                ?.liveWallpaperData
                ?.description
                ?.content
                ?.getPreferredClockSize()
        }

    fun hideSmallPreviewTooltip() = wallpaperPreviewRepository.hideSmallPreviewTooltip()

    val hasFullPreviewTooltipBeenShown: StateFlow<Boolean> =
        wallpaperPreviewRepository.hasFullPreviewTooltipBeenShown

    fun hideFullPreviewTooltip() = wallpaperPreviewRepository.hideFullPreviewTooltip()

    fun setPreviewWallpaper(wallpaperModel: WallpaperModel) {
        wallpaperPreviewRepository.setWallpaperModel(wallpaperModel)
    }

    suspend fun setStaticWallpaper(
        @UserEventLogger.SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        destination: WallpaperDestination,
        wallpaperModel: StaticWallpaperModel,
        bitmap: Bitmap,
        wallpaperSize: Point,
        asset: Asset,
        fullPreviewCropModels: Map<Point, FullPreviewCropModel>? = null,
    ) {
        wallpaperRepository.setStaticWallpaper(
            setWallpaperEntryPoint,
            destination,
            wallpaperModel,
            bitmap,
            wallpaperSize,
            asset,
            fullPreviewCropModels,
        )
    }

    suspend fun setLiveWallpaper(
        @UserEventLogger.SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        destination: WallpaperDestination,
        wallpaperModel: LiveWallpaperModel,
    ) {
        // TODO(b/376846928) Move these calls to a separate injected component
        val updatedWallpaperModel =
            applyAndUpdateLiveWallpaper(destination, wallpaperModel, wallpaperConnectionUtils)
                ?: wallpaperModel

        wallpaperRepository.setLiveWallpaper(
            setWallpaperEntryPoint,
            destination,
            updatedWallpaperModel,
        )
    }

    suspend fun getWallpaperColors(bitmap: Bitmap, cropHints: Map<Point, Rect>?): WallpaperColors? =
        wallpaperRepository.getWallpaperColors(bitmap, cropHints)

    private suspend fun applyAndUpdateLiveWallpaper(
        destination: WallpaperDestination,
        wallpaperModel: LiveWallpaperModel,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
    ): LiveWallpaperModel? {
        if (liveWallpaperContentHandling()) {
            try {
                wallpaperConnectionUtils.applyWallpaper(destination, wallpaperModel)?.let {
                    val description =
                        if (it.component != null) {
                            it
                        } else {
                            it.toBuilder()
                                .setComponent(
                                    wallpaperModel.liveWallpaperData.systemWallpaperInfo.component
                                )
                                .build()
                        }
                    val sourceLiveData = wallpaperModel.liveWallpaperData
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
                    return LiveWallpaperModel(
                        wallpaperModel.commonWallpaperData,
                        updatedLiveData,
                        wallpaperModel.creativeWallpaperData,
                        wallpaperModel.internalLiveWallpaperData,
                    )
                }
            } catch (e: NoSuchMethodException) {
                // Deliberate no-op, this means the apply function was not found
            }
        }

        return wallpaperModel.creativeWallpaperData?.let {
            saveCreativeWallpaperAtExternal(wallpaperModel, destination)
        }
    }

    /**
     * Call the external app to save the creative wallpaper, and return an updated model based on
     * the response.
     */
    private fun saveCreativeWallpaperAtExternal(
        wallpaperModel: LiveWallpaperModel,
        destination: WallpaperDestination,
    ): LiveWallpaperModel? {
        wallpaperModel.getSaveWallpaperUriAndAuthority(destination)?.let { (uri, authority) ->
            try {
                context.contentResolver.acquireContentProviderClient(authority).use { client ->
                    val cursor =
                        client?.query(
                            /* url= */ uri,
                            /* projection= */ null,
                            /* selection= */ null,
                            /* selectionArgs= */ null,
                            /* sortOrder= */ null,
                        )
                    if (cursor == null || !cursor.moveToFirst()) return null
                    val info =
                        CreativeWallpaperInfo.buildFromCursor(
                            wallpaperModel.liveWallpaperData.systemWallpaperInfo,
                            cursor,
                        )
                    // NB: need to regenerate common data to update the thumbnail asset
                    return LiveWallpaperModel(
                        info.getCommonWallpaperData(context),
                        wallpaperModel.liveWallpaperData,
                        info.getCreativeWallpaperData(),
                        wallpaperModel.internalLiveWallpaperData,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed updating creative live wallpaper at external.")
            }
        }
        return null
    }

    /** Get the URI to call the external app to save the creative wallpaper. */
    private fun LiveWallpaperModel.getSaveWallpaperUriAndAuthority(
        destination: WallpaperDestination
    ): Pair<Uri, String>? {
        val uriString =
            liveWallpaperData.systemWallpaperInfo.serviceInfo.metaData.getString(
                CreativeCategory.KEY_WALLPAPER_SAVE_CREATIVE_CATEGORY_WALLPAPER
            ) ?: return null
        val uri =
            Uri.parse(uriString)
                ?.buildUpon()
                ?.appendQueryParameter("destination", destination.toDestinationInt().toString())
                ?.build() ?: return null
        val authority = uri.authority ?: return null
        return Pair(uri, authority)
    }

    companion object {
        const val TAG = "WallpaperPreviewInteractor"
    }
}
