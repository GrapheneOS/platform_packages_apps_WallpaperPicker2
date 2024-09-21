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
package com.android.wallpaper.picker.preview.ui.view

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import com.android.wallpaper.util.WallpaperCropUtils
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

/**
 * Simulates the actual wallpaper surface's default system zoom view size based on its parent
 * surface size and the device's system wallpaper scale.
 *
 * Scales its size to surface_size * system_scale and centers the view on the surface.
 *
 * Acts like a [SubsamplingScaleImageView] if not given a surface size.
 *
 * Used in wallpaper small and full preview.
 */
class SystemScaledSubsamplingScaleImageView(context: Context, attrs: AttributeSet? = null) :
    SubsamplingScaleImageView(context, attrs) {

    private var surfaceSize: Point? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (surfaceSize != null) {
            val scale = WallpaperCropUtils.getSystemWallpaperMaximumScale(context)
            setMeasuredDimension((measuredWidth * scale).toInt(), (measuredHeight * scale).toInt())
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {

        surfaceSize?.let {
            // Calculate the size of wallpaper surface based on the system zoom
            // and scale & center the wallpaper preview to respect the zoom.
            val scale = WallpaperCropUtils.getSystemWallpaperMaximumScale(context)

            val scaledWidth = (it.x * scale).toInt()
            val scaledHeight = (it.y * scale).toInt()
            val xCentered = (it.x - scaledWidth) / 2
            val yCentered = (it.y - scaledHeight) / 2

            x = xCentered.toFloat()
            y = yCentered.toFloat()
            layoutParams.width = scaledWidth
            layoutParams.height = scaledHeight
        }
    }

    fun setSurfaceSize(size: Point) {
        surfaceSize = size
    }
}
