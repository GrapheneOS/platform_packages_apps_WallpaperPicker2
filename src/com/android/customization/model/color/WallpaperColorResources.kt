/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.customization.model.color

import android.R
import android.app.WallpaperColors
import android.content.Context
import android.util.SparseIntArray
import android.widget.RemoteViews.ColorResources
import com.android.systemui.monet.ColorScheme
import com.android.systemui.monet.TonalPalette

/** A class to override colors in a [Context] with wallpaper colors. */
open class WallpaperColorResources(wallpaperColors: WallpaperColors) {
    private val colorOverlay = SparseIntArray()

    init {
        val wallpaperColorScheme = ColorScheme(wallpaperColors = wallpaperColors, darkTheme = false)
        with(wallpaperColorScheme) {
            addOverlayColor(neutral1, R.color.system_neutral1_10)
            addOverlayColor(neutral2, R.color.system_neutral2_10)
            addOverlayColor(accent1, R.color.system_accent1_10)
            addOverlayColor(accent2, R.color.system_accent2_10)
            addOverlayColor(accent3, R.color.system_accent3_10)
        }
    }

    /** Applies the wallpaper color resources to the `context`. */
    fun apply(context: Context) {
        ColorResources.create(context, colorOverlay)?.apply(context)
    }

    fun addOverlayColor(colorSchemeHue: TonalPalette, firstResourceColorId: Int) {
        colorSchemeHue.allShades.forEachIndexed { index, color ->
            colorOverlay.put(firstResourceColorId + index, color)
        }
    }
}
