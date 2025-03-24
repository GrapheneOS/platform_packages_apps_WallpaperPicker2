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

package com.android.wallpaper.picker.category.ui.viewmodel

import android.app.PendingIntent
import com.android.wallpaper.picker.data.PhotosErrorData

/** This view model is specifically for the photos section. */
class PhotosViewModel(
    val isDismissed: Boolean,
    override val tileViewModels: List<TileViewModel>,
    // This pending intent initiates the Google Photos sign-in process.
    val pendingIntent: PendingIntent?,
    override val columnCount: Int,
    override val sectionTitle: String? = null,
    override val displayType: CategoriesViewModel.DisplayType =
        CategoriesViewModel.DisplayType.Default,
    override val status: PhotosErrorData? = null,
    override val onSectionClicked: (() -> Unit)? = null,
) :
    SectionViewModel(
        tileViewModels,
        columnCount,
        sectionTitle,
        displayType,
        status,
        onSectionClicked,
    )
