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

import com.android.wallpaper.picker.category.data.repository.WallpaperCategoryRepository
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CommonCategoryData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class FakeDefaultWallpaperCategoryRepository @Inject constructor() : WallpaperCategoryRepository {

    private val _myPhotosCategory = MutableStateFlow<CategoryModel?>(null)
    override val myPhotosCategory: StateFlow<CategoryModel?> = _myPhotosCategory

    override val systemCategories: StateFlow<List<CategoryModel>>
        get() = TODO("Not yet implemented")

    override val onDeviceCategory: StateFlow<CategoryModel?>
        get() = TODO("Not yet implemented")

    override val isDefaultCategoriesFetched: StateFlow<Boolean>
        get() = TODO("Not yet implemented")

    override suspend fun fetchMyPhotosCategory() {
        _myPhotosCategory.value =
            CategoryModel(
                commonCategoryData = CommonCategoryData("Fake My Photos", "fake_my_photos_id", 1),
                thirdPartyCategoryData = null,
                imageCategoryData = null,
                collectionCategoryData = null
            )
    }
}
