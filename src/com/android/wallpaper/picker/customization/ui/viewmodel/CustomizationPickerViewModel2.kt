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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.map

@HiltViewModel
class CustomizationPickerViewModel2
@Inject
constructor(
    val customizationOptionsViewModel: CustomizationOptionsViewModel,
) : ViewModel() {

    enum class PickerScreen {
        MAIN,
        CUSTOMIZATION_OPTION,
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
