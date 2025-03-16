/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License.O
 */
package com.android.wallpaper.testing

import com.android.wallpaper.model.WallpaperMetadata
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.WallpaperRefresher
import com.android.wallpaper.module.WallpaperRefresher.RefreshListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test implementation of [WallpaperRefresher] which simply provides whatever metadata is saved in
 * WallpaperPreferences and the image wallpaper set to [WallpaperManager].
 */
@Singleton
class FakeWallpaperRefresher @Inject constructor(private val prefs: WallpaperPreferences) :
    WallpaperRefresher {

    override fun refresh(listener: RefreshListener) {
        if (prefs.getLockWallpaperManagerId() > 0) {
            listener.onRefreshed(
                WallpaperMetadata(
                    prefs.getHomeWallpaperAttributions(),
                    prefs.getHomeWallpaperActionUrl(),
                    prefs.getHomeWallpaperCollectionId(), /* wallpaperComponent */
                    null,
                    /* cropHints= */ null,
                    /* imageUri= */ null,
                ),
                WallpaperMetadata(
                    prefs.getLockWallpaperAttributions(),
                    prefs.getLockWallpaperActionUrl(),
                    prefs.getLockWallpaperCollectionId(), /* wallpaperComponent */
                    null,
                    /* cropHints= */ null,
                    /* imageUri= */ null,
                ),
                prefs.getWallpaperPresentationMode(),
            )
        } else {
            listener.onRefreshed(
                WallpaperMetadata(
                    prefs.getHomeWallpaperAttributions(),
                    prefs.getHomeWallpaperActionUrl(),
                    prefs.getHomeWallpaperCollectionId(), /* wallpaperComponent */
                    null,
                    /* cropHints= */ null,
                    /* imageUri= */ null,
                ),
                null,
                prefs.getWallpaperPresentationMode(),
            )
        }
    }
}
