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

package com.android.wallpaper.picker.category.domain.interactor.implementations

import com.android.wallpaper.picker.category.domain.interactor.MyPhotosInteractor
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CommonCategoryData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** This class implements the business logic in assembling my photos category model */
@Singleton
class MyPhotosInteractorImpl @Inject constructor() : MyPhotosInteractor {
    override val category: Flow<CategoryModel> = flow {
        // TODO: to provide concrete implementation
        emit(
            CategoryModel(
                CommonCategoryData("", "", 1),
                /* previewImage= */ null,
                /* previewImageThumbnail= */ null,
                /* previewImageThumbnailTransformation= */ null,
            )
        )
    }

    override fun updateMyPhotos() {
        // TODO: call repo to update my photos category
    }
}
