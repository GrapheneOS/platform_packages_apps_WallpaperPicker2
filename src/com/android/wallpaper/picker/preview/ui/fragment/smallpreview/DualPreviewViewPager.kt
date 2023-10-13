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
package com.android.wallpaper.picker.preview.ui.fragment.smallpreview

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager
import com.android.wallpaper.R
import com.android.wallpaper.picker.wallpaper.utils.DualDisplayAspectRatioLayout

/**
 * This view pager sizes itself to be the exact height required by its content views:
 * [DualDisplayAspectRatioLayout]. This is required because the actual heights of
 * [DualDisplayAspectRatioLayout] are determined after the their parent ViewPager is rendered. This
 * prevents the ViewPager from sizing itself to wrap its contents.
 */
class DualPreviewViewPager
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null /* attrs */) : ViewPager(context, attrs) {
    private var previewDisplaySizes:
        Map<DualDisplayAspectRatioLayout.Companion.PreviewView, Point>? =
        null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (previewDisplaySizes == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        getPreviewDisplaySize(DualDisplayAspectRatioLayout.Companion.PreviewView.FOLDED)?.let {
            smallDisplaySize ->
            getPreviewDisplaySize(DualDisplayAspectRatioLayout.Companion.PreviewView.UNFOLDED)
                ?.let { largeDisplaySize ->
                    val parentWidth =
                        this.measuredWidth -
                            context.resources.getDimension(R.dimen.small_dual_preview_edge_space) *
                                2 -
                            context.resources.getDimension(
                                R.dimen.small_preview_inter_preview_margin
                            ) * 3

                    val smallDisplayAR = smallDisplaySize.x.toFloat() / smallDisplaySize.y

                    val largeDisplayAR = largeDisplaySize.x.toFloat() / largeDisplaySize.y

                    val viewPagerHeight = parentWidth / (largeDisplayAR + smallDisplayAR)

                    super.onMeasure(
                        widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(
                            viewPagerHeight.toInt(),
                            MeasureSpec.EXACTLY,
                        )
                    )
                }
        }
    }

    fun setDisplaySizes(
        displaySizes: Map<DualDisplayAspectRatioLayout.Companion.PreviewView, Point>
    ) {
        previewDisplaySizes = displaySizes
    }

    fun getPreviewDisplaySize(
        previewView: DualDisplayAspectRatioLayout.Companion.PreviewView
    ): Point? {
        return previewDisplaySizes?.get(previewView)
    }
}
