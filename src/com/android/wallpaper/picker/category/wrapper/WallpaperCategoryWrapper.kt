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

package com.android.wallpaper.picker.category.wrapper

import com.android.wallpaper.model.Category

/**
 * Temporary wrapper to maintain compatibility with legacy code. It prevents redundant category data
 * fetches by reusing data fetched via the recommended architecture.
 */
interface WallpaperCategoryWrapper {

    /**
     * This function is used to get categories that have already been fetched. The
     * forceRefreshLiveWallpapers flag is used to decide whether we should re-fetch live wallpaper
     * categories or not.
     */
    suspend fun getCategories(forceRefreshLiveWallpaperCategories: Boolean): List<Category>

    /**
     * This function is used to get a single particular category out of all the fetched categories.
     * It also accepts forceRefreshLiveWallpapers flag in case the category has been updated.
     */
    fun getCategory(
        categories: List<Category>,
        collectionId: String,
        forceRefreshLiveWallpaperCategories: Boolean,
    ): Category?

    /** This function is used to trigger re-fetching live wallpaper categories. */
    suspend fun refreshLiveWallpaperCategories()
}
