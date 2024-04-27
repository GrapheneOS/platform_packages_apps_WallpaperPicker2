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
package com.android.wallpaper.util

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisplaysProviderImpl
@Inject
constructor(@ApplicationContext private val appContext: Context) : DisplaysProvider {
    private val displayManager: DisplayManager by lazy {
        appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    override fun getInternalDisplays(): List<Display> {
        val allDisplays: Array<out Display> =
            displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED)
        if (allDisplays.isEmpty()) {
            Log.e(TAG, "No displays found on context $appContext")
            throw RuntimeException("No displays found!")
        }
        return allDisplays.filter { it.type == Display.TYPE_INTERNAL }
    }
    companion object {
        private const val TAG = "DisplaysProviderImpl"
    }
}
