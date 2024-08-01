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

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import com.android.wallpaper.picker.category.domain.interactor.ThirdPartyCategoryInteractor
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CommonCategoryData
import com.android.wallpaper.picker.data.category.ThirdPartyCategoryData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class FakeThirdPartyCategoryInteractor @Inject constructor() : ThirdPartyCategoryInteractor {
    override val categories: Flow<List<CategoryModel>> = flow {
        // stubbing the list of single section categories
        val categoryModels =
            generateCategoryData().map { pair ->
                CategoryModel(
                    pair.first,
                    pair.second,
                    null,
                    null,
                )
            }

        // Emit the list of categories
        emit(categoryModels)
    }

    private fun generateCategoryData(): List<Pair<CommonCategoryData, ThirdPartyCategoryData>> {
        val biktokResolveInfo = ResolveInfo()
        val biktokComponentName =
            ComponentName("com.zhiliaoapp.musically", "com.ss.android.ugc.aweme.main.MainActivity")

        biktokResolveInfo.activityInfo =
            ActivityInfo().apply {
                packageName = biktokComponentName.packageName
                name = biktokComponentName.className
            }

        val binstragramResolveInfo = ResolveInfo()
        val binstagramComponentName =
            ComponentName("com.instagram.android", "com.instagram.mainactivity.MainActivity")

        binstragramResolveInfo.activityInfo =
            ActivityInfo().apply {
                packageName = binstagramComponentName.packageName
                name = binstagramComponentName.className
            }

        val dataList =
            listOf(
                Pair(
                    CommonCategoryData("Biktok", "biktok", 1),
                    ThirdPartyCategoryData(biktokResolveInfo)
                ),
                Pair(
                    CommonCategoryData("Binstagram", "binstagram", 2),
                    ThirdPartyCategoryData(binstragramResolveInfo)
                ),
            )
        return dataList
    }
}
