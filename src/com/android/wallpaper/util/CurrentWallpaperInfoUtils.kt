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

package com.android.wallpaper.util

import android.content.Context
import android.net.Uri
import com.android.wallpaper.model.CurrentWallpaperInfo
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.InjectorProvider
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Utils for [CurrentWallpaperInfo]. */
object CurrentWallpaperInfoUtils {

    /**
     * Determines WallpaperInfo objects representing the currently set wallpaper(s), retrieving the
     * wallpaper id from local metadata if necessary. Updates the current recents key(s) and returns
     * the WallpaperInfo pair, one for the lock screen and one for the home screen.
     */
    suspend fun getCurrentWallpapers(
        context: Context,
        updateRecents: Boolean,
        forceRefresh: Boolean,
        onFetchUri: (CurrentWallpaperInfo, Screen) -> Uri?,
    ): Pair<WallpaperInfo, WallpaperInfo> = suspendCoroutine { continuation ->
        val injector = InjectorProvider.getInjector()
        val currentWallpaperFactory = injector.getCurrentWallpaperInfoFactory(context)
        currentWallpaperFactory.createCurrentWallpaperInfos(context, forceRefresh) {
            homeWallpaper,
            lockWallpaper,
            _ ->
            val preferences = injector.getPreferences(context)
            val hw =
                if (homeWallpaper is CurrentWallpaperInfo) {
                    homeWallpaper.augmentByRecent(
                        context,
                        Screen.HOME_SCREEN,
                        preferences.getHomeWallpaperRecentsKey(),
                        updateRecents,
                        onFetchUri.invoke(homeWallpaper, Screen.HOME_SCREEN),
                    )
                } else {
                    homeWallpaper
                }
            val lw =
                when (lockWallpaper) {
                    null -> {
                        hw
                    }
                    is CurrentWallpaperInfo -> {
                        lockWallpaper.augmentByRecent(
                            context,
                            Screen.LOCK_SCREEN,
                            preferences.getLockWallpaperRecentsKey(),
                            updateRecents,
                            onFetchUri.invoke(lockWallpaper, Screen.LOCK_SCREEN),
                        )
                    }
                    else -> {
                        lockWallpaper
                    }
                }

            if (updateRecents) {
                preferences.setHomeWallpaperRecentsKey(hw.wallpaperId)
                preferences.setLockWallpaperRecentsKey(lw.wallpaperId)
            }

            continuation.resume(Pair(hw, lw))
        }
    }

    /** Augments the current wallpaper info by its recent wallpaper data. */
    private fun CurrentWallpaperInfo.augmentByRecent(
        context: Context,
        screen: Screen,
        recentKey: String?,
        updateRecents: Boolean,
        sharableUri: Uri?,
    ): CurrentWallpaperInfo {
        val cropHints =
            if (InjectorProvider.getInjector().getFlags().isMultiCropEnabled()) {
                wallpaperCropHints
            } else {
                null
            }

        return object :
                CurrentWallpaperInfo(
                    getAttributions(context),
                    getActionUrl(context),
                    getCollectionId(context),
                    screen.toFlag(),
                    sharableUri,
                ) {
                override fun getWallpaperId(): String {
                    if (!updateRecents) {
                        return super.getWallpaperId()
                    }

                    return recentKey ?: super.getWallpaperId()
                }
            }
            .apply { wallpaperCropHints = cropHints }
    }
}
