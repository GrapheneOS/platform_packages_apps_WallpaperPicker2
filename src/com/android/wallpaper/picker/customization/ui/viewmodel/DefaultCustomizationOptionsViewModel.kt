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
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
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
    customizationOptionUtil: CustomizationOptionUtil,
    @Assisted viewModelScope: CoroutineScope,
    @Assisted private val initialDeepLinkDestination: String?,
) : CustomizationOptionsViewModel {

    override val customizationOptionsData: Flow<CustomizationOptionsData> =
        flowOf(DefaultCustomizationOptionsData())

    override val wallpaperCarouselViewModel: WallpaperCarouselViewModel =
        wallpaperCarouselViewModelFactory.create(viewModelScope)

    private val _selectedOptionState: MutableStateFlow<CustomizationOption?> =
        MutableStateFlow(
            initialDeepLinkDestination?.let {
                customizationOptionUtil.getCustomizationOptionFromDestination(it)
            }
        )
    override val selectedOption = _selectedOptionState.asStateFlow()

    private val _discardChangesDialogViewModel: MutableStateFlow<DiscardChangesDialogViewModel?> =
        MutableStateFlow(null)
    override val discardChangesDialogViewModel: Flow<DiscardChangesDialogViewModel?> =
        _discardChangesDialogViewModel.asStateFlow()

    fun showDiscardChangesDialogViewModel(onDiscard: () -> Unit) {
        _discardChangesDialogViewModel.value =
            DiscardChangesDialogViewModel(
                onDismiss = { _discardChangesDialogViewModel.value = null },
                onKeepEditing = { _discardChangesDialogViewModel.value = null },
                onDiscard = {
                    onDiscard.invoke()
                    _discardChangesDialogViewModel.value = null
                    unselectOption()
                },
            )
    }

    override fun handleBackPressed(): Boolean {
        if (initialDeepLinkDestination != null) {
            // If initial deep link destination is not null. We should navigate back to the previous
            // app or activity, instead of navigating back the main screen. Thus we return false to
            // have the parent activity handle the navigation to the previous app or activity.
            return false
        }
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

    override fun onTransitionToSecondaryScreenComplete() {}

    fun selectOption(option: CustomizationOption) {
        _selectedOptionState.value = option
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory : CustomizationOptionsViewModelFactory {
        override fun create(
            viewModelScope: CoroutineScope,
            initialDeepLinkDestination: String?,
        ): DefaultCustomizationOptionsViewModel
    }
}
