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

package com.android.wallpaper.picker.customization.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import com.android.wallpaper.R
import com.android.wallpaper.util.ScreenSizeCalculator

/**
 * [CardView] that calculates the corner radius dynamically when on measure. We use the ratio
 * between its height and the screen height to calculate the corner radius.
 */
class PreviewCardView(
    context: Context,
    attrs: AttributeSet?,
) : CardView(context, attrs) {

    // This should be the corner radius when the preview height is the screen height
    private val previewCornerRadius = resources.getDimensionPixelSize(R.dimen.preview_corner_radius)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val screenHeight = ScreenSizeCalculator.getInstance().getScreenHeight(context)
        radius = previewCornerRadius * (measuredHeight.toFloat() / screenHeight.toFloat())
    }
}
