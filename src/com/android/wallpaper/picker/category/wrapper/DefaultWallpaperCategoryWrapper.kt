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
import com.android.wallpaper.picker.category.data.repository.WallpaperCategoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWallpaperCategoryWrapper
@Inject
constructor(private var defaultWallpaperCategoryRepository: WallpaperCategoryRepository) :
    WallpaperCategoryWrapper {

    private var categoryMap: Map<String, Category>? = null

    override suspend fun getCategories(
        forceRefreshLiveWallpaperCategories: Boolean
    ): List<Category> {
        val systemCategories = defaultWallpaperCategoryRepository.getSystemFetchedCategories()
        val thirdPartyCategory = defaultWallpaperCategoryRepository.getThirdPartyFetchedCategories()
        val myPhotosCategory = defaultWallpaperCategoryRepository.getMyPhotosFetchedCategory()
        val onDeviceCategory = defaultWallpaperCategoryRepository.getOnDeviceFetchedCategories()
        val thirdPartyLiveWallpaperFetchedCategory =
            defaultWallpaperCategoryRepository.getThirdPartyLiveWallpaperFetchedCategories()

        val onDeviceCategories = onDeviceCategory?.let { listOf(it) } ?: emptyList()
        val myPhotosCategories = myPhotosCategory?.let { listOf(it) } ?: emptyList()

        return myPhotosCategories +
            onDeviceCategories +
            thirdPartyCategory +
            systemCategories +
            thirdPartyLiveWallpaperFetchedCategory
    }

    override fun getCategory(
        categories: List<Category>,
        collectionId: String,
        forceRefreshLiveWallpaperCategories: Boolean,
    ): Category? {
        if (categoryMap == null) {
            categoryMap = categories.associateBy { it.collectionId }
        }
        return categoryMap?.get(collectionId)
    }

    override suspend fun refreshLiveWallpaperCategories() {
        TODO("Not yet implemented")
    }
}
