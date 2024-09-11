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
import com.android.wallpaper.picker.category.data.repository.WallpaperCategoryRepository
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CommonCategoryData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeDefaultWallpaperCategoryRepository @Inject constructor() : WallpaperCategoryRepository {

    private val _myPhotosCategory = MutableStateFlow<CategoryModel?>(null)
    override val myPhotosCategory: StateFlow<CategoryModel?> = _myPhotosCategory

    override val systemCategories: StateFlow<List<CategoryModel>>
        get() = MutableStateFlow(emptyList())

    override val onDeviceCategory: StateFlow<CategoryModel?>
        get() =
            MutableStateFlow(
                CategoryModel(
                    commonCategoryData =
                        CommonCategoryData("On-device-category-1", "on_device_sample_id", 2),
                    thirdPartyCategoryData = null,
                    imageCategoryData = null,
                    collectionCategoryData = null,
                )
            )

    private val _isDefaultCategoriesFetched = MutableStateFlow(true)
    override val isDefaultCategoriesFetched: StateFlow<Boolean> =
        _isDefaultCategoriesFetched.asStateFlow()

    override fun getMyPhotosFetchedCategory(): Category {
        return ImageCategory("MyPhotos", "MyPhotosCollectionId", 4)
    }

    override fun getOnDeviceFetchedCategories(): Category? {
        return null
    }

    override fun getThirdPartyFetchedCategories(): List<Category> {
        return emptyList()
    }

    override fun getSystemFetchedCategories(): List<Category> {
        return emptyList()
    }

    override fun getThirdPartyLiveWallpaperFetchedCategories(): List<Category> {
        return emptyList()
    }

    override val thirdPartyAppCategory: StateFlow<List<CategoryModel>>
        get() =
            MutableStateFlow(
                listOf(
                    CategoryModel(
                        commonCategoryData = CommonCategoryData("ThirdParty-1", "on_device_id", 2),
                        thirdPartyCategoryData = null,
                        imageCategoryData = null,
                        collectionCategoryData = null,
                    ),
                    CategoryModel(
                        commonCategoryData = CommonCategoryData("ThirdParty-2", "downloads_id", 3),
                        thirdPartyCategoryData = null,
                        imageCategoryData = null,
                        collectionCategoryData = null,
                    ),
                    CategoryModel(
                        commonCategoryData =
                            CommonCategoryData("ThirdParty-3", "screenshots_id", 4),
                        thirdPartyCategoryData = null,
                        imageCategoryData = null,
                        collectionCategoryData = null,
                    ),
                )
            )

    override val thirdPartyLiveWallpaperCategory: StateFlow<List<CategoryModel>>
        get() =
            MutableStateFlow(
                listOf(
                    CategoryModel(
                        commonCategoryData =
                            CommonCategoryData("ThirdPartyLiveWallpaper-1", "on_device_live_id", 2),
                        thirdPartyCategoryData = null,
                        imageCategoryData = null,
                        collectionCategoryData = null,
                    )
                )
            )

    override suspend fun fetchMyPhotosCategory() {
        _myPhotosCategory.value =
            CategoryModel(
                commonCategoryData = CommonCategoryData("Fake My Photos", "fake_my_photos_id", 1),
                thirdPartyCategoryData = null,
                imageCategoryData = null,
                collectionCategoryData = null,
            )
    }

    override suspend fun refreshNetworkCategories() {
        // empty
    }
}
