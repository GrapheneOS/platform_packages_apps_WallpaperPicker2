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

package com.android.wallpaper.picker.customization.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.picker.common.preview.ui.viewmodel.BasePreviewViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

@HiltViewModel
class CustomizationPickerViewModel2
@Inject
constructor(
    customizationOptionsViewModelFactory: CustomizationOptionsViewModelFactory,
    basePreviewViewModelFactory: BasePreviewViewModel.Factory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialDestination: String? = savedStateHandle[KEY_DESTINATION]

    val customizationOptionsViewModel =
        customizationOptionsViewModelFactory.create(
            viewModelScope = viewModelScope,
            initialDestination,
        )
    val basePreviewViewModel = basePreviewViewModelFactory.create(viewModelScope)

    enum class PickerScreen {
        MAIN,
        CUSTOMIZATION_OPTION,
    }

    private val _selectedPreviewScreen = MutableStateFlow(LOCK_SCREEN)
    val selectedPreviewScreen = _selectedPreviewScreen.asStateFlow()

    fun selectPreviewScreen(screen: Screen) {
        _selectedPreviewScreen.value = screen
    }

    val screen =
        customizationOptionsViewModel.selectedOption
            .map {
                if (it != null) {
                    Pair(PickerScreen.CUSTOMIZATION_OPTION, it)
                } else {
                    Pair(PickerScreen.MAIN, null)
                }
            }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    private val isLockPreviewReady: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val isHomePreviewReady: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun setPreviewReady(screen: Screen, isReady: Boolean) {
        when (screen) {
            LOCK_SCREEN -> isLockPreviewReady.value = isReady
            HOME_SCREEN -> isHomePreviewReady.value = isReady
        }
    }

    /** Flow of float that emits to trigger the lock screen preview to animate to an alpha value. */
    val lockPreviewAlpha: Flow<PreviewAlpha> =
        combine(isLockPreviewReady, screen, selectedPreviewScreen) {
                isPreviewReady,
                navigationScreen,
                previewScreen ->
                getPreviewAlpha(
                    isPreviewReady = isPreviewReady,
                    navigationScreen = navigationScreen.first,
                    previewScreen = previewScreen,
                    targetScreen = LOCK_SCREEN,
                )
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    /** Flow of a style res Id that represents the home preview label text appearance. */
    val lockPreviewLabelTextAppearance: Flow<Int> =
        selectedPreviewScreen.map {
            if (it == LOCK_SCREEN) {
                R.style.TextAppearance_Preview_Label_Selected
            } else {
                R.style.TextAppearance_Preview_Label_Unselected
            }
        }

    /** Flow of float that emits to trigger the home screen preview to animate to an alpha value. */
    val homePreviewAlpha: Flow<PreviewAlpha> =
        combine(isHomePreviewReady, screen, selectedPreviewScreen) {
                isPreviewReady,
                navigationScreen,
                previewScreen ->
                getPreviewAlpha(
                    isPreviewReady = isPreviewReady,
                    navigationScreen = navigationScreen.first,
                    previewScreen = previewScreen,
                    targetScreen = HOME_SCREEN,
                )
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    /** Flow of a style res Id that represents the home preview label text appearance. */
    val homePreviewLabelTextAppearance: Flow<Int> =
        selectedPreviewScreen.map {
            if (it == HOME_SCREEN) {
                R.style.TextAppearance_Preview_Label_Selected
            } else {
                R.style.TextAppearance_Preview_Label_Unselected
            }
        }

    /**
     * Get the preview's target alpha value to animate or set to.
     *
     * @return [PreviewAlpha] contains the target alpha value and shouldAnimate. If shouldAnimate is
     *   true, the view should animate to the target alpha; otherwise, directly set to the alpha.
     */
    private fun getPreviewAlpha(
        isPreviewReady: Boolean,
        navigationScreen: PickerScreen,
        previewScreen: Screen,
        targetScreen: Screen,
    ): PreviewAlpha {
        return if (isPreviewReady) {
            when (navigationScreen) {
                PickerScreen.MAIN ->
                    if (previewScreen == targetScreen)
                        PreviewAlpha(alpha = PREVIEW_SHOW_ALPHA, shouldAnimate = true)
                    else PreviewAlpha(alpha = PREVIEW_FADE_ALPHA, shouldAnimate = true)
                PickerScreen.CUSTOMIZATION_OPTION -> {
                    when (previewScreen) {
                        targetScreen ->
                            PreviewAlpha(alpha = PREVIEW_SHOW_ALPHA, shouldAnimate = true)
                        else -> PreviewAlpha(alpha = PREVIEW_HIDE_ALPHA, shouldAnimate = true)
                    }
                }
            }
        } else {
            PreviewAlpha(alpha = PREVIEW_HIDE_ALPHA, shouldAnimate = false)
        }
    }

    val isPagerInteractable: Flow<Boolean> =
        customizationOptionsViewModel.selectedOption.map { it == null }

    val isPreviewClickable: Flow<Boolean> =
        combine(basePreviewViewModel.wallpapers, isPagerInteractable) {
            wallpapers,
            isPagerInteractable ->
            wallpapers != null && isPagerInteractable
        }

    companion object {
        const val PREVIEW_SHOW_ALPHA = 1F
        const val PREVIEW_HIDE_ALPHA = 0F
        const val PREVIEW_FADE_ALPHA = 0.4F

        const val KEY_DESTINATION = "destination"
    }
}
