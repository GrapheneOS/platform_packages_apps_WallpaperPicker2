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

package com.android.wallpaper.picker.preview.ui.binder

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.preview.ui.view.PreviewTabs
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.launch

object TabsBinder {
    fun bind(
        tabs: PreviewTabs,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        viewLifecycleOwner: LifecycleOwner,
    ) {
        tabs.setOnTabSelected { wallpaperPreviewViewModel.setSmallPreviewSelectedTabIndex(it) }

        val texts =
            wallpaperPreviewViewModel.smallPreviewTabs.map {
                when (it) {
                    Screen.LOCK_SCREEN -> tabs.context.resources.getString(R.string.lock_screen_tab)
                    Screen.HOME_SCREEN -> tabs.context.resources.getString(R.string.home_screen_tab)
                }
            }

        tabs.setTabsText(texts[0], texts[1])

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Set initial tab to make sure tabs start at the correct state.
                tabs.setTab(wallpaperPreviewViewModel.getSmallPreviewTabIndex())
                wallpaperPreviewViewModel.smallPreviewSelectedTabIndex.collect {
                    tabs.transitionToTab(it)
                }
            }
        }
    }
}
