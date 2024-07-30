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

package com.android.wallpaper.picker.common.preview.ui.viewmodel

import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.WallpaperModelsPair
import com.android.wallpaper.picker.common.preview.domain.interactor.BasePreviewInteractor
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.WallpaperConnection
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Common base preview view-model that is only responsible for binding the workspace and wallpaper.
 */
// Based on WallpaperPreviewViewModel, except cleaned up to only bind wallpaper and workspace
// (workspace binding to be added). Also it is changed to no longer be a top-level ViewModel.
// Instead, the viewModelScope is passed in using assisted inject.
class BasePreviewViewModel
@AssistedInject
constructor(
    private val interactor: BasePreviewInteractor,
    staticPreviewViewModelFactory: StaticPreviewViewModel.Factory,
    displayUtils: DisplayUtils,
    @Assisted private val viewModelScope: CoroutineScope,
) {
    // Don't update smaller display since we always use portrait, always use wallpaper display on
    // single display device.
    val smallerDisplaySize = displayUtils.getRealSize(displayUtils.getSmallerDisplay())
    private val _wallpaperDisplaySize =
        MutableStateFlow(displayUtils.getRealSize(displayUtils.getWallpaperDisplay()))
    val wallpaperDisplaySize = _wallpaperDisplaySize.asStateFlow()

    val staticHomeWallpaperPreviewViewModel by lazy {
        staticPreviewViewModelFactory.create(Screen.HOME_SCREEN, viewModelScope)
    }
    val staticLockWallpaperPreviewViewModel by lazy {
        staticPreviewViewModelFactory.create(Screen.LOCK_SCREEN, viewModelScope)
    }

    private val _whichPreview = MutableStateFlow<WallpaperConnection.WhichPreview?>(null)
    private val whichPreview: Flow<WallpaperConnection.WhichPreview> =
        _whichPreview.asStateFlow().filterNotNull()

    fun setWhichPreview(whichPreview: WallpaperConnection.WhichPreview) {
        _whichPreview.value = whichPreview
    }

    val wallpapers =
        interactor.wallpapers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    val wallpapersAndWhichPreview:
        Flow<Pair<WallpaperModelsPair, WallpaperConnection.WhichPreview>> =
        combine(wallpapers.filterNotNull(), whichPreview) { wallpapers, whichPreview ->
            Pair(wallpapers, whichPreview)
        }

    // TODO (b/348462236): implement complete wallpaper colors flow to bind workspace
    private val _isWallpaperColorPreviewEnabled = MutableStateFlow(false)
    val isWallpaperColorPreviewEnabled = _isWallpaperColorPreviewEnabled.asStateFlow()

    fun setIsWallpaperColorPreviewEnabled(isWallpaperColorPreviewEnabled: Boolean) {
        _isWallpaperColorPreviewEnabled.value = isWallpaperColorPreviewEnabled
    }

    private val _wallpaperConnectionColors: MutableStateFlow<WallpaperColorsModel> =
        MutableStateFlow(WallpaperColorsModel.Loading as WallpaperColorsModel).apply {
            viewModelScope.launch {
                delay(1000)
                if (value == WallpaperColorsModel.Loading) {
                    emit(WallpaperColorsModel.Loaded(null))
                }
            }
        }

    fun setWallpaperConnectionColors(wallpaperColors: WallpaperColorsModel) {
        _wallpaperConnectionColors.value = wallpaperColors
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): BasePreviewViewModel
    }
}
