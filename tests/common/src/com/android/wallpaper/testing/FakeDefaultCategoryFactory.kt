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

import android.content.Context
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import com.android.wallpaper.model.Category
import com.android.wallpaper.model.ImageCategory
import com.android.wallpaper.model.ThirdPartyAppCategory
import com.android.wallpaper.model.WallpaperCategory
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CollectionCategoryData
import com.android.wallpaper.picker.data.category.CommonCategoryData
import com.android.wallpaper.picker.data.category.ImageCategoryData
import com.android.wallpaper.picker.data.category.ThirdPartyCategoryData
import com.android.wallpaper.util.converter.category.CategoryFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDefaultCategoryFactory @Inject constructor() : CategoryFactory {

    private val wallpaperModels = mutableListOf<WallpaperModel>()
    private var overlayIconDrawable: Drawable? = null
    private var resolveInfo: ResolveInfo? = null

    fun setWallpaperModels(wallpaperModels: List<WallpaperModel>) {
        this.wallpaperModels.clear()
        this.wallpaperModels.addAll(wallpaperModels)
    }

    fun setOverlayIconDrawable(drawable: Drawable?) {
        overlayIconDrawable = drawable
    }

    fun setResolveInfo(resolveInfo: ResolveInfo?) {
        this.resolveInfo = resolveInfo
    }

    override fun getCategoryModel(context: Context, category: Category): CategoryModel {
        return CategoryModel(
            commonCategoryData = createCommonCategoryData(category),
            collectionCategoryData = createCollectionsCategoryData(category),
            imageCategoryData = createImageCategoryData(category),
            thirdPartyCategoryData = createThirdPartyCategoryData(category)
        )
    }

    private fun createCommonCategoryData(category: Category): CommonCategoryData {
        return CommonCategoryData(
            title = category.title,
            collectionId = category.collectionId,
            priority = category.priority
        )
    }

    private fun createCollectionsCategoryData(
        category: Category,
    ): CollectionCategoryData? {
        return if (category is WallpaperCategory) {
            CollectionCategoryData(
                wallpaperModels = wallpaperModels,
                thumbAsset = fakeAsset,
                featuredThumbnailIndex = category.featuredThumbnailIndex,
                isSingleWallpaperCategory = category.isSingleWallpaperCategory
            )
        } else {
            null
        }
    }

    private fun createImageCategoryData(category: Category): ImageCategoryData? {
        return if (category is ImageCategory) {
            ImageCategoryData(overlayIconDrawable = overlayIconDrawable)
        } else {
            null
        }
    }

    private fun createThirdPartyCategoryData(category: Category): ThirdPartyCategoryData? {
        return if (category is ThirdPartyAppCategory) {
            resolveInfo?.let { ThirdPartyCategoryData(resolveInfo = it) }
        } else {
            null
        }
    }

    companion object {
        val fakeAsset = TestAsset(TestStaticWallpaperInfo.COLOR_DEFAULT, false)
    }
}
