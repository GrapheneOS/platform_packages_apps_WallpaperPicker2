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

import android.app.WallpaperColors
import android.content.Context
import android.graphics.Point
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.SurfaceViewUtils
import com.android.wallpaper.util.SurfaceViewUtils.attachView
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils.shouldEnforceSingleEngine
import com.android.wallpaper.util.wallpaperconnection.WallpaperEngineConnection.WallpaperEngineConnectionListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Bind the [SurfaceView] with [WallpaperPreviewViewModel] for rendering static or live wallpaper
 * preview, with regard to its underlying [WallpaperModel].
 */
object SmallWallpaperPreviewBinder {
    /**
     * @param onFullResImageViewCreated This callback is only used when the wallpaperModel is a
     *   [WallpaperModel.StaticWallpaperModel]. [FullWallpaperPreviewBinder] needs the callback to
     *   further delegate the touch events and set the state change listener.
     */
    fun bind(
        surface: SurfaceView,
        viewModel: WallpaperPreviewViewModel,
        displaySize: Point,
        applicationContext: Context,
        viewLifecycleOwner: LifecycleOwner,
        deviceDisplayType: DeviceDisplayType,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
    ) {
        var surfaceCallback: SurfaceViewUtils.SurfaceCallback? = null
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                surfaceCallback =
                    bindSurface(
                        applicationContext = applicationContext,
                        surface = surface,
                        viewModel = viewModel,
                        deviceDisplayType = deviceDisplayType,
                        displaySize = displaySize,
                        lifecycleOwner = viewLifecycleOwner,
                        isFirstBindingDeferred,
                    )
                surface.setZOrderMediaOverlay(true)
                surfaceCallback?.let { surface.holder.addCallback(it) }
            }
            // When OnDestroy, release the surface
            surfaceCallback?.let {
                surface.holder.removeCallback(it)
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
        surface: SurfaceView,
        viewModel: WallpaperPreviewViewModel,
        deviceDisplayType: DeviceDisplayType,
        displaySize: Point,
        lifecycleOwner: LifecycleOwner,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
    ): SurfaceViewUtils.SurfaceCallback {

        return object : SurfaceViewUtils.SurfaceCallback {

            var job: Job? = null
            var loadingAnimationBinding: PreviewEffectsLoadingBinder.Binding? = null

            override fun surfaceCreated(holder: SurfaceHolder) {
                job =
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.smallWallpaper.collect { (wallpaper, whichPreview) ->
                            if (wallpaper is WallpaperModel.LiveWallpaperModel) {
                                WallpaperConnectionUtils.connect(
                                    applicationContext,
                                    wallpaper,
                                    whichPreview,
                                    viewModel.getWallpaperPreviewSource().toFlag(),
                                    surface,
                                    WallpaperConnectionUtils.EngineRenderingConfig(
                                        wallpaper.shouldEnforceSingleEngine(),
                                        deviceDisplayType = deviceDisplayType,
                                        viewModel.smallerDisplaySize,
                                        viewModel.wallpaperDisplaySize.value,
                                    ),
                                    isFirstBindingDeferred,
                                    object : WallpaperEngineConnectionListener {
                                        override fun onWallpaperColorsChanged(
                                            colors: WallpaperColors?,
                                            displayId: Int
                                        ) {
                                            viewModel.setWallpaperConnectionColors(
                                                WallpaperColorsModel.Loaded(colors)
                                            )
                                        }
                                    },
                                )
                            } else if (wallpaper is WallpaperModel.StaticWallpaperModel) {
                                val staticPreviewView =
                                    LayoutInflater.from(applicationContext)
                                        .inflate(R.layout.fullscreen_wallpaper_preview, null)
                                surface.attachView(staticPreviewView)
                                // Bind static wallpaper
                                StaticWallpaperPreviewBinder.bind(
                                    lowResImageView =
                                        staticPreviewView.requireViewById(R.id.low_res_image),
                                    fullResImageView =
                                        staticPreviewView.requireViewById(R.id.full_res_image),
                                    viewModel = viewModel.staticWallpaperPreviewViewModel,
                                    displaySize = displaySize,
                                    parentCoroutineScope = this,
                                )
                                // This is to possibly shut down all live wallpaper services
                                // if they exist; otherwise static wallpaper can not show up.
                                WallpaperConnectionUtils.disconnectAllServices(applicationContext)

                                loadingAnimationBinding =
                                    PreviewEffectsLoadingBinder.bind(
                                        view =
                                            staticPreviewView.requireViewById(R.id.full_res_image),
                                        viewModel = viewModel,
                                        viewLifecycleOwner = lifecycleOwner,
                                    )
                            }
                        }
                    }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                job?.cancel()
                job = null
                loadingAnimationBinding?.destroy()
                loadingAnimationBinding = null
                // Note that we disconnect wallpaper connection for live wallpapers in
                // WallpaperPreviewActivity's onDestroy().
                // This is to reduce multiple times of connecting and disconnecting live
                // wallpaper services, when going back and forth small and full preview.
            }
        }
    }
}
