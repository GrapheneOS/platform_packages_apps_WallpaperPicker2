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

package com.android.wallpaper.picker.category.data.repository

import com.android.wallpaper.picker.data.category.CategoryModel
import kotlinx.coroutines.flow.StateFlow

/**
 * This is the common repository interface that is responsible for communicating with wallpaper
 * category data clients and also convert them to CategoryData classes.
 */
interface WallpaperCategoryRepository {
    val systemCategories: StateFlow<List<CategoryModel>>
    val myPhotosCategory: StateFlow<CategoryModel?>
    val onDeviceCategory: StateFlow<CategoryModel?>
    val thirdPartyAppCategory: StateFlow<List<CategoryModel>>
    val isDefaultCategoriesFetched: StateFlow<Boolean>

    suspend fun fetchMyPhotosCategory()
}
