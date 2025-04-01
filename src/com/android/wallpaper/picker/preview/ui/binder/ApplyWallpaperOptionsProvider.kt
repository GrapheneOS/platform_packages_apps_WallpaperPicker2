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

package com.android.wallpaper.picker.preview.ui.binder

import android.app.wallpaper.WallpaperDescription
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination

/** Defines what options the wallpaper can have to customize the apply wallpaper screen. */
interface ApplyWallpaperOptionsProvider {

    /**
     * Gets the recommended [WallpaperDestination] by the wallpaper from its [WallpaperDescription].
     */
    fun getSuggestedWallpaperDestination(description: WallpaperDescription): WallpaperDestination?

    /**
     * Gets the reason for the recommended [WallpaperDestination] by the wallpaper from its
     * [WallpaperDescription].
     */
    fun getSuggestedWallpaperDestinationReason(description: WallpaperDescription): String?
}
