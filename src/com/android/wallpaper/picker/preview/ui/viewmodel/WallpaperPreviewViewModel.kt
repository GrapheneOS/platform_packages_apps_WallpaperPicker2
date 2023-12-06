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
import com.android.wallpaper.model.wallpaper.FoldableDisplay
import com.android.wallpaper.model.wallpaper.WallpaperModel
import com.android.wallpaper.model.wallpaper.getScreenOrientation
import com.android.wallpaper.module.CustomizationSections.Screen
import com.android.wallpaper.picker.di.modules.PreviewUtilsModule.HomeScreenPreviewUtils
import com.android.wallpaper.picker.di.modules.PreviewUtilsModule.LockScreenPreviewUtils
import com.android.wallpaper.picker.preview.domain.interactor.WallpaperPreviewInteractor
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.PreviewUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

/** Top level [ViewModel] for [WallpaperPreviewActivity] and its fragments */
@HiltViewModel
class WallpaperPreviewViewModel
@Inject
constructor(
    interactor: WallpaperPreviewInteractor,
    val staticWallpaperPreviewViewModel: StaticWallpaperPreviewViewModel,
    private val previewActionsViewModel: PreviewActionsViewModel,
    private val displayUtils: DisplayUtils,
    @HomeScreenPreviewUtils private val homePreviewUtils: PreviewUtils,
    @LockScreenPreviewUtils private val lockPreviewUtils: PreviewUtils,
) : ViewModel() {

    val smallerDisplaySize = displayUtils.getRealSize(displayUtils.getSmallerDisplay())
    val wallpaperDisplaySize = displayUtils.getRealSize(displayUtils.getWallpaperDisplay())

    val wallpaper: Flow<WallpaperModel> = interactor.wallpaperModel

    // This is only used for the full screen wallpaper preview.
    private val fullWallpaperPreviewConfigViewModel:
        MutableStateFlow<WallpaperPreviewConfigViewModel?> =
        MutableStateFlow(null)

    // This is only used for the full screen wallpaper preview.
    val fullWallpaper: Flow<Pair<WallpaperModel, WallpaperPreviewConfigViewModel>> =
        combine(wallpaper, fullWallpaperPreviewConfigViewModel.filterNotNull()) {
            wallpaper,
            previewViewModel ->
            Pair(wallpaper, previewViewModel)
        }

    // This is only used for the full screen wallpaper preview.
    private val _fullWorkspacePreviewConfigViewModel:
        MutableStateFlow<WorkspacePreviewConfigViewModel?> =
        MutableStateFlow(null)

    // This is only used for the full screen wallpaper preview.
    val fullWorkspacePreviewConfigViewModel: Flow<WorkspacePreviewConfigViewModel> =
        _fullWorkspacePreviewConfigViewModel.filterNotNull()

    val onCropButtonClick: Flow<() -> Unit> =
        combine(wallpaper, fullWallpaperPreviewConfigViewModel.filterNotNull()) {
            wallpaper,
            previewViewModel ->
            {
                if (wallpaper is WallpaperModel.StaticWallpaperModel) {
                    staticWallpaperPreviewViewModel.fullPreviewCrop?.let {
                        staticWallpaperPreviewViewModel.updateCropHints(
                            mapOf(previewViewModel.screenOrientation to it)
                        )
                    }
                }
            }
        }

    fun getWorkspacePreviewConfig(
        screen: Screen,
        foldableDisplay: FoldableDisplay?,
    ): WorkspacePreviewConfigViewModel {
        val previewUtils =
            when (screen) {
                Screen.HOME_SCREEN -> {
                    homePreviewUtils
                }
                Screen.LOCK_SCREEN -> {
                    lockPreviewUtils
                }
            }
        val displayId =
            when (foldableDisplay) {
                FoldableDisplay.FOLDED -> {
                    displayUtils.getSmallerDisplay().displayId
                }
                FoldableDisplay.UNFOLDED -> {
                    displayUtils.getWallpaperDisplay().displayId
                }
                null -> {
                    displayUtils.getWallpaperDisplay().displayId
                }
            }
        return WorkspacePreviewConfigViewModel(
            previewUtils = previewUtils,
            displayId = displayId,
        )
    }

    fun onSmallPreviewClicked(screen: Screen, foldableDisplay: FoldableDisplay?) {
        fullWallpaperPreviewConfigViewModel.value =
            getWallpaperPreviewConfig(screen, foldableDisplay)
        _fullWorkspacePreviewConfigViewModel.value =
            getWorkspacePreviewConfig(screen, foldableDisplay)
    }

    private fun getWallpaperPreviewConfig(
        screen: Screen,
        foldableDisplay: FoldableDisplay?,
    ): WallpaperPreviewConfigViewModel {
        val displaySize =
            when (foldableDisplay) {
                FoldableDisplay.FOLDED -> {
                    smallerDisplaySize
                }
                FoldableDisplay.UNFOLDED -> {
                    wallpaperDisplaySize
                }
                null -> {
                    wallpaperDisplaySize
                }
            }
        return WallpaperPreviewConfigViewModel(
            screen = screen,
            displaySize = displaySize,
            screenOrientation = getScreenOrientation(displaySize),
        )
    }

    /** Gets the view model for action buttons and action sheet for small preview */
    fun getPreviewActionsViewModel(): PreviewActionsViewModel = previewActionsViewModel
}
