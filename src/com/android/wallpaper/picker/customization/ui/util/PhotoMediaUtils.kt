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

package com.android.wallpaper.picker.customization.ui.util

import android.net.Uri
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.category.PhotoCategoryModel

/** This object provides util methods to compare sets of images */
object PhotoMediaUtils {

    private fun extractMediaKey(uri: Uri): String? {
        val path = uri.encodedPath ?: return null
        val segments = path.split('/')
        return segments.find { it.contains("mediakey") }?.substringAfter("mediakey%3A%2Flocal%253A")
    }

    /**
     * Returns a lambda suitable for use with Flow's distinctUntilChanged operator to determine
     * whether two {@link PhotoCategoryModel} instances have the same set of media keys.
     *
     * <p>The comparison is done by:
     * <ul>
     * <li>Extracting URIs from {@link WallpaperModel.StaticWallpaperModel} instances inside the
     *   {@link PhotoCategoryModel}'s collectionCategoryData.wallpaperModels list.</li>
     * <li>Parsing out the encoded media key from each URI using {@link #extractMediaKey(Uri)}.</li>
     * <li>Comparing the resulting sets of media keys for equality.</li>
     * </ul>
     *
     * <p>This allows detection of meaningful changes in wallpaper content without being affected by
     * minor or irrelevant differences (e.g., timestamps in the URI).
     *
     * @return a lambda that returns true if the old and new models have the same set of media keys,
     *   false otherwise.
     */
    fun distinctMediaKeyChanged(): (PhotoCategoryModel, PhotoCategoryModel) -> Boolean {
        return { old, new ->
            val oldKeys =
                old.categoryModel.collectionCategoryData
                    ?.wallpaperModels
                    ?.mapNotNull {
                        val static = it as? WallpaperModel.StaticWallpaperModel
                        static?.imageWallpaperData?.uri?.let { uri -> extractMediaKey(uri) }
                    }
                    ?.toSet()
                    .orEmpty()

            val newKeys =
                new.categoryModel.collectionCategoryData
                    ?.wallpaperModels
                    ?.mapNotNull {
                        val static = it as? WallpaperModel.StaticWallpaperModel
                        static?.imageWallpaperData?.uri?.let { uri -> extractMediaKey(uri) }
                    }
                    ?.toSet()
                    .orEmpty()

            oldKeys == newKeys
        }
    }
}
