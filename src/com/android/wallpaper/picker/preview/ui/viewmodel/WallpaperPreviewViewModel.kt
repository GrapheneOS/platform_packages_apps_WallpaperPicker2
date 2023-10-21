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
 */
package com.android.wallpaper.picker.preview.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.model.wallpaper.WallpaperModel
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Top level [ViewModel] for [WallpaperPreviewActivity] and its fragments */
@HiltViewModel
class WallpaperPreviewViewModel
@Inject
constructor(
    private val staticWallpaperPreviewViewModel: StaticWallpaperPreviewViewModel,
) : ViewModel() {
    /**
     * User selected [WallpaperInfo] for editing.
     *
     * TODO(b/303457890): using WallpaperModel instead.
     */
    var editingWallpaper: WallpaperInfo? = null

    /** User selected [WallpaperModel] for editing. */
    var editingWallpaperModel: WallpaperModel? = null

    /** Data used during transition from small to full preview, based on duo preview view pager. */
    var previewTransitionViewModel: PreviewTransitionViewModel? = null

    /** Gets the view model for static wallpaper preview views. */
    fun getStaticWallpaperPreviewViewModel(): StaticWallpaperPreviewViewModel =
        staticWallpaperPreviewViewModel

    /** Initializes [WallpaperPreviewViewModel] and all its children view models. */
    fun initializeViewModel(context: Context, wallpaper: WallpaperInfo, model: WallpaperModel) {
        editingWallpaper = wallpaper
        editingWallpaperModel = model
        viewModelScope.launch {
            staticWallpaperPreviewViewModel.initializeViewModel(context, wallpaper)
        }
    }
}
