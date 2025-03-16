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

package com.android.wallpaper.module

import android.net.Uri
import com.android.wallpaper.model.CurrentWallpaperInfo
import com.android.wallpaper.model.Screen

/** Manages the CRUD of recent wallpapers. */
interface RecentWallpaperManager {

    /** Gets the uri for current wallpaper of the [Screen]. */
    fun getCurrentWallpaperBitmapUri(wallpaperInfo: CurrentWallpaperInfo, screen: Screen): Uri?
}
