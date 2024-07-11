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

package com.android.wallpaper.picker.category.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.category.ui.binder.CategoriesBinder
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel
import com.android.wallpaper.util.SizeCalculator
import dagger.hilt.android.AndroidEntryPoint

/** This fragment displays the user interface for the categories */
@AndroidEntryPoint(AppbarFragment::class)
class CategoriesFragment : Hilt_CategoriesFragment() {

    // TODO: this may need to be scoped to fragment if the architecture changes
    private val categoriesViewModel by activityViewModels<CategoriesViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view =
            inflater.inflate(R.layout.categories_fragment, container, /* attachToRoot= */ false)

        CategoriesBinder.bind(
            categoriesPage = view.requireViewById<RecyclerView>(R.id.content_parent),
            viewModel = categoriesViewModel,
            SizeCalculator.getActivityWindowWidthPx(this.activity),
            lifecycleOwner = viewLifecycleOwner,
        ) { _ ->
            Toast.makeText(this.context, "Navigate to wallpapers ", Toast.LENGTH_SHORT).show()
        }
        return view
    }
}
