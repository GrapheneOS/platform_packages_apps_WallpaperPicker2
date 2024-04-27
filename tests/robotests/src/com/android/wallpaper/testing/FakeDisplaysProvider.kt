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

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.view.Display
import com.android.wallpaper.util.DisplaysProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.robolectric.shadows.ShadowDisplayManager

/**
 * Uses ShadowDisplayManager to create fake displays for testing, due to the difficulty in directly
 * creating Display instances. The created display's type will not be internal, but will function
 * the same when testing DisplayUtils.
 */
@Singleton
class FakeDisplaysProvider @Inject constructor(@ApplicationContext appContext: Context) :
    DisplaysProvider {
    private val displayManager =
        appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    fun setDisplays(displaySizes: List<Point>) {
        ShadowDisplayManager.reset()
        displaySizes
            // Map display sizes to qualifier strings, e.g. Point(360, 400) -> "w360dp-h400dp"
            .map { "w${it.x}dp-h${it.y}dp" }
            .forEachIndexed { index, qualifierStr ->
                if (index == 0) {
                    ShadowDisplayManager.changeDisplay(Display.DEFAULT_DISPLAY, qualifierStr)
                } else {
                    ShadowDisplayManager.addDisplay(qualifierStr)
                }
            }
    }

    override fun getInternalDisplays(): List<Display> {
        val allDisplays: Array<out Display> =
            displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED)
        return allDisplays.toList()
    }

    companion object {
        // Common display sizes used for testing
        val FOLDABLE_FOLDED = Point(1080, 2092)
        val FOLDABLE_UNFOLDED_LAND = Point(2208, 1840)
        val FOLDABLE_UNFOLDED_PORT = Point(1840, 2208)
        val LARGE_SCREEN_LAND = Point(2560, 1600)
        val LARGE_SCREEN_PORT = Point(1600, 2560)
    }
}
