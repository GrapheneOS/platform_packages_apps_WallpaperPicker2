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

import com.android.wallpaper.picker.category.domain.interactor.CategoryInteractor
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CommonCategoryData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** This class implements the business logic in assembling ungrouped category models */
@Singleton
class FakeCategoryInteractor @Inject constructor() : CategoryInteractor {
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

    override fun refreshNetworkCategories() {
        // empty
    }

    private fun generateCategoryData(): List<CommonCategoryData> {
        val dataList =
            listOf(
                CommonCategoryData("Celestial Dreamscape", "celestial_dreamscapes", 1),
                CommonCategoryData("Geometric Fusion", "geometric_fusion", 2),
                CommonCategoryData("Neon Metropolis", "neon_metropolis", 3),
                CommonCategoryData("Whispering Wilderness", "whispering_wilderness", 4),
                CommonCategoryData("Retro Wave", "retro_wave", 5),
                CommonCategoryData("Abstract Expressionism", "abstract_expressionism", 6),
                CommonCategoryData("Cyberpunk Cityscape", "cyberpunk_cityscape", 7),
                CommonCategoryData("Floral Fantasy", "floral_fantasy", 8),
                CommonCategoryData("Cosmic Nebula", "cosmic_nebula", 9),
                CommonCategoryData("Minimalist Lines", "minimalist_lines", 10),
                CommonCategoryData("Watercolor Wonder", "watercolor_wonder", 11),
                CommonCategoryData("Urban Graffiti", "urban_graffiti", 12),
                CommonCategoryData("Vintage Botanicals", "vintage_botanicals", 13),
                CommonCategoryData("Pastel Dreams", "pastel_dreams", 14),
                CommonCategoryData("Polygonal Paradise", "polygonal_paradise", 15),
                CommonCategoryData("Oceanic Depths", "oceanic_depths", 16),
                CommonCategoryData("Fractal Fantasia", "fractal_fantasia", 17)
            )
        return dataList
    }
}
