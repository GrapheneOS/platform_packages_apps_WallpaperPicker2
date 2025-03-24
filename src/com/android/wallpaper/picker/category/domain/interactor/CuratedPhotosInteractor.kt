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

package com.android.wallpaper.picker.category.domain.interactor

import com.android.wallpaper.picker.data.category.PhotoCategoryModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Classes that implement this interface implement the business logic in assembling curated photos
 * category model
 */
interface CuratedPhotosInteractor {
    val category: Flow<PhotoCategoryModel>
    val dismissBanner: StateFlow<Boolean>

    /**
     * This methods is responsible for setting the value of dismiss button which controls the
     * visibility of sign in banner.
     */
    fun setBannerDismissed(dismissed: Boolean)
}
