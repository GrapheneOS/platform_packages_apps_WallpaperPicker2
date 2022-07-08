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
package com.android.wallpaper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.location.Location
import android.util.Log
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.model.AdaptiveType
import com.android.wallpaper.model.AdaptiveWallpaperInfo
import com.android.wallpaper.module.BitmapCropper
import com.android.wallpaper.module.InjectorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Util class for adaptive wallpaper.
 */
object AdaptiveWallpaperUtils {
    private const val TAG = "AdaptiveWallpaperUtils"

    /**
     * Gets adaptive wallpaper image filename.
     *
     * @param type The adaptive type to get filename. It should be light or dark type.
     * @return The correspond adaptive type adaptive image filename.
     */
    @JvmStatic
    fun getAdaptiveWallpaperFilename(type: AdaptiveType): String {
        return "wp-adaptive-" + type.toString().lowercase(Locale.ROOT) + ".png"
    }

    /**
     * Saves adaptive wallpaper images.
     */
    @JvmStatic
    fun saveAdaptiveWallpaperImage(
        context: Context, lightBitmap: Bitmap,
        darkBitmap: Bitmap, myCallback: (success: Boolean, throwable: Throwable?) -> Unit
    ) {
        try {
            lightBitmap.compress(
                Bitmap.CompressFormat.PNG, 100,
                context.openFileOutput(
                    getAdaptiveWallpaperFilename(AdaptiveType.LIGHT),
                    Context.MODE_PRIVATE
                )
            )
            darkBitmap.compress(
                Bitmap.CompressFormat.PNG, 100,
                context.openFileOutput(
                    getAdaptiveWallpaperFilename(AdaptiveType.DARK),
                    Context.MODE_PRIVATE
                )
            )
            lightBitmap.recycle()
            darkBitmap.recycle()
            myCallback.invoke(/* success= */ true, null)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save adaptive wallpaper image.", e)
            myCallback.invoke(/* success= */ false, e)
        }
    }

    /**
     * Crops and saves adaptive wallpaper images.
     */
    @JvmStatic
    fun cropAndSaveAdaptiveWallpaper(
        context: Context,
        wallpaperScale: Float,
        scaledCropRect: Rect,
        adaptiveWallpaperInfo: AdaptiveWallpaperInfo,
        myCallback: (success: Boolean, throwable: Throwable?) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope.launch {
            val lightCroppedBitmap = cropAndScaleBitmap(
                context,
                wallpaperScale,
                scaledCropRect,
                adaptiveWallpaperInfo.getAdaptiveAsset(context, AdaptiveType.LIGHT)
            )
            val darkCroppedBitmap = cropAndScaleBitmap(
                context,
                wallpaperScale,
                scaledCropRect,
                adaptiveWallpaperInfo.getAdaptiveAsset(context, AdaptiveType.DARK)
            )
            if (lightCroppedBitmap == null || darkCroppedBitmap == null) {
                myCallback.invoke(/* success= */ false, NullPointerException())
            } else {
                saveAdaptiveWallpaperImage(
                    context,
                    lightCroppedBitmap,
                    darkCroppedBitmap,
                    myCallback
                )
            }
        }
    }

    private suspend fun cropAndScaleBitmap(
        context: Context,
        wallpaperScale: Float,
        scaledCropRect: Rect, asset: Asset?
    ): Bitmap? = suspendCoroutine { continuation ->
        InjectorProvider.getInjector().bitmapCropper.cropAndScaleBitmap(
            asset,
            wallpaperScale,
            scaledCropRect,
            WallpaperCropUtils.isRtl(context),
            object : BitmapCropper.Callback {
                override fun onBitmapCropped(croppedBitmap: Bitmap?) {
                    continuation.resume(croppedBitmap)
                }

                override fun onError(e: Throwable?) {
                    continuation.resume(null)
                }
            }
        )
    }

    /**
     * Gets the adaptive type calculated by CalendarAstronomer with location, if location is null
     * then the switch time will be 6 a.m. and 6 p.m..
     */
    @JvmStatic
    fun getCurrentAdaptiveType(currentTimeMillis: Long, location: Location?): AdaptiveType {
        val sunriseTimeMillis: Long
        val sunsetTimeMillis: Long
        val calendar = Calendar.getInstance()
        if (location == null) {
            calendar.timeInMillis = currentTimeMillis
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
            calendar[Calendar.HOUR_OF_DAY] = 6
            sunriseTimeMillis = calendar.timeInMillis
            calendar[Calendar.HOUR_OF_DAY] = 18
            sunsetTimeMillis = calendar.timeInMillis
            if (sunsetTimeMillis < currentTimeMillis || sunriseTimeMillis > currentTimeMillis) {
                return AdaptiveType.DARK
            } else {
                return AdaptiveType.LIGHT
            }
        } else {
            // TODO(b/197815029): Implement get sunset sunrise by CalendarAstronomer with location
            return AdaptiveType.NONE
        }
    }
}
