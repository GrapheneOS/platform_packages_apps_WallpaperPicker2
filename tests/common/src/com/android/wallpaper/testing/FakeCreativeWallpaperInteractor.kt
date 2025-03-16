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
import kotlinx.coroutines.flow.MutableStateFlow

/** This class implements the business logic in assembling creative category models */
@Singleton
class FakeCreativeWallpaperInteractor @Inject constructor() : CreativeCategoryInteractor {
    private val _categories = MutableStateFlow(dataListLessThanMinimum)

    override val categories: Flow<List<CategoryModel>>
        get() = _categories

    fun setCreativeCategories(newCategories: List<CategoryModel>) {
        _categories.value = newCategories
    }

    override fun updateCreativeCategories() {
        // empty
    }

    companion object {
        val dataListLessThanMinimum =
            listOf(
                    CommonCategoryData("Emoji", "emoji_wallpapers", 1),
                    CommonCategoryData("A.I.", "ai_wallpapers", 2),
                )
                .map { commonCategoryData -> CategoryModel(commonCategoryData, null, null, null) }

        val dataListMinumumOrMore =
            listOf(
                    CommonCategoryData("Emoji", "emoji_wallpapers", 1),
                    CommonCategoryData("A.I.", "ai_wallpapers", 2),
                    CommonCategoryData("Magic Portrait", "magic_portrait", 2),
                )
                .map { commonCategoryData -> CategoryModel(commonCategoryData, null, null, null) }
    }
}
