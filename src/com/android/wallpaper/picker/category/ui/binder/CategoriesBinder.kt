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

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel
import kotlinx.coroutines.launch

/** Binds the wallpaper categories and its meta data to the category screen */
object CategoriesBinder {

    fun bind(
        categoriesPage: View,
        viewModel: CategoriesViewModel,
        windowWidth: Int,
        lifecycleOwner: LifecycleOwner,
    ) {
        // instantiate the grid and assign its adapter and layout configuration
        val sectionsListView = categoriesPage.requireViewById<RecyclerView>(R.id.category_grid)
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // bind the state for List<SectionsViewModel>
                launch {
                    viewModel.sections.collect { sections ->
                        SectionsBinder.bind(sectionsListView, sections, windowWidth, lifecycleOwner)
                    }
                }
            }
        }
    }
}
