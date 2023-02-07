/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display

/**
 * Utility class to provide methods to find and obtain information about displays via {@link
 * DisplayManager}
 */
class DisplayUtils(context: Context) {
    companion object {
        private const val TAG = "DisplayUtils"
    }

    private val internalDisplays: List<Display>

    init {
        val appContext = context.applicationContext
        val dm = appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val allDisplays: Array<out Display> =
            dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED)
        if (allDisplays.isEmpty()) {
            Log.e(TAG, "No displays found on context $appContext")
            throw RuntimeException("No displays found!")
        }
        internalDisplays = allDisplays.filter { it.type == Display.TYPE_INTERNAL }
    }

    fun hasMultiInternalDisplays(): Boolean {
        return internalDisplays.size > 1
    }

    /** Returns the {@link Display} to be used to calculate wallpaper size and cropping. */
    fun getWallpaperDisplay(): Display {
        return internalDisplays.maxWithOrNull { a, b -> getRealSize(a) - getRealSize(b) }
            ?: internalDisplays[0]
    }

    /**
     * Returns `true` if the current display is the wallpaper display on a multi-display device.
     *
     * On a multi-display device the wallpaper display is the largest display while on a single
     * display device the only display is both the wallpaper display and the current display.
     */
    fun isOnWallpaperDisplay(activity: Activity): Boolean {
        return activity.display.displayId == getWallpaperDisplay().displayId
    }

    private fun getRealSize(display: Display): Int {
        val p = Point()
        display.getRealSize(p)
        return p.x * p.y
    }
}
