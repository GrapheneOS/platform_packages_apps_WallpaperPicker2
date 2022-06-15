/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.wallpaper.model

import android.app.Activity
import android.content.Context
import android.os.Parcel
import com.android.wallpaper.asset.Asset

/**
 * Interface for adaptive wallpaper info model.
 */
abstract class AdaptiveWallpaperInfo : WallpaperInfo {

    protected var mUnitId: Long
    protected var mAdaptiveTypeWallpaperInfoMap: MutableMap<AdaptiveType, WallpaperInfo> = HashMap()

    companion object {
        private val DEFAULT_TYPE = AdaptiveType.LIGHT
    }

    protected constructor(
        unitId: Long, adaptiveType: AdaptiveType,
        wallpaperInfo: WallpaperInfo
    ) {
        mUnitId = unitId
        updateAdaptiveWallpaperInfo(adaptiveType, wallpaperInfo)
    }

    protected constructor(`in`: Parcel) : super(`in`) {
        mUnitId = `in`.readLong()
    }

    /**
     * Updates wallpaperInfo to AdaptiveWallpaperInfo hashmap.
     *
     * @param adaptiveType The type for hashmap key.
     * @param wallpaperInfo The wallpaperInfo for hashmap value.
     */
    protected fun updateAdaptiveWallpaperInfo(
        adaptiveType: AdaptiveType,
        wallpaperInfo: WallpaperInfo
    ) {
        mAdaptiveTypeWallpaperInfoMap[adaptiveType] = wallpaperInfo
    }

    override fun getAttributions(context: Context): List<String>? {
        return mAdaptiveTypeWallpaperInfoMap[DEFAULT_TYPE]?.getAttributions(context)
    }

    override fun getBaseImageUrl(): String? {
        return mAdaptiveTypeWallpaperInfoMap[DEFAULT_TYPE]?.baseImageUrl
    }

    override fun getActionUrl(unused: Context): String? {
        return mAdaptiveTypeWallpaperInfoMap[DEFAULT_TYPE]?.getActionUrl(unused)
    }

    override fun getActionLabelRes(context: Context): Int {
        return mAdaptiveTypeWallpaperInfoMap[DEFAULT_TYPE]?.getActionLabelRes(context) ?: 0
    }

    override fun getActionIconRes(context: Context): Int {
        return mAdaptiveTypeWallpaperInfoMap[DEFAULT_TYPE]?.getActionIconRes(context) ?: 0
    }

    override fun getAsset(context: Context): Asset? {
        return mAdaptiveTypeWallpaperInfoMap[DEFAULT_TYPE]?.getAsset(context)
    }

    override fun getThumbAsset(context: Context): Asset? {
        return mAdaptiveTypeWallpaperInfoMap[DEFAULT_TYPE]?.getThumbAsset(context)
    }

    override fun getDesktopAsset(context: Context): Asset? {
        return mAdaptiveTypeWallpaperInfoMap[DEFAULT_TYPE]?.getDesktopAsset(context)
    }

    override fun getCollectionId(unused: Context): String? {
        return mAdaptiveTypeWallpaperInfoMap[DEFAULT_TYPE]?.getCollectionId(unused)
    }

    override fun getWallpaperId(): String? {
        return mAdaptiveTypeWallpaperInfoMap[DEFAULT_TYPE]?.wallpaperId
    }

    override fun showPreview(
        srcActivity: Activity, factory: InlinePreviewIntentFactory, requestCode: Int
    ) {
        srcActivity.startActivityForResult(
            factory.newIntent(srcActivity, this), requestCode
        )
    }

    /**
     * Gets the [Asset] by [AdaptiveType].
     */
    fun getAdaptiveAsset(context: Context?, adaptiveType: AdaptiveType): Asset? {
        return mAdaptiveTypeWallpaperInfoMap[adaptiveType]?.getAsset(context)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeLong(mUnitId)
    }
}
