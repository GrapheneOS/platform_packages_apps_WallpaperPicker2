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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
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
import com.android.wallpaper.util.RtlUtils.isRtl
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
        savedInstanceState: Bundle?,
        isFirstBinding: Boolean,
        onWallpaperLoaded: ((Boolean) -> Unit)? = null,
    ) {
        val wallpaperPreviewCrop: FullPreviewFrameLayout =
            view.requireViewById(R.id.wallpaper_preview_crop)
        val previewCard: CardView = view.requireViewById(R.id.preview_card)
        val scrimView: View = view.requireViewById(R.id.preview_scrim)
        var transitionDisposableHandle: DisposableHandle? = null
        val mediumAnimTimeMs =
            view.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullWallpaper.collect { (_, _, displaySize, _) ->
                    val currentSize = displayUtils.getRealSize(checkNotNull(view.context.display))
                    wallpaperPreviewCrop.setCurrentAndTargetDisplaySize(
                        currentSize,
                        displaySize,
                    )

                    val setFinalPreviewCardRadiusAndEndLoading = { isWallpaperFullScreen: Boolean ->
                        if (isWallpaperFullScreen) {
                            previewCard.radius = 0f
                        }
                        scrimView.isVisible = isWallpaperFullScreen
                        onWallpaperLoaded?.invoke(isWallpaperFullScreen)
                    }
                    val isPreviewingFullScreen = displaySize == currentSize
                    if (transition == null || savedInstanceState != null) {
                        setFinalPreviewCardRadiusAndEndLoading(isPreviewingFullScreen)
                    } else {
                        transitionDisposableHandle?.dispose()
                        val listener =
                            object : TransitionListenerAdapter() {
                                override fun onTransitionStart(transition: Transition) {
                                    super.onTransitionStart(transition)
                                    if (isPreviewingFullScreen) {
                                        scrimView.isVisible = true
                                        scrimView.alpha = 0f
                                        scrimView
                                            .animate()
                                            .alpha(1f)
                                            .setDuration(mediumAnimTimeMs)
                                            .start()
                                    }
                                }

                                override fun onTransitionEnd(transition: Transition) {
                                    super.onTransitionEnd(transition)
                                    setFinalPreviewCardRadiusAndEndLoading(isPreviewingFullScreen)
                                }
                            }
                        transition.addListener(listener)
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

        if (displayUtils.hasMultiInternalDisplays()) {
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.fullPreviewConfigViewModel.collect { fullPreviewConfigViewModel ->
                        val deviceDisplayType = fullPreviewConfigViewModel?.deviceDisplayType
                        val descriptionResourceId =
                            when (deviceDisplayType) {
                                DeviceDisplayType.FOLDED -> R.string.folded_device_state_description
                                else -> R.string.unfolded_device_state_description
                            }
                        val descriptionString =
                            surfaceTouchForwardingLayout.context.getString(descriptionResourceId)
                        surfaceTouchForwardingLayout.contentDescription =
                            surfaceTouchForwardingLayout.context.getString(
                                R.string.preview_screen_description_editable,
                                descriptionString
                            )
                    }
                }
            }
        } else {
            surfaceTouchForwardingLayout.contentDescription =
                surfaceTouchForwardingLayout.context.getString(
                    R.string.preview_screen_description_editable,
                    ""
                )
        }

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
                        isFirstBinding = isFirstBinding,
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
        isFirstBinding: Boolean,
    ): SurfaceViewUtil.SurfaceCallback {
        return object : SurfaceViewUtil.SurfaceCallback {

            var job: Job? = null
            var surfaceOrigWidth: Int? = null
            var surfaceOrigHeight: Int? = null

            // Suppress lint warning for setting on touch listener to a live wallpaper surface view.
            // This is because the touch effect on a live wallpaper is purely visual, instead of
            // functional. The effect can be different for different live wallpapers.
            @SuppressLint("ClickableViewAccessibility")
            override fun surfaceCreated(holder: SurfaceHolder) {
                job =
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.fullWallpaper.collect {
                            (wallpaper, config, displaySize, allowUserCropping, whichPreview) ->
                            if (wallpaper is WallpaperModel.LiveWallpaperModel) {
                                val engineRenderingConfig =
                                    WallpaperConnectionUtils.EngineRenderingConfig(
                                        wallpaper.shouldEnforceSingleEngine(),
                                        config.deviceDisplayType,
                                        viewModel.smallerDisplaySize,
                                        displaySize,
                                    )
                                WallpaperConnectionUtils.connect(
                                    applicationContext,
                                    wallpaper,
                                    whichPreview,
                                    viewModel.getWallpaperPreviewSource().toFlag(),
                                    surfaceView,
                                    engineRenderingConfig,
                                    isFirstBinding,
                                )
                                surfaceTouchForwardingLayout.initTouchForwarding(surfaceView)
                                surfaceView.setOnTouchListener { _, event ->
                                    lifecycleOwner.lifecycleScope.launch {
                                        WallpaperConnectionUtils.dispatchTouchEvent(
                                            wallpaper,
                                            engineRenderingConfig,
                                            event,
                                        )
                                    }
                                    false
                                }
                            } else if (wallpaper is WallpaperModel.StaticWallpaperModel) {
                                val preview =
                                    LayoutInflater.from(applicationContext)
                                        .inflate(R.layout.fullscreen_wallpaper_preview, null)
                                adjustSizeAndAttachPreview(
                                    applicationContext,
                                    surfaceOrigWidth
                                        ?: surfaceView.width.also { surfaceOrigWidth = it },
                                    surfaceOrigHeight
                                        ?: surfaceView.height.also { surfaceOrigHeight = it },
                                    surfaceView,
                                    preview,
                                )

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
                                            .fullPreviewCropModels[displaySize] =
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
                                    lowResImageView = lowResImageView,
                                    fullResImageView = fullResImageView,
                                    viewModel = viewModel.staticWallpaperPreviewViewModel,
                                    displaySize = displaySize,
                                    parentCoroutineScope = this,
                                    isFullScreen = true,
                                )
                            }
                        }
                    }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                job?.cancel()
                job = null
                // Clean up surface view's on touche listener
                surfaceTouchForwardingLayout.removeTouchForwarding()
                surfaceView.setOnTouchListener(null)
                // Note that we disconnect wallpaper connection for live wallpapers in
                // WallpaperPreviewActivity's onDestroy().
                // This is to reduce multiple times of connecting and disconnecting live
                // wallpaper services, when going back and forth small and full preview.
            }
        }
    }

    // When showing full screen, we set the parent SurfaceView to be bigger than the image by N
    // percent (usually 10%) as given by getSystemWallpaperMaximumScale. This ensures that no matter
    // what scale and pan is set by the user, at least N% of the source image in the preview will be
    // preserved around the visible crop. This is needed for system zoom out animations.
    private fun adjustSizeAndAttachPreview(
        applicationContext: Context,
        origWidth: Int,
        origHeight: Int,
        surfaceView: SurfaceView,
        preview: View,
    ) {
        val scale = WallpaperCropUtils.getSystemWallpaperMaximumScale(applicationContext)

        val width = (origWidth * scale).toInt()
        val height = (origHeight * scale).toInt()
        val left =
            ((origWidth - width) / 2).let {
                if (isRtl(applicationContext)) {
                    -it
                } else {
                    it
                }
            }
        val top = (origHeight - height) / 2

        val params = surfaceView.layoutParams
        params.width = width
        params.height = height
        surfaceView.x = left.toFloat()
        surfaceView.y = top.toFloat()
        surfaceView.layoutParams = params
        surfaceView.requestLayout()

        preview.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        preview.layout(0, 0, width, height)

        surfaceView.attachView(preview, width, height)
    }

    private fun TouchForwardingLayout.initTouchForwarding(targetView: View) {
        // Make sure the touch forwarding layout same size of the target view
        layoutParams = FrameLayout.LayoutParams(targetView.width, targetView.height, Gravity.CENTER)
        setForwardingEnabled(true)
        setTargetView(targetView)
    }

    private fun TouchForwardingLayout.removeTouchForwarding() {
        setForwardingEnabled(false)
        setTargetView(null)
    }
}
