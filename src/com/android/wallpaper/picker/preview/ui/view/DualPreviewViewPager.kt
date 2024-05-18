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
package com.android.wallpaper.picker.preview.ui.view

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.DeviceDisplayType

/**
 * This view pager sizes itself to be the exact height required by its content views:
 * [DualDisplayAspectRatioLayout]. This is required because the actual heights of
 * [DualDisplayAspectRatioLayout] are determined after the their parent ViewPager is rendered. This
 * prevents the ViewPager from sizing itself to wrap its contents.
 */
class DualPreviewViewPager
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null /* attrs */) : ViewPager(context, attrs) {
    private var previewDisplaySizes: Map<DeviceDisplayType, Point>? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (previewDisplaySizes == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val parentWidth =
            this.measuredWidth -
                context.resources.let {
                    it.getDimension(R.dimen.small_dual_preview_edge_space) * 2 -
                        it.getDimension(R.dimen.small_preview_inter_preview_margin) * 3
                }

        val smallDisplayAspectRatio =
            getPreviewDisplaySize(DeviceDisplayType.FOLDED).let { it.x.toFloat() / it.y }

        val largeDisplayAspectRatio =
            getPreviewDisplaySize(DeviceDisplayType.UNFOLDED).let { it.x.toFloat() / it.y }

        val viewPagerHeight = parentWidth / (largeDisplayAspectRatio + smallDisplayAspectRatio)

        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(
                viewPagerHeight.toInt(),
                MeasureSpec.EXACTLY,
            )
        )
    }

    fun setDisplaySizes(displaySizes: Map<DeviceDisplayType, Point>) {
        previewDisplaySizes = displaySizes
    }

    /**
     * Gets the display size for a [DualDisplayAspectRatioLayout.Companion.PreviewView].
     *
     * Outside this class we should get display size via
     * [DualDisplayAspectRatioLayout.getPreviewDisplaySize].
     */
    private fun getPreviewDisplaySize(display: DeviceDisplayType): Point {
        return checkNotNull(previewDisplaySizes?.get(display))
    }
}
