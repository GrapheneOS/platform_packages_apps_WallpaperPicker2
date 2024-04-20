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
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel

/** Binds the wallpaper categories and its meta data to the category screen */
object CategoriesBinder {

    fun bind(
        gridView: RecyclerView,
        viewModel: CategoriesViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {}
}
