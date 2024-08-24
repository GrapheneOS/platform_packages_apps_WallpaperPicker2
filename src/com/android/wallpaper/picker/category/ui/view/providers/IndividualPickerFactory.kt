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

package com.android.wallpaper.picker.category.ui.view.providers

import androidx.fragment.app.Fragment
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel

/**
 * This interface provides the signature to classes to provide the correct IndividualPickerFragment
 */
interface IndividualPickerFactory {
    fun getIndividualPickerInstance(collectionId: String): Fragment

    fun getIndividualPickerInstance(
        collectionId: String,
        categoryType: CategoriesViewModel.CategoryType
    ): Fragment
}
