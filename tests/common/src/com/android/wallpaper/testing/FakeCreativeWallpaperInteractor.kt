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

import com.android.wallpaper.picker.category.domain.interactor.CreativeCategoryInteractor
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CommonCategoryData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** This class implements the business logic in assembling creative category models */
@Singleton
class FakeCreativeWallpaperInteractor @Inject constructor() : CreativeCategoryInteractor {
    override val categories: Flow<List<CategoryModel>> = flow {
        // stubbing the list of single section categories
        val categoryModels =
            generateCategoryData().map { commonCategoryData ->
                CategoryModel(
                    commonCategoryData,
                    null,
                    null,
                    null,
                )
            }

        // Emit the list of categories
        emit(categoryModels)
    }

    override fun updateCreativeCategories() {
        // empty
    }

    private fun generateCategoryData(): List<CommonCategoryData> {
        val dataList =
            listOf(
                CommonCategoryData("Emoji", "emoji_wallpapers", 1),
                CommonCategoryData("A.I.", "ai_wallpapers", 2),
            )
        return dataList
    }
}
