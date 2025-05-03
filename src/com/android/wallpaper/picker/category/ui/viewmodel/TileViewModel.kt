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

package com.android.wallpaper.picker.category.ui.viewmodel

import android.graphics.drawable.Drawable
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.picker.category.ui.view.SectionCardinality

/** This class represents the view model for a single category tile. */
class TileViewModel(
    /** This is the fallback drawable in the case thumbnailAsset is null */
    val defaultDrawable: Drawable?,
    /** This is the primary image source for the tile */
    val thumbnailAsset: Asset?,
    val text: String,
    val showTitle: Boolean = true,
    val maxCategoriesInRow: SectionCardinality = SectionCardinality.Single,
    val contentDescription: String? = null,
    val onClicked: (() -> Unit)? = null,
)
