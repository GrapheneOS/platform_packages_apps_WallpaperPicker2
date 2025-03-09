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

package com.android.wallpaper.picker.category.domain.interactor.implementations

import com.android.wallpaper.picker.category.data.repository.WallpaperCategoryRepository
import com.android.wallpaper.picker.category.domain.interactor.OnDeviceWallpapersInteractor
import com.android.wallpaper.picker.data.WallpaperModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest

/**
 * This class provides a [Flow] which emits on device wallpapers for WallpaperPicker2. This includes
 * both on-device and system categories.
 */
@Singleton
class DefaultOnDeviceWallpapersInteractor
@Inject
constructor(private val defaultWallpaperCategoryRepository: WallpaperCategoryRepository) :
    OnDeviceWallpapersInteractor {

    override val defaultWallpapers: Flow<List<WallpaperModel>> =
        defaultWallpaperCategoryRepository.isDefaultCategoriesFetched
            .filter { it }
            .flatMapLatest {
                combine(
                    defaultWallpaperCategoryRepository.onDeviceCategory,
                    defaultWallpaperCategoryRepository.systemCategories,
                ) { onDeviceCategory, systemCategories ->
                    val finalList =
                        onDeviceCategory?.let { systemCategories + it } ?: systemCategories
                    val cmf =
                        finalList.flatMap { categoryModel ->
                            categoryModel?.collectionCategoryData?.wallpaperModels ?: emptyList()
                        }
                    return@combine cmf
                }
            }
}
