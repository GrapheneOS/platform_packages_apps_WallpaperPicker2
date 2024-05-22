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
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Configuration.Orientation
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
 * creating Display instances.
 *
 * The limitations of the fake displays include:
 * - The created display's type will not be internal, but will function the same when testing
 *   DisplayUtils.
 * - The created displays will have the same uniqueId, and is not customizable through
 *   ShadowDisplayManager.
 */
@Singleton
class FakeDisplaysProvider
@Inject
constructor(@ApplicationContext private val appContext: Context) : DisplaysProvider {
    private val displayManager =
        appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    fun setDisplays(displayConfigs: List<FakeDisplayConfig>) {
        ShadowDisplayManager.reset()
        displayConfigs.forEachIndexed { index, config ->
            if (index == 0) {
                ShadowDisplayManager.changeDisplay(
                    Display.DEFAULT_DISPLAY,
                    configToQualifierString(config)
                )
                config.naturallyPortrait?.let {
                    ShadowDisplayManager.setNaturallyPortrait(Display.DEFAULT_DISPLAY, it)
                }
                appContext.resources.configuration.densityDpi = config.dpi
            } else {
                val displayId = ShadowDisplayManager.addDisplay(configToQualifierString(config))
                config.naturallyPortrait?.let {
                    ShadowDisplayManager.setNaturallyPortrait(displayId, it)
                }
            }
        }
    }

    fun configToQualifierString(config: FakeDisplayConfig): String {
        val suffix =
            if (config.orientation == ORIENTATION_LANDSCAPE) "-land"
            else if (config.orientation == ORIENTATION_PORTRAIT) "-port" else ""
        return "w${config.displaySize.x}dp-h${config.displaySize.y}dp$suffix"
    }

    override fun getInternalDisplays(): List<Display> {
        val allDisplays: Array<out Display> =
            displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED)
        return allDisplays.toList()
    }

    data class FakeDisplayConfig(
        /**
         * The display width and height in pixels. When returned from the Display, it is adjusted
         * based on the orientation.
         */
        val displaySize: Point,
        /** Whether the device is naturally portrait, used to determine the degree of rotation. */
        val naturallyPortrait: Boolean? = null,
        /**
         * The current orientation of the device. The Display adjusts its size accordingly, with x
         * as the larger dimension if landscape, and y as the larger dimension if portrait.
         */
        @Orientation val orientation: Int? = null,
        /**
         * The DPI of a device, used to calculate screen size. This value needs to be set in
         * appContext.resources.configuration.densityDpi to take effect.
         */
        val dpi: Int = 1,
    )

    companion object {
        // Common display sizes used for testing
        val HANDHELD = FakeDisplayConfig(Point(1440, 3120), true, ORIENTATION_PORTRAIT, 560)
        val FOLDABLE_FOLDED = FakeDisplayConfig(Point(1080, 2092), true, ORIENTATION_PORTRAIT, 408)
        val FOLDABLE_UNFOLDED_LAND =
            FakeDisplayConfig(Point(2208, 1840), false, ORIENTATION_LANDSCAPE, 380)
        val FOLDABLE_UNFOLDED_PORT =
            FakeDisplayConfig(Point(2208, 1840), false, ORIENTATION_PORTRAIT, 380)
        val TABLET_LAND = FakeDisplayConfig(Point(2560, 1600), false, ORIENTATION_LANDSCAPE, 276)
        val TABLET_PORT = FakeDisplayConfig(Point(2560, 1600), false, ORIENTATION_PORTRAIT, 276)
    }
}
