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

import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class DefaultCustomizationOptionsViewModel
@AssistedInject
constructor(
    wallpaperCarouselViewModelFactory: WallpaperCarouselViewModel.Factory,
    @Assisted viewModelScope: CoroutineScope,
) : CustomizationOptionsViewModel {

    override val customizationOptionsData: Flow<CustomizationOptionsData> =
        flowOf(DefaultCustomizationOptionsData())

    override val wallpaperCarouselViewModel =
        wallpaperCarouselViewModelFactory.create(viewModelScope)

    private val _selectedOptionState =
        MutableStateFlow<CustomizationOptionUtil.CustomizationOption?>(null)
    override val selectedOption = _selectedOptionState.asStateFlow()

    private val _discardChangesDialogViewModel: MutableStateFlow<DiscardChangesDialogViewModel?> =
        MutableStateFlow(null)
    override val discardChangesDialogViewModel: Flow<DiscardChangesDialogViewModel?> =
        _discardChangesDialogViewModel.asStateFlow()

    fun showDiscardChangesDialogViewModel() {
        _discardChangesDialogViewModel.value =
            DiscardChangesDialogViewModel(
                onDismiss = { _discardChangesDialogViewModel.value = null },
                onKeepEditing = { _discardChangesDialogViewModel.value = null },
                onDiscard = {
                    _discardChangesDialogViewModel.value = null
                    unselectOption()
                },
            )
    }

    override fun handleBackPressed(): Boolean {
        return unselectOption()
    }

    /**
     * Unselect the currently selected option. If the option is already unselected, do nothing and
     * return false; otherwise, unselect and return true.
     */
    private fun unselectOption(): Boolean {
        return if (_selectedOptionState.value != null) {
            _selectedOptionState.value = null
            true
        } else {
            false
        }
    }

    override fun resetPreview() {}

    fun selectOption(option: CustomizationOptionUtil.CustomizationOption) {
        _selectedOptionState.value = option
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory : CustomizationOptionsViewModelFactory {
        override fun create(viewModelScope: CoroutineScope): DefaultCustomizationOptionsViewModel
    }
}
