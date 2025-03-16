/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.customization.picker.mode.data.repository

import com.android.wallpaper.system.UiModeManagerWrapper
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DarkModeStateRepository @Inject constructor(private val uiModeManager: UiModeManagerWrapper) {

    private val _isDarkMode = MutableStateFlow(uiModeManager.getIsNightModeActivated())
    val isDarkMode = _isDarkMode.asStateFlow()

    fun setIsDarkMode(isActive: Boolean) {
        val success = uiModeManager.setNightModeActivated(isActive)
        if (success) {
            _isDarkMode.value = isActive
        }
    }

    fun refreshIsDarkMode() {
        _isDarkMode.value = uiModeManager.getIsNightModeActivated()
    }
}
