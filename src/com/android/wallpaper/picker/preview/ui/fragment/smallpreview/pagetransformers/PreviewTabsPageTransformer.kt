/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.wallpaper.picker.preview.ui.fragment.smallpreview.pagetransformers

import android.graphics.Point
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.android.wallpaper.R
import kotlin.math.abs

/**
 * This class provides the transformations for the tabs in the small preview screen. It enables the
 * behaviour where one out of two tabs cna be focused on and the other is clamped to the side of the
 * left or right of the display
 */
class PreviewTabsPageTransformer(
    private val previewDisplaySize: Point,
) : ViewPager.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        // TODO: cache this reference since findViewById is expensive
        val textView = page.requireViewById<View>(R.id.preview_tab_text)

        val absPosition = Math.abs(position)
        var textViewWidthOffset: Float = (textView.width / 2 * (absPosition))
        if (position > 0) {
            textViewWidthOffset *= -1
        }
        page.translationX = -previewDisplaySize.x / 2 * position + textViewWidthOffset
        val scaleFactor = 0.95f
        page.scaleX = scaleFactor + (1 - scaleFactor) * (1 - absPosition)
        page.scaleY = scaleFactor + (1 - scaleFactor) * (1 - absPosition)
        page.alpha = 0.25f + (1 - abs(position))
    }
}
