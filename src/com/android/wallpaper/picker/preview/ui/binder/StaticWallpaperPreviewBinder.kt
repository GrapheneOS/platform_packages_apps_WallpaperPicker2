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
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.SurfaceView
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import android.widget.ImageView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import com.android.app.tracing.TraceUtils.trace
import com.android.wallpaper.R
import com.android.wallpaper.picker.preview.shared.model.CropSizeModel
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.picker.preview.ui.util.FullResImageViewUtil
import com.android.wallpaper.picker.preview.ui.view.SystemScaledSubsamplingScaleImageView
import com.android.wallpaper.picker.preview.ui.viewmodel.StaticWallpaperPreviewViewModel
import com.android.wallpaper.util.RtlUtils
import com.android.wallpaper.util.SurfaceViewUtils.attachView
import com.android.wallpaper.util.WallpaperCropUtils
import com.android.wallpaper.util.WallpaperSurfaceCallback.LOW_RES_BITMAP_BLUR_RADIUS
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object StaticWallpaperPreviewBinder {

    private val ALPHA_OUT: Interpolator = PathInterpolator(0f, 0f, 0.8f, 1f)
    private const val CROSS_FADE_DURATION: Long = 200

    fun bind(
        staticPreviewView: View,
        wallpaperSurface: SurfaceView,
        viewModel: StaticWallpaperPreviewViewModel,
        displaySize: Point,
        parentCoroutineScope: CoroutineScope,
        isFullScreen: Boolean = false,
    ) {
        val fullResImageView =
            staticPreviewView.requireViewById<SystemScaledSubsamplingScaleImageView>(
                R.id.full_res_image
            )
        val lowResImageView = staticPreviewView.requireViewById<ImageView>(R.id.low_res_image)

        // surfaceView.width and surfaceFrame.width here can be different,
        // one represents the size of the view and the other represents the
        // size of the surface. When setting a view to the surface host,
        // we want to set it based on the surface's size not the view's size
        adjustSizeAndAttachPreview(
            wallpaperSurface.holder.surfaceFrame,
            wallpaperSurface,
            staticPreviewView,
            fullResImageView,
        )

        lowResImageView.initLowResImageView()
        fullResImageView.initFullResImageView()

        parentCoroutineScope.launch {
            // Show low res image only for small preview with supported wallpaper
            if (!isFullScreen) {
                launch {
                    viewModel.lowResBitmap.collect {
                        it?.let {
                            lowResImageView.setImageBitmap(it)
                            lowResImageView.isVisible = true
                        }
                    }
                }
            }

            launch {
                viewModel.subsamplingScaleImageViewModel.collect { imageModel ->
                    trace(TAG) {
                        val cropHint = imageModel.fullPreviewCropModels?.get(displaySize)?.cropHint
                        fullResImageView.setFullResImage(
                            ImageSource.cachedBitmap(imageModel.rawWallpaperBitmap),
                            imageModel.rawWallpaperSize,
                            displaySize,
                            cropHint,
                            RtlUtils.isRtl(lowResImageView.context),
                            isFullScreen,
                        )

                        // Fill in the default crop region if the displaySize for this preview
                        // is missing.
                        val imageSize = Point(fullResImageView.width, fullResImageView.height)
                        viewModel.updateDefaultPreviewCropModel(
                            displaySize,
                            FullPreviewCropModel(
                                cropHint =
                                    WallpaperCropUtils.calculateVisibleRect(
                                        imageModel.rawWallpaperSize,
                                        imageSize,
                                    ),
                                cropSizeModel =
                                    CropSizeModel(
                                        wallpaperZoom =
                                            WallpaperCropUtils.calculateMinZoom(
                                                imageModel.rawWallpaperSize,
                                                imageSize,
                                            ),
                                        hostViewSize = imageSize,
                                        cropViewSize =
                                            WallpaperCropUtils.calculateCropSurfaceSize(
                                                fullResImageView.resources,
                                                max(imageSize.x, imageSize.y),
                                                min(imageSize.x, imageSize.y),
                                                imageSize.x,
                                                imageSize.y,
                                            ),
                                    ),
                            ),
                        )

                        if (lowResImageView.isVisible) {
                            crossFadeInFullResImageView(lowResImageView, fullResImageView)
                        }
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
                Shader.TileMode.CLAMP,
            )
        )
    }

    private fun SubsamplingScaleImageView.initFullResImageView() {
        setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
        setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
    }

    private fun SubsamplingScaleImageView.setFullResImage(
        imageSource: ImageSource,
        rawWallpaperSize: Point,
        displaySize: Point,
        cropHint: Rect?,
        isRtl: Boolean,
        isFullScreen: Boolean,
    ) {
        // Set the full res image
        setImage(imageSource)
        // Calculate the scale and the center point for the full res image
        doOnLayout {
            FullResImageViewUtil.getScaleAndCenter(
                    Point(measuredWidth, measuredHeight),
                    rawWallpaperSize,
                    displaySize,
                    cropHint,
                    isRtl,
                )
                .let { scaleAndCenter ->
                    minScale = scaleAndCenter.minScale
                    maxScale = scaleAndCenter.maxScale
                    setScaleAndCenter(scaleAndCenter.defaultScale, scaleAndCenter.center)
                }
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

    // When showing static wallpaper preview, we set the full res image view to be bigger than the
    // image by N percent (usually 10%) as given by getSystemWallpaperMaximumScale via
    // SystemScaledSubsamplingScaleImageView. This ensures that no matter what scale and pan is set
    // by the user, at least N% of the source image in the preview will be preserved around the
    // visible crop. This is needed for system zoom out animations.
    private fun adjustSizeAndAttachPreview(
        surfacePosition: Rect,
        surfaceView: SurfaceView,
        preview: View,
        fullResView: SystemScaledSubsamplingScaleImageView,
    ) {
        val width = surfacePosition.width()
        val height = surfacePosition.height()
        preview.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        preview.layout(0, 0, width, height)

        fullResView.setSurfaceSize(Point(width, height))
        surfaceView.attachView(fullResView, width, height)
    }

    private const val TAG = "StaticWallpaperPreviewBinder"
}
