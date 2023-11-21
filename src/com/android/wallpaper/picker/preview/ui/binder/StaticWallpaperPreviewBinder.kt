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
package com.android.wallpaper.picker.preview.ui.binder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.model.wallpaper.ScreenOrientation
import com.android.wallpaper.picker.preview.ui.util.FullResImageViewUtil
import com.android.wallpaper.picker.preview.ui.util.FullResImageViewUtil.getCropRect
import com.android.wallpaper.picker.preview.ui.viewmodel.StaticWallpaperPreviewViewModel
import com.android.wallpaper.util.WallpaperSurfaceCallback.LOW_RES_BITMAP_BLUR_RADIUS
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.launch

object StaticWallpaperPreviewBinder {

    private val ALPHA_OUT: Interpolator = PathInterpolator(0f, 0f, 0.8f, 1f)
    private const val CROSS_FADE_DURATION: Long = 200

    fun bind(
        lowResImageView: ImageView,
        fullResImageView: SubsamplingScaleImageView,
        viewModel: StaticWallpaperPreviewViewModel,
        screenOrientation: ScreenOrientation,
        viewLifecycleOwner: LifecycleOwner,
    ) {
        lowResImageView.initLowResImageView()
        fullResImageView.initFullResImageView()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.lowResBitmap.collect { lowResImageView.setImageBitmap(it) } }

                launch {
                    viewModel.subsamplingScaleImageViewModel.collect {
                        val cropHint = it.cropHints?.get(screenOrientation)
                        fullResImageView.setFullResImage(
                            it.rawWallpaperBitmap,
                            it.rawWallpaperSize,
                            cropHint,
                        )

                        // Both small and full previews change fullPreviewCrop but it should track
                        // only full preview crop, initial value should align with existing crop
                        // otherwise it's a new preview selection and use current visible crop
                        viewModel.fullPreviewCrop = cropHint ?: fullResImageView.getCropRect()
                        crossFadeInFullResImageView(lowResImageView, fullResImageView)
                    }
                }
            }
        }
    }

    private fun ImageView.initLowResImageView() {
        setRenderEffect(
            RenderEffect.createBlurEffect(
                LOW_RES_BITMAP_BLUR_RADIUS,
                LOW_RES_BITMAP_BLUR_RADIUS,
                Shader.TileMode.CLAMP
            )
        )
    }

    private fun SubsamplingScaleImageView.initFullResImageView() {
        setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
        setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
    }

    private fun SubsamplingScaleImageView.setFullResImage(
        rawWallpaperBitmap: Bitmap,
        rawWallpaperSize: Point,
        cropHint: Rect?,
    ) {
        // Set the full res image
        setImage(ImageSource.bitmap(rawWallpaperBitmap))
        // Calculate the scale and the center point for the full res image
        FullResImageViewUtil.getScaleAndCenter(
                Point(measuredWidth, measuredHeight),
                rawWallpaperSize,
                cropHint,
            )
            .let { scaleAndCenter ->
                minScale = scaleAndCenter.minScale
                maxScale = scaleAndCenter.maxScale
                setScaleAndCenter(scaleAndCenter.defaultScale, scaleAndCenter.center)
            }
    }

    private fun crossFadeInFullResImageView(lowResImageView: ImageView, fullResImageView: View) {
        fullResImageView.alpha = 0f
        fullResImageView
            .animate()
            .alpha(1f)
            .setInterpolator(ALPHA_OUT)
            .setDuration(CROSS_FADE_DURATION)
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        lowResImageView.setImageBitmap(null)
                    }
                }
            )
    }
}
