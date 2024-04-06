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

package com.android.wallpaper.testing

import android.app.UiModeManager.ContrastChangeListener
import com.android.wallpaper.system.UiModeManagerWrapper
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeUiModeManager @Inject constructor() : UiModeManagerWrapper {
    val listeners = mutableListOf<ContrastChangeListener>()
    private var _contrast: Float? = 0.0f

    override fun addContrastChangeListener(
        executor: Executor,
        listener: ContrastChangeListener,
    ) {
        listeners.add(listener)
    }

    override fun removeContrastChangeListener(listener: ContrastChangeListener) {
        listeners.remove(listener)
    }

    override fun getContrast(): Float? {
        return _contrast
    }

    fun setContrast(contrast: Float?) {
        _contrast = contrast
        contrast?.let { v -> listeners.forEach { it.onContrastChanged(v) } }
    }

    override fun setNightModeActivated(isActive: Boolean) {
        // no-op
    }
}
