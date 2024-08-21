/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.wallpaper.picker.category.client

import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import com.android.wallpaper.model.WallpaperInfo

/**
 * This class is used for handling all operations related to live wallpapers. This is meant to
 * contain all methods/functions that LiveWallpaperInfo class currently holds.
 */
interface LiveWallpapersClient {

    /**
     * Retrieves a list of all installed live wallpapers on the device,
     * excluding those whose package names are specified in the provided set.
     */
    fun getAll(excludedPackageNames: Set<String?>?): List<WallpaperInfo>
}