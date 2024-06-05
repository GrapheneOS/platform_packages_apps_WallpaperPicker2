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
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@ViewModelScoped
class DefaultCustomizationOptionsViewModel @Inject constructor() : CustomizationOptionsViewModel {

    private val _selectedOptionState =
        MutableStateFlow<CustomizationOptionUtil.CustomizationOption?>(null)
    override val selectedOption = _selectedOptionState.asStateFlow()

    override fun deselectOption(): Boolean {
        return if (_selectedOptionState.value != null) {
            _selectedOptionState.value = null
            true
        } else {
            false
        }
    }

    fun selectOption(option: CustomizationOptionUtil.CustomizationOption) {
        _selectedOptionState.value = option
    }
}
