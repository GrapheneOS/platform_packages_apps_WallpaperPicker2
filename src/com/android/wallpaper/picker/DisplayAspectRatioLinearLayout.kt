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
 *
 */

package com.android.wallpaper.picker

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.android.wallpaper.util.ScreenSizeCalculator

/**
 * [LinearLayout] that sizes its children using a fixed aspect ratio that is the same as that of the
 * display, and can lay out multiple children horizontally with margin
 */
class DisplayAspectRatioLinearLayout(
    context: Context,
    attrs: AttributeSet?,
) : LinearLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val screenAspectRatio = ScreenSizeCalculator.getInstance().getScreenAspectRatio(context)
        val parentWidth = this.measuredWidth
        val parentHeight = this.measuredHeight
        val (childWidth, childHeight) =
            if (orientation == HORIZONTAL) {
                var childMargins = 0
                children.forEach { childMargins += it.marginStart + it.marginEnd }
                val availableWidth = parentWidth - paddingStart - paddingEnd - childMargins
                val availableHeight = parentHeight - paddingTop - paddingBottom
                var width = availableWidth / childCount
                var height = (width * screenAspectRatio).toInt()
                if (height > availableHeight) {
                    height = availableHeight
                    width = (height / screenAspectRatio).toInt()
                }
                width to height
            } else {
                var childMargins = 0
                children.forEach { childMargins += it.marginTop + it.marginBottom }
                val availableWidth = parentWidth - paddingStart - paddingEnd
                val availableHeight = parentHeight - paddingTop - paddingBottom - childMargins
                var height = availableHeight / childCount
                var width = (height / screenAspectRatio).toInt()
                if (width > availableWidth) {
                    width = availableWidth
                    height = (width * screenAspectRatio).toInt()
                }
                width to height
            }

        children.forEachIndexed { index, child ->
            child.measure(
                MeasureSpec.makeMeasureSpec(
                    childWidth,
                    MeasureSpec.EXACTLY,
                ),
                MeasureSpec.makeMeasureSpec(
                    childHeight,
                    MeasureSpec.EXACTLY,
                ),
            )
        }
    }
}
