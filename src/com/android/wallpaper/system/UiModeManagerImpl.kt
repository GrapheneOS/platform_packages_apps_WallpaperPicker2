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

package com.android.wallpaper.system

import android.app.UiModeManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UiModeManagerImpl @Inject constructor(@ApplicationContext context: Context) :
    UiModeManagerWrapper {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager?
    override fun addContrastChangeListener(
        executor: Executor,
        listener: UiModeManager.ContrastChangeListener,
    ) {
        uiModeManager?.addContrastChangeListener(executor, listener)
    }

    override fun removeContrastChangeListener(listener: UiModeManager.ContrastChangeListener) {
        uiModeManager?.removeContrastChangeListener(listener)
    }

    override fun getContrast(): Float? {
        return uiModeManager?.contrast
    }

    override fun setNightModeActivated(isActive: Boolean) {
        uiModeManager?.setNightModeActivated(isActive)
    }
}
