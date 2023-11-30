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

import androidx.lifecycle.ViewModel
import com.android.wallpaper.model.wallpaper.WallpaperModel
import com.android.wallpaper.picker.preview.domain.interactor.WallpaperPreviewInteractor
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Top level [ViewModel] for [WallpaperPreviewActivity] and its fragments */
@HiltViewModel
class WallpaperPreviewViewModel
@Inject
constructor(
    interactor: WallpaperPreviewInteractor,
    private val staticWallpaperPreviewViewModel: StaticWallpaperPreviewViewModel,
    private val previewActionsViewModel: PreviewActionsViewModel,
) : ViewModel() {

    /** User selected small preview configuration. */
    var selectedSmallPreviewConfig: SmallPreviewConfigViewModel? = null
    /** User selected workspace configuration. */
    var selectedWorkspacePreviewConfig: WorkspacePreviewConfigViewModel? = null

    val wallpaper: Flow<WallpaperModel> = interactor.wallpaperModel

    val onCropButtonClick: Flow<() -> Unit> =
        wallpaper.map { wallpaper ->
            {
                val screenOrientation = selectedSmallPreviewConfig?.screenOrientation
                if (wallpaper is WallpaperModel.StaticWallpaperModel && screenOrientation != null) {
                    getStaticWallpaperPreviewViewModel().fullPreviewCrop?.let {
                        staticWallpaperPreviewViewModel.updateCropHints(
                            mapOf(screenOrientation to it)
                        )
                    }
                }
            }
        }

    /** Gets the view model for static wallpaper preview views. */
    fun getStaticWallpaperPreviewViewModel(): StaticWallpaperPreviewViewModel =
        staticWallpaperPreviewViewModel

    /** Gets the view model for action buttons and action sheet for small preview */
    fun getPreviewActionsViewModel(): PreviewActionsViewModel = previewActionsViewModel
}
