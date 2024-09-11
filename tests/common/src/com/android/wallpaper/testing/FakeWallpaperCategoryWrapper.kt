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

package com.android.wallpaper.testing

import com.android.wallpaper.model.Category
import com.android.wallpaper.picker.category.wrapper.WallpaperCategoryWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeWallpaperCategoryWrapper @Inject constructor() : WallpaperCategoryWrapper {
    override suspend fun getCategories(
        forceRefreshLiveWallpaperCategories: Boolean
    ): List<Category> {
        TODO("Not yet implemented")
    }

    override fun getCategory(
        categories: List<Category>,
        collectionId: String,
        forceRefreshLiveWallpaperCategories: Boolean,
    ): Category? {
        TODO("Not yet implemented")
    }

    override suspend fun refreshLiveWallpaperCategories() {
        TODO("Not yet implemented")
    }
}
