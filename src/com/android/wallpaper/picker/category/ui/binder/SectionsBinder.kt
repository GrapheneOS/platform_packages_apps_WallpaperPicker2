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

package com.android.wallpaper.picker.category.ui.binder

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.categorypicker.viewmodel.SectionViewModel
import com.android.wallpaper.picker.category.ui.view.adapter.CategorySectionsAdapter

/** Binds the collection of SectionViewModel to a section */
object SectionsBinder {
    private const val FULL_GRID_SPAN = 3
    private const val SINGLE_COLUMN_SPAN = 1

    fun bind(
        sectionsListView: RecyclerView,
        sectionsViewModel:
            List<SectionViewModel>, // TODO: this should not be a list rather a simple view model
        displayDensity: Float,
        lifecycleOwner: LifecycleOwner,
    ) {
        sectionsListView.adapter = CategorySectionsAdapter(sectionsViewModel, displayDensity)

        val gridLayoutManager =
            GridLayoutManager(sectionsListView.context, 3).apply { // 3 columns by default
                spanSizeLookup =
                    object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            // TODO: this will be updated to use SectionViewModel.columnCount
                            return if (sectionsViewModel[position].items.size > 1) {
                                FULL_GRID_SPAN // Item occupies all columns (full width)
                            } else {
                                SINGLE_COLUMN_SPAN // Item occupies a single column
                            }
                        }
                    }
            }
        sectionsListView.layoutManager = gridLayoutManager
    }
}
