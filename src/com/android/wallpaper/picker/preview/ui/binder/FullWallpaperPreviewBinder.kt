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

import android.content.Context
import android.graphics.Point
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.Transition
import androidx.transition.doOnEnd
import com.android.wallpaper.R
import com.android.wallpaper.picker.TouchForwardingLayout
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.shared.model.CropSizeModel
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.picker.preview.ui.util.SubsamplingScaleImageViewUtil.setOnNewCropListener
import com.android.wallpaper.picker.preview.ui.util.SurfaceViewUtil
import com.android.wallpaper.picker.preview.ui.util.SurfaceViewUtil.attachView
import com.android.wallpaper.picker.preview.ui.view.FullPreviewFrameLayout
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.WallpaperCropUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils.shouldEnforceSingleEngine
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.lang.Integer.min
import kotlin.math.max
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Binds wallpaper preview surface view and its view models. */
object FullWallpaperPreviewBinder {

    fun bind(
        applicationContext: Context,
        view: View,
        viewModel: WallpaperPreviewViewModel,
        transition: Transition?,
        displayUtils: DisplayUtils,
        lifecycleOwner: LifecycleOwner,
    ) {
        val wallpaperPreviewCrop: FullPreviewFrameLayout =
            view.requireViewById(R.id.wallpaper_preview_crop)
        val previewCard: CardView = view.requireViewById(R.id.preview_card)
        var transitionDisposableHandle: DisposableHandle? = null
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullWallpaper.collect { (_, config, _) ->
                    val currentSize = displayUtils.getRealSize(checkNotNull(view.context.display))
                    val targetSize = config.displaySize
                    wallpaperPreviewCrop.setCurrentAndTargetDisplaySize(
                        currentSize,
                        targetSize,
                    )
                    val setFinalPreviewCardRadius = {
                        if (targetSize == currentSize) previewCard.radius = 0f
                    }
                    if (transition == null) {
                        setFinalPreviewCardRadius()
                    } else {
                        transitionDisposableHandle?.dispose()
                        val listener = transition.doOnEnd { setFinalPreviewCardRadius() }
                        transitionDisposableHandle = DisposableHandle {
                            listener.let { transition.removeListener(it) }
                        }
                    }
                }
            }
            transitionDisposableHandle?.dispose()
        }
        val surfaceView: SurfaceView = view.requireViewById(R.id.wallpaper_surface)
        val surfaceTouchForwardingLayout: TouchForwardingLayout =
            view.requireViewById(R.id.touch_forwarding_layout)

        var surfaceCallback: SurfaceViewUtil.SurfaceCallback? = null
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                surfaceCallback =
                    bindSurface(
                        applicationContext = applicationContext,
                        surfaceView = surfaceView,
                        surfaceTouchForwardingLayout = surfaceTouchForwardingLayout,
                        viewModel = viewModel,
                        lifecycleOwner = lifecycleOwner,
                    )
                surfaceView.setZOrderMediaOverlay(true)
                surfaceView.holder.addCallback(surfaceCallback)
            }
            // When OnDestroy, release the surface
            surfaceCallback?.let {
                surfaceView.holder.removeCallback(it)
                surfaceCallback = null
            }
        }
    }

    /**
     * Create a surface callback that binds the surface when surface created. Note that we return
     * the surface callback reference so that we can remove the callback from the surface when the
     * screen is destroyed.
     */
    private fun bindSurface(
        applicationContext: Context,
        surfaceView: SurfaceView,
        surfaceTouchForwardingLayout: TouchForwardingLayout,
        viewModel: WallpaperPreviewViewModel,
        lifecycleOwner: LifecycleOwner,
    ): SurfaceViewUtil.SurfaceCallback {
        return object : SurfaceViewUtil.SurfaceCallback {

            var job: Job? = null

            override fun surfaceCreated(holder: SurfaceHolder) {
                job =
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.fullWallpaper.collect {
                            (wallpaper, config, allowUserCropping, whichPreview) ->
                            if (wallpaper is WallpaperModel.LiveWallpaperModel) {
                                WallpaperConnectionUtils.connect(
                                    applicationContext,
                                    wallpaper,
                                    whichPreview,
                                    config.screen.toFlag(),
                                    surfaceView,
                                    WallpaperConnectionUtils.EngineRenderingConfig(
                                        wallpaper.shouldEnforceSingleEngine(),
                                        config.deviceDisplayType,
                                        viewModel.smallerDisplaySize,
                                        config.displaySize,
                                    )
                                )
                            } else if (wallpaper is WallpaperModel.StaticWallpaperModel) {
                                val preview =
                                    LayoutInflater.from(applicationContext)
                                        .inflate(R.layout.fullscreen_wallpaper_preview, null)
                                surfaceView.attachView(preview)
                                val fullResImageView =
                                    preview.requireViewById<SubsamplingScaleImageView>(
                                        R.id.full_res_image
                                    )
                                fullResImageView.doOnLayout {
                                    val imageSize =
                                        Point(fullResImageView.width, fullResImageView.height)
                                    val cropImageSize =
                                        WallpaperCropUtils.calculateCropSurfaceSize(
                                            applicationContext.resources,
                                            max(imageSize.x, imageSize.y),
                                            min(imageSize.x, imageSize.y),
                                            imageSize.x,
                                            imageSize.y
                                        )
                                    fullResImageView.setOnNewCropListener { crop, zoom ->
                                        viewModel.staticWallpaperPreviewViewModel
                                            .fullPreviewCropModels[config.displaySize] =
                                            FullPreviewCropModel(
                                                cropHint = crop,
                                                cropSizeModel =
                                                    CropSizeModel(
                                                        wallpaperZoom = zoom,
                                                        hostViewSize = imageSize,
                                                        cropViewSize = cropImageSize,
                                                    ),
                                            )
                                    }
                                }
                                val lowResImageView =
                                    preview.requireViewById<ImageView>(R.id.low_res_image)

                                // We do not allow users to pinch to crop if it is a
                                // downloadable wallpaper.
                                if (allowUserCropping) {
                                    surfaceTouchForwardingLayout.initTouchForwarding(
                                        fullResImageView
                                    )
                                }

                                // Bind static wallpaper
                                StaticWallpaperPreviewBinder.bind(
                                    lowResImageView,
                                    fullResImageView,
                                    viewModel.staticWallpaperPreviewViewModel,
                                    config.displaySize,
                                    lifecycleOwner,
                                )
                            }
                        }
                    }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                job?.cancel()
                job = null
                // Note that we disconnect wallpaper connection for live wallpapers in
                // WallpaperPreviewActivity's onDestroy().
                // This is to reduce multiple times of connecting and disconnecting live
                // wallpaper services, when going back and forth small and full preview.
            }
        }
    }

    private fun TouchForwardingLayout.initTouchForwarding(targetView: View) {
        setForwardingEnabled(true)
        setTargetView(targetView)
    }
}
