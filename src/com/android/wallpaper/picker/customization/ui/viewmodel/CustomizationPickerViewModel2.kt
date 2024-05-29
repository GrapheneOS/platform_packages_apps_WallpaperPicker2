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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class CustomizationPickerViewModel2 {

    enum class PickerScreen {
        MAIN,
        CLOCK,
        SHORTCUT,
    }

    private val _screen = MutableStateFlow(PickerScreen.MAIN)
    val screen = _screen.asStateFlow()

    val onCustomizeClockClicked: Flow<(() -> Unit)?> =
        _screen.map {
            if (it == PickerScreen.MAIN) {
                { _screen.value = PickerScreen.CLOCK }
            } else {
                null
            }
        }

    val onCustomizeShortcutClicked: Flow<(() -> Unit)?> =
        _screen.map {
            if (it == PickerScreen.MAIN) {
                { _screen.value = PickerScreen.SHORTCUT }
            } else {
                null
            }
        }

    /**
     * @return If the screen state is already [PickerScreen.MAIN], return false; otherwise, set if
     *   to main and return true.
     */
    fun setScreenStateMain(): Boolean {
        return if (_screen.value != PickerScreen.MAIN) {
            _screen.value = PickerScreen.MAIN
            true
        } else {
            false
        }
    }
}
