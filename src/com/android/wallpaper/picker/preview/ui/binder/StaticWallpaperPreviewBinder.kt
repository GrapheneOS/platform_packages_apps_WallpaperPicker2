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
import com.android.wallpaper.R
import com.android.wallpaper.picker.preview.ui.util.FullResImageViewUtil
import com.android.wallpaper.picker.preview.ui.viewmodel.FullResWallpaperViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.StaticWallpaperPreviewViewModel
import com.android.wallpaper.util.WallpaperSurfaceCallback.LOW_RES_BITMAP_BLUR_RADIUS
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.launch

object StaticWallpaperPreviewBinder {

    private val ALPHA_OUT: Interpolator = PathInterpolator(0f, 0f, 0.8f, 1f)
    private const val CROSS_FADE_DURATION: Long = 200

    fun bind(
        view: View,
        viewModel: StaticWallpaperPreviewViewModel,
        lifecycleOwner: LifecycleOwner,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        isRtl: Boolean,
    ) {
        val lowResImageView: ImageView = view.requireViewById(R.id.low_res_image)
        lowResImageView.initLowResImageView()

        val fullResImageView: SubsamplingScaleImageView = view.requireViewById(R.id.full_res_image)
        fullResImageView.initFullResImageView()

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.lowResBitmap.collect { lowResImageView.setImageBitmap(it) } }

                launch {
                    viewModel.subsamplingScaleImageViewModel.collect {
                        fullResImageView.setFullResImage(
                            it,
                            isSingleDisplayOrUnfoldedHorizontalHinge,
                            isRtl,
                        )
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
        viewModel: FullResWallpaperViewModel,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        isRtl: Boolean,
    ) {
        // Calculate the scale and the center point for the full res image
        FullResImageViewUtil.getScaleAndCenter(
                measuredWidth,
                measuredHeight,
                viewModel.offsetToStart,
                viewModel.rawWallpaperSize,
                isSingleDisplayOrUnfoldedHorizontalHinge,
                isRtl,
            )
            .also { scaleAndCenter ->
                minScale = scaleAndCenter.minScale
                maxScale = scaleAndCenter.maxScale
                setScaleAndCenter(scaleAndCenter.defaultScale, scaleAndCenter.center)
            }
        // Set the full res image
        setImage(ImageSource.bitmap(viewModel.rawWallpaperBitmap))
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
