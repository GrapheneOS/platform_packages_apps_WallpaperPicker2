/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 */

package com.android.wallpaper.picker.customization.ui.section

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import com.android.wallpaper.R
import com.android.wallpaper.model.CustomizationSectionController
import com.android.wallpaper.model.CustomizationSectionController.CustomizationSectionNavigationController
import com.android.wallpaper.picker.CategorySelectorFragment
import com.android.wallpaper.picker.customization.domain.interactor.WallpaperInteractor
import com.android.wallpaper.picker.customization.ui.binder.WallpaperQuickSwitchSectionBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperQuickSwitchViewModel
import kotlinx.coroutines.CoroutineScope

/** Controls a section that lets the user switch wallpapers quickly. */
class WallpaperQuickSwitchSectionController(
    interactor: WallpaperInteractor,
    private val lifecycleOwner: LifecycleOwner,
    scope: CoroutineScope,
    private val navigationController: CustomizationSectionNavigationController,
) : CustomizationSectionController<WallpaperQuickSwitchView> {

    private val viewModel =
        WallpaperQuickSwitchViewModel(
            interactor = interactor,
            maxOptions = MAX_OPTIONS,
            onNavigateToFullWallpaperSelector = {
                navigationController.navigateTo(
                    CategorySelectorFragment(),
                )
            },
            scope = scope,
        )

    override fun isAvailable(context: Context?): Boolean {
        return true
    }

    @SuppressLint("InflateParams") // We don't care that the parent is null.
    override fun createView(context: Context?): WallpaperQuickSwitchView {
        val view =
            LayoutInflater.from(context)
                .inflate(
                    R.layout.wallpaper_quick_switch_section,
                    /* parent= */ null,
                ) as WallpaperQuickSwitchView
        WallpaperQuickSwitchSectionBinder.bind(
            view = view,
            viewModel = viewModel,
            lifecycleOwner = lifecycleOwner,
        )
        return view
    }

    companion object {
        /** The maximum number of options to show, including the currently-selected one. */
        private const val MAX_OPTIONS = 5
    }
}
