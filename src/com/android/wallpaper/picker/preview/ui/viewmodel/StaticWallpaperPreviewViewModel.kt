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
package com.android.wallpaper.picker.preview.ui.viewmodel

import android.app.WallpaperColors
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Point
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.asset.CurrentWallpaperAssetVN
import com.android.wallpaper.dispatchers.BackgroundDispatcher
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** Top level [ViewModel] for [WallpaperPreviewActivity] and its fragments */
@HiltViewModel
class StaticWallpaperPreviewViewModel
@Inject
constructor(
    private val wallpaperPreferences: WallpaperPreferences,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private var initialized = false

    private val _lowResBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val lowResBitmap: Flow<Bitmap> = _lowResBitmap.filterNotNull()

    private val _cachedWallpaperColors: MutableStateFlow<WallpaperColors?> = MutableStateFlow(null)
    private val croppedBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val wallpaperColors: Flow<WallpaperColors> =
        merge(
            _cachedWallpaperColors.filterNotNull(),
            croppedBitmap
                .filterNotNull()
                .map { it.extractColors() }
                .filterNotNull()
                .flowOn(bgDispatcher),
        )

    // Wallpaper ID is required to cache the wallpaper colors to the preferences
    private var wallpaperId: String? = null
    private val wallpaperAsset: MutableStateFlow<Asset?> = MutableStateFlow(null)
    val subsamplingScaleImageViewModel: Flow<FullResWallpaperViewModel> =
        wallpaperAsset
            .filterNotNull()
            .map {
                val dimensions = it.decodeRawDimensions()
                val bitmap = it.decodeBitmap(dimensions)
                if (bitmap != null) {
                    if (_cachedWallpaperColors.value == null && wallpaperId != null) {
                        // If no cached colors from the preferences, extra colors from the original
                        // bitmap and cache them to the preferences.
                        val colors = bitmap.extractColors()
                        _cachedWallpaperColors.value = colors
                        wallpaperPreferences.storeWallpaperColors(wallpaperId, colors)
                    }
                    FullResWallpaperViewModel(
                        bitmap,
                        dimensions,
                        offsetToStart = it is CurrentWallpaperAssetVN,
                    )
                } else {
                    null
                }
            }
            .filterNotNull()
            .flowOn(bgDispatcher)

    /**
     * Init function for setting the wallpaper info that is retrieved from the intent bundle when
     * onCreate() in Activity or Fragment.
     */
    fun initializeViewModel(context: Context, wallpaper: WallpaperInfo) {
        val appContext = context.applicationContext
        if (!initialized) {
            val asset: Asset? = wallpaper.getAsset(appContext)
            val id: String? = wallpaper.getStoredWallpaperId(appContext)
            wallpaperAsset.value = asset
            wallpaperId = id
            id?.let { wallpaperPreferences.getWallpaperColors(it) }
                ?.run { _cachedWallpaperColors.value = this }
            viewModelScope.launch(bgDispatcher) {
                _lowResBitmap.value = asset?.getLowResBitmap(appContext)
            }
            initialized = true
        }
    }

    // TODO b/296288298 Create a util class for Bitmap and Asset
    private suspend fun Asset.decodeRawDimensions(): Point =
        suspendCancellableCoroutine { k: CancellableContinuation<Point> ->
            val callback = Asset.DimensionsReceiver { k.resumeWith(Result.success(Point(it))) }
            decodeRawDimensions(null, callback)
        }

    // TODO b/296288298 Create a util class functions for Bitmap and Asset
    private suspend fun Asset.decodeBitmap(dimensions: Point): Bitmap? =
        suspendCancellableCoroutine { k: CancellableContinuation<Bitmap?> ->
            val callback = Asset.BitmapReceiver { k.resumeWith(Result.success(it)) }
            decodeBitmap(dimensions.x, dimensions.y, callback)
        }

    // TODO b/296288298 Create a util class functions for Bitmap and Asset
    private fun Bitmap.extractColors(): WallpaperColors? {
        val tmpOut = ByteArrayOutputStream()
        var shouldRecycle = false
        var cropped = this
        if (cropped.compress(Bitmap.CompressFormat.PNG, 100, tmpOut)) {
            val outByteArray = tmpOut.toByteArray()
            val options = BitmapFactory.Options()
            options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            cropped = BitmapFactory.decodeByteArray(outByteArray, 0, outByteArray.size)
        }
        if (cropped.config == Bitmap.Config.HARDWARE) {
            cropped = cropped.copy(Bitmap.Config.ARGB_8888, false)
            shouldRecycle = true
        }
        val colors = WallpaperColors.fromBitmap(cropped)
        if (shouldRecycle) {
            cropped.recycle()
        }
        return colors
    }
}
