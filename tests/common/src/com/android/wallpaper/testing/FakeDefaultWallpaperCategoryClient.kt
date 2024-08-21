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
import com.android.wallpaper.model.ImageCategory
import com.android.wallpaper.picker.category.client.DefaultWallpaperCategoryClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDefaultWallpaperCategoryClient @Inject constructor() : DefaultWallpaperCategoryClient {

    private var fakeSystemCategories: List<Category> = emptyList()
    private var fakeOnDeviceCategory: Category? = null
    private var fakeThirdPartyAppCategories: List<Category> = emptyList()
    private var fakeThirdPartyLiveWallpaperCategories: List<Category> = emptyList()

    fun setOnDeviceCategory(category: Category?) {
        fakeOnDeviceCategory = category
    }

    fun setThirdPartyLiveWallpaperCategories(categories: List<Category>) {
        fakeThirdPartyLiveWallpaperCategories = categories
    }

    fun setSystemCategories(categories: List<Category>) {
        fakeSystemCategories = categories
    }

    fun setThirdPartyAppCategories(categories: List<Category>) {
        fakeThirdPartyAppCategories = categories
    }

    override suspend fun getMyPhotosCategory(): Category {
        return ImageCategory(
            "Fake My Photos",
            "fake_my_photos_id",
            1,
            0 // Placeholder resource ID
        )
    }

    override suspend fun getSystemCategories(): List<Category> {
        return fakeSystemCategories
    }

    override suspend fun getOnDeviceCategory(): Category? {
        return fakeOnDeviceCategory
    }

    override suspend fun getThirdPartyCategory(excludedPackageNames: List<String>): List<Category> {
        TODO("Not yet implemented")
    }

    override fun getExcludedThirdPartyPackageNames(): List<String> {
        TODO("Not yet implemented")
    }

    suspend fun getThirdPartyCategory(): List<Category> {
        return fakeThirdPartyAppCategories
    }

    override suspend fun getThirdPartyLiveWallpaperCategory(
        excludedPackageNames: Set<String>
    ): List<Category> {
        return fakeThirdPartyLiveWallpaperCategories
    }

    override fun getExcludedLiveWallpaperPackageNames(): Set<String> {
        TODO("Not yet implemented")
    }
}
