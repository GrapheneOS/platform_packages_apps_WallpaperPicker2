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
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.location.Location
import android.location.LocationManager
import android.text.TextUtils
import android.util.Log
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.asset.BitmapUtils
import com.android.wallpaper.compat.WallpaperManagerCompat
import com.android.wallpaper.model.AdaptiveType
import com.android.wallpaper.model.AdaptiveWallpaperInfo
import com.android.wallpaper.module.BitmapCropper
import com.android.wallpaper.module.Injector
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.module.WallpaperPersister.DEST_BOTH
import com.android.wallpaper.module.WallpaperPersister.DEST_HOME_SCREEN
import com.android.wallpaper.module.WallpaperPreferences
import com.ibm.icu.impl.CalendarAstronomer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
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
     * @param type the adaptive type to get filename. It should be light or dark type.
     * @return the correspond adaptive type adaptive image filename.
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
        } else {
            val calendarAstronomer = CalendarAstronomer(
                location.longitude,
                location.latitude
            )
            calendarAstronomer.time = currentTimeMillis

            sunriseTimeMillis = calendarAstronomer.getSunRiseSet(/* rise= */ true)
            sunsetTimeMillis = calendarAstronomer.getSunRiseSet(/* rise= */ false)
        }
        return if (sunsetTimeMillis > sunriseTimeMillis) {
            if (sunsetTimeMillis < currentTimeMillis || sunriseTimeMillis > currentTimeMillis) {
                AdaptiveType.DARK
            } else {
                AdaptiveType.LIGHT
            }
        } else {
            if (sunsetTimeMillis < currentTimeMillis && currentTimeMillis < sunriseTimeMillis) {
                AdaptiveType.DARK
            } else {
                AdaptiveType.LIGHT
            }
        }
    }

    /**
     * Rotates the adaptive wallpaper. It rotates by destination gotten from WallpaperPreferences.
     * Also saves the bitmap hash code to make category check mark correct.
     *
     * @return false if rotates failed.
     */
    fun rotateNextAdaptiveWallpaper(context: Context, nextAdaptiveType: AdaptiveType): Boolean {
        val bitmap = getAdaptiveWallpaperImage(context, nextAdaptiveType) ?: return false
        val bitmapHash = BitmapUtils.generateHashCode(bitmap)
        val injector: Injector = InjectorProvider.getInjector()
        val wallpaperStatusChecker = injector.wallpaperStatusChecker
        val isLockWallpaperSet = wallpaperStatusChecker.isLockWallpaperSet(context)
        val destination = if (isLockWallpaperSet) DEST_HOME_SCREEN else DEST_BOTH
        val wallpaperPreferences: WallpaperPreferences = injector.getPreferences(context)
        val appliedAdaptiveWallpaperId = wallpaperPreferences.appliedAdaptiveWallpaperId
        val whichWallpaper: Int

        // Saves bitmap hash to make sure the check mark in individual fragment position is correct.
        when (destination) {
            DEST_HOME_SCREEN -> {
                whichWallpaper = WallpaperManagerCompat.FLAG_SYSTEM
                wallpaperPreferences.homeWallpaperHashCode = bitmapHash
            }
            else -> { // DEST_BOTH
                whichWallpaper = (WallpaperManagerCompat.FLAG_SYSTEM
                        or WallpaperManagerCompat.FLAG_LOCK)
                wallpaperPreferences.homeWallpaperHashCode = bitmapHash
                wallpaperPreferences.lockWallpaperHashCode = bitmapHash
            }
        }

        if (!TextUtils.isEmpty(appliedAdaptiveWallpaperId)) {
            wallpaperPreferences.changeRecentHomeWallpaperImage(
                appliedAdaptiveWallpaperId,
                bitmap
            )
        }

        wallpaperPreferences.appliedAdaptiveType = nextAdaptiveType
        return InjectorProvider.getInjector().getWallpaperPersister(context)
            .setBitmapToWallpaperManagerCompat(bitmap, false, whichWallpaper) != 0
    }

    /**
     * Gets the next rotate adaptive wallpaper time by AdaptiveType calculated by
     * CalendarAstronomer with location.
     */
    @JvmStatic
    fun getNextRotateAdaptiveWallpaperTimeByAdaptiveType(
        location: Location?,
        nextAdaptiveType: AdaptiveType
    ): Long {
        val timeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return if (location == null) {
            if (nextAdaptiveType === AdaptiveType.LIGHT) {
                calendar[Calendar.HOUR_OF_DAY] = 6
            } else {
                calendar[Calendar.HOUR_OF_DAY] = 18
            }
            if (timeMillis > calendar.timeInMillis) {
                calendar[Calendar.DATE] = calendar[Calendar.DATE] + 1
            }
            calendar.timeInMillis
        } else {
            val calendarAstronomer = CalendarAstronomer(
                location.longitude,
                location.latitude
            )
            calendarAstronomer.time = timeMillis
            var nextRotateTime = getTimeMillisByAdaptiveType(calendarAstronomer, nextAdaptiveType)
            if (timeMillis > nextRotateTime) {
                calendarAstronomer.time =
                    timeMillis + TimeUnit.DAYS.toMillis(/* duration= */ 1)
                nextRotateTime = getTimeMillisByAdaptiveType(calendarAstronomer, nextAdaptiveType)
            }
            nextRotateTime
        }
    }

    private fun getTimeMillisByAdaptiveType(
        calendarAstronomer: CalendarAstronomer,
        adaptiveType: AdaptiveType
    ) = when (adaptiveType) {
        AdaptiveType.LIGHT -> calendarAstronomer.getSunRiseSet(/* rise= */ true)
        AdaptiveType.DARK -> calendarAstronomer.getSunRiseSet(/* rise= */ false)
        else -> calendarAstronomer.time
    }

    /**
     * Gets the device location.
     */
    @JvmStatic
    fun getLocation(context: Context): Location? {
        return if (PermissionUtils.isAccessCoarseLocationPermissionGranted(context)) {
            val locationManager = context.getSystemService(LocationManager::class.java)
            locationManager.lastLocation
        } else {
            null
        }
    }

    /**
     * Gets adaptive wallpaper image.
     */
    private fun getAdaptiveWallpaperImage(context: Context, adaptiveType: AdaptiveType): Bitmap? {
        try {
            return BitmapFactory.decodeStream(
                context.openFileInput(getAdaptiveWallpaperFilename(adaptiveType))
            )
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Failed to get adaptive wallpaper image.", e)
        }
        return null
    }
}
