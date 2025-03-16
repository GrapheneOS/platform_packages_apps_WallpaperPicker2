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

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
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
import com.android.wallpaper.picker.preview.ui.util.FullResImageViewUtil.getCropRect
import com.android.wallpaper.picker.preview.ui.util.SubsamplingScaleImageViewUtil.setOnNewCropListener
import com.android.wallpaper.picker.preview.ui.view.FullPreviewFrameLayout
import com.android.wallpaper.picker.preview.ui.view.SystemScaledSubsamplingScaleImageView
import com.android.wallpaper.picker.preview.ui.viewmodel.StaticWallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.SurfaceViewUtils
import com.android.wallpaper.util.WallpaperCropUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils.Companion.shouldEnforceSingleEngine
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.lang.Integer.min
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Binds wallpaper preview surface view and its view models. */
object FullWallpaperPreviewBinder {

    fun bind(
        applicationContext: Context,
        view: View,
        viewModel: WallpaperPreviewViewModel,
        transition: Transition?,
        displayUtils: DisplayUtils,
        mainScope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        savedInstanceState: Bundle?,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
        onWallpaperLoaded: ((Boolean) -> Unit)? = null,
    ) {
        val surfaceView: SurfaceView = view.requireViewById(R.id.wallpaper_surface)
        val wallpaperPreviewCrop: FullPreviewFrameLayout =
            view.requireViewById(R.id.wallpaper_preview_crop)
        val previewCard: CardView = view.requireViewById(R.id.preview_card)
        val scrimView: View = view.requireViewById(R.id.preview_scrim)
        var transitionDisposableHandle: DisposableHandle? = null
        val mediumAnimTimeMs =
            view.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        val setFinalPreviewCardRadiusAndEndLoading = { isWallpaperFullScreen: Boolean ->
            if (isWallpaperFullScreen) {
                previewCard.radius = 0f
            }
            surfaceView.cornerRadius = previewCard.radius
            scrimView.isVisible = isWallpaperFullScreen
            onWallpaperLoaded?.invoke(isWallpaperFullScreen)
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullWallpaper.collect { (_, _, displaySize, _) ->
                    val currentSize = displayUtils.getRealSize(checkNotNull(view.context.display))
                    wallpaperPreviewCrop.setCurrentAndTargetDisplaySize(currentSize, displaySize)
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
                                    transitionDisposableHandle?.dispose()
                                    transitionDisposableHandle = null
                                }
                            }
                        transition.addListener(listener)
                        transitionDisposableHandle = DisposableHandle {
                            listener.let { transition.removeListener(it) }
                        }
                    }
                }
            }
            setFinalPreviewCardRadiusAndEndLoading(false)
            transitionDisposableHandle?.dispose()
            transitionDisposableHandle = null
        }
        val surfaceTouchForwardingLayout: TouchForwardingLayout =
            view.requireViewById(R.id.touch_forwarding_layout)

        if (displayUtils.hasMultiInternalDisplays()) {
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    combine(viewModel.fullWallpaper, viewModel.fullPreviewConfigViewModel) {
                            fullWallpaper,
                            fullWallpaperPreviewConfig ->
                            fullWallpaper to fullWallpaperPreviewConfig
                        }
                        .collect { (fullWallpaper, fullWallpaperPreviewConfig) ->
                            val deviceDisplayType = fullWallpaperPreviewConfig?.deviceDisplayType
                            val descriptionResourceId =
                                when (deviceDisplayType) {
                                    DeviceDisplayType.FOLDED ->
                                        R.string.folded_device_state_description
                                    else -> R.string.unfolded_device_state_description
                                }
                            val descriptionString =
                                surfaceTouchForwardingLayout.context.getString(
                                    descriptionResourceId
                                )
                            surfaceTouchForwardingLayout.contentDescription =
                                surfaceTouchForwardingLayout.context.getString(
                                    R.string.preview_screen_description_non_editable,
                                    descriptionString,
                                )
                        }
                }
            }
        } else {
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.fullWallpaper.collect { fullWallpaper ->
                        val isPreviewEditable =
                            fullWallpaper.wallpaper is WallpaperModel.StaticWallpaperModel
                        surfaceTouchForwardingLayout.contentDescription =
                            surfaceTouchForwardingLayout.context.getString(
                                R.string.preview_screen_description_non_editable,
                                "",
                            )
                    }
                }
            }
        }

        var surfaceCallback: SurfaceViewUtils.SurfaceCallback? = null
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                surfaceCallback =
                    bindSurface(
                        applicationContext = applicationContext,
                        surfaceView = surfaceView,
                        surfaceTouchForwardingLayout = surfaceTouchForwardingLayout,
                        viewModel = viewModel,
                        mainScope = mainScope,
                        lifecycleOwner = lifecycleOwner,
                        wallpaperConnectionUtils = wallpaperConnectionUtils,
                        isFirstBindingDeferred = isFirstBindingDeferred,
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

    private fun addZoomAccessibilityActions(
        viewLayout: TouchForwardingLayout,
        viewModel: StaticWallpaperPreviewViewModel,
        view: SubsamplingScaleImageView,
        applicationContext: Context,
        onZoomChanged: ((crop: android.graphics.Rect, zoom: Float) -> Unit)? = null,
    ) {
        ViewCompat.addAccessibilityAction(
            viewLayout,
            applicationContext.getString(R.string.action_zoom_in),
        ) { _, _ ->
            val zoomFactor = 1.5f // Enables a 50% zoom
            val maxAvailableScale = viewModel.scaleAndCenter?.maxScale
            val minAvailableScale = viewModel.scaleAndCenter?.minScale
            val currentScale = view.scale
            val currentCenter = viewModel.scaleAndCenter?.center

            if (maxAvailableScale != null && minAvailableScale != null) {
                val newScale = currentScale * zoomFactor
                if (newScale > currentScale && newScale <= maxAvailableScale) {
                    view.setScaleAndCenter(newScale, currentCenter)
                    val zoomPercentage = ((newScale / minAvailableScale - 1) * 100).roundToInt()
                    val newDescription =
                        applicationContext.getString(R.string.zoomed_to_percent, zoomPercentage)
                    viewLayout.contentDescription = newDescription
                    //  The `setScaleAndCenter` method requires time to take effect.
                    //  To ensure accurate crop rectangle retrieval, we use `post` to execute the
                    //  `getCropRect` call after the scaling is complete. This prevents obtaining
                    //  an outdated crop rectangle.
                    view.post {
                        val crop = view.getCropRect()
                        onZoomChanged?.invoke(crop, newScale)
                    }
                } else {
                    viewLayout.contentDescription =
                        applicationContext.getString(R.string.max_zoom_percentage)
                }
            }
            true
        }

        ViewCompat.addAccessibilityAction(
            viewLayout,
            applicationContext.getString(R.string.action_zoom_out),
        ) { _, _ ->
            val currentScale = view.scale
            val minAvailable = viewModel.scaleAndCenter?.minScale
            val currentCenter = viewModel.scaleAndCenter?.center
            val zoomFactor = 0.6666667f // 33%

            if (minAvailable != null) {
                val newScaleValue = currentScale * zoomFactor
                if (newScaleValue < currentScale && newScaleValue >= minAvailable) {
                    view.setScaleAndCenter(newScaleValue, currentCenter)
                    val zoomPercentage = ((newScaleValue / minAvailable - 1) * 100).roundToInt()
                    val newDescription =
                        applicationContext.getString(R.string.zoomed_to_percent, zoomPercentage)
                    viewLayout.contentDescription = newDescription
                    //  The `setScaleAndCenter` method requires time to take effect.
                    //  To ensure accurate crop rectangle retrieval, we use `post` to execute the
                    //  `getCropRect` call after the scaling is complete. This prevents obtaining
                    //  an outdated crop rectangle.
                    view.post {
                        val crop = view.getCropRect()
                        onZoomChanged?.invoke(crop, newScaleValue)
                    }
                } else {
                    viewLayout.contentDescription =
                        applicationContext.getString(R.string.min_zoom_percentage)
                }
            }
            true
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
        mainScope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
    ): SurfaceViewUtils.SurfaceCallback {
        return object : SurfaceViewUtils.SurfaceCallback {

            var job: Job? = null

            // Suppress lint warning for setting on touch listener to a live wallpaper surface view.
            // This is because the touch effect on a live wallpaper is purely visual, instead of
            // functional. The effect can be different for different live wallpapers.
            @SuppressLint("ClickableViewAccessibility")
            override fun surfaceCreated(holder: SurfaceHolder) {
                job =
                    // Ensure the wallpaper connection is connected / disconnected in [mainScope].
                    mainScope.launch {
                        viewModel.fullWallpaper.collect {
                            (wallpaper, config, displaySize, allowUserCropping, whichPreview) ->
                            if (wallpaper is WallpaperModel.LiveWallpaperModel) {
                                val engineRenderingConfig =
                                    WallpaperConnectionUtils.Companion.EngineRenderingConfig(
                                        wallpaper.shouldEnforceSingleEngine(),
                                        config.deviceDisplayType,
                                        viewModel.smallerDisplaySize,
                                        displaySize,
                                    )
                                wallpaperConnectionUtils.connect(
                                    applicationContext,
                                    wallpaper,
                                    whichPreview,
                                    config.screen.toFlag(),
                                    surfaceView,
                                    engineRenderingConfig,
                                    isFirstBindingDeferred,
                                )
                                if (!isAccessibilityEnabled(applicationContext)) {
                                    surfaceTouchForwardingLayout.initTouchForwarding(surfaceView)
                                    surfaceView.setOnTouchListener { _, event ->
                                        lifecycleOwner.lifecycleScope.launch {
                                            wallpaperConnectionUtils.dispatchTouchEvent(
                                                wallpaper,
                                                engineRenderingConfig,
                                                config.screen.toFlag(),
                                                event,
                                            )
                                        }
                                        false
                                    }
                                }
                            } else if (wallpaper is WallpaperModel.StaticWallpaperModel) {
                                val preview =
                                    LayoutInflater.from(applicationContext)
                                        .inflate(R.layout.fullscreen_wallpaper_preview, null)

                                val fullResImageView =
                                    preview.requireViewById<SystemScaledSubsamplingScaleImageView>(
                                        R.id.full_res_image
                                    )
                                val onZoomChanged:
                                    ((crop: android.graphics.Rect, zoom: Float) -> Unit) =
                                    { crop, zoom ->
                                        val imageSize =
                                            Point(fullResImageView.width, fullResImageView.height)
                                        val cropImageSize =
                                            WallpaperCropUtils.calculateCropSurfaceSize(
                                                applicationContext.resources,
                                                max(imageSize.x, imageSize.y),
                                                min(imageSize.x, imageSize.y),
                                                imageSize.x,
                                                imageSize.y,
                                            )
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

                                addZoomAccessibilityActions(
                                    surfaceTouchForwardingLayout,
                                    viewModel.staticWallpaperPreviewViewModel,
                                    fullResImageView,
                                    applicationContext,
                                    onZoomChanged,
                                )
                                // Bind static wallpaper
                                StaticWallpaperPreviewBinder.bind(
                                    staticPreviewView = preview,
                                    wallpaperSurface = surfaceView,
                                    viewModel = viewModel.staticWallpaperPreviewViewModel,
                                    displaySize = displaySize,
                                    parentCoroutineScope = this,
                                    isFullScreen = true,
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
                                            imageSize.y,
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
                                // We do not allow users to pinch to crop if it is a
                                // downloadable wallpaper.
                                if (
                                    allowUserCropping && !isAccessibilityEnabled(applicationContext)
                                ) {
                                    surfaceTouchForwardingLayout.initTouchForwarding(
                                        fullResImageView
                                    )
                                }
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

    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.isNotEmpty()
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
