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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.picker.common.preview.ui.viewmodel.BasePreviewViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

@HiltViewModel
class CustomizationPickerViewModel2
@Inject
constructor(
    customizationOptionsViewModelFactory: CustomizationOptionsViewModelFactory,
    basePreviewViewModelFactory: BasePreviewViewModel.Factory,
) : ViewModel() {

    val customizationOptionsViewModel =
        customizationOptionsViewModelFactory.create(viewModelScope = viewModelScope)
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
        customizationOptionsViewModel.selectedOption.map {
            if (it != null) {
                Pair(PickerScreen.CUSTOMIZATION_OPTION, it)
            } else {
                Pair(PickerScreen.MAIN, null)
            }
        }

    fun onBackPressed(): Boolean = customizationOptionsViewModel.deselectOption()
}
