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

package com.android.wallpaper.picker.common.preview.ui.binder

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
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.common.preview.ui.viewmodel.BasePreviewViewModel
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.util.SurfaceViewUtils
import com.android.wallpaper.util.SurfaceViewUtils.attachView
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils.shouldEnforceSingleEngine
import com.android.wallpaper.util.wallpaperconnection.WallpaperEngineConnection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Bind the [SurfaceView] with [BasePreviewViewModel] for rendering static or live wallpaper
 * preview, with regard to its underlying [WallpaperModel].
 */
// Based on SmallWallpaperPreviewBinder, mostly unchanged, except with LoadingAnimationBinding
// removed. Also we enable a screen to be defined during binding rather than reading from
// viewModel.isViewAsHome. In addition the call to WallpaperConnectionUtils.disconnectAllServices at
// the end of the static wallpaper binding is removed since it interferes with previewing one live
// and one static wallpaper side by side, but should be re-visited when integrating into
// WallpaperPreviewActivity for the cinematic wallpaper toggle case.
object WallpaperPreviewBinder {
    fun bind(
        applicationContext: Context,
        surfaceView: SurfaceView,
        viewModel: BasePreviewViewModel,
        screen: Screen,
        displaySize: Point,
        deviceDisplayType: DeviceDisplayType,
        viewLifecycleOwner: LifecycleOwner,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
    ) {
        var surfaceCallback: SurfaceViewUtils.SurfaceCallback? = null
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                surfaceCallback =
                    bindSurface(
                        applicationContext = applicationContext,
                        surfaceView = surfaceView,
                        viewModel = viewModel,
                        screen = screen,
                        deviceDisplayType = deviceDisplayType,
                        displaySize = displaySize,
                        lifecycleOwner = viewLifecycleOwner,
                        isFirstBindingDeferred = isFirstBindingDeferred,
                    )
                surfaceView.setZOrderMediaOverlay(true)
                surfaceCallback?.let { surfaceView.holder.addCallback(it) }
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
        viewModel: BasePreviewViewModel,
        screen: Screen,
        deviceDisplayType: DeviceDisplayType,
        displaySize: Point,
        lifecycleOwner: LifecycleOwner,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
    ): SurfaceViewUtils.SurfaceCallback {

        return object : SurfaceViewUtils.SurfaceCallback {

            var job: Job? = null

            override fun surfaceCreated(holder: SurfaceHolder) {
                job =
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.wallpapersAndWhichPreview.collect { (wallpapers, whichPreview) ->
                            val wallpaper =
                                if (screen == Screen.HOME_SCREEN) wallpapers.homeWallpaper
                                else wallpapers.lockWallpaper ?: wallpapers.homeWallpaper
                            if (wallpaper is WallpaperModel.LiveWallpaperModel) {
                                val engineRenderingConfig =
                                    WallpaperConnectionUtils.EngineRenderingConfig(
                                        wallpaper.shouldEnforceSingleEngine(),
                                        deviceDisplayType = deviceDisplayType,
                                        viewModel.smallerDisplaySize,
                                        viewModel.wallpaperDisplaySize.value,
                                    )
                                val listener =
                                    object :
                                        WallpaperEngineConnection.WallpaperEngineConnectionListener {
                                        override fun onWallpaperColorsChanged(
                                            colors: WallpaperColors?,
                                            displayId: Int
                                        ) {
                                            viewModel.setWallpaperConnectionColors(
                                                WallpaperColorsModel.Loaded(colors)
                                            )
                                        }
                                    }
                                WallpaperConnectionUtils.connect(
                                    applicationContext,
                                    wallpaper,
                                    whichPreview,
                                    screen.toFlag(),
                                    surfaceView,
                                    engineRenderingConfig,
                                    isFirstBindingDeferred,
                                    listener,
                                )
                            } else if (wallpaper is WallpaperModel.StaticWallpaperModel) {
                                val staticPreviewView =
                                    LayoutInflater.from(applicationContext)
                                        .inflate(R.layout.fullscreen_wallpaper_preview, null)
                                // surfaceView.width and surfaceFrame.width here can be different,
                                // one represents the size of the view and the other represents the
                                // size of the surface. When setting a view to the surface host,
                                // we want to set it based on the surface's size not the view's size
                                val surfacePosition = surfaceView.holder.surfaceFrame
                                surfaceView.attachView(
                                    staticPreviewView,
                                    surfacePosition.width(),
                                    surfacePosition.height()
                                )
                                // Bind static wallpaper
                                StaticPreviewBinder.bind(
                                    lowResImageView =
                                        staticPreviewView.requireViewById(R.id.low_res_image),
                                    fullResImageView =
                                        staticPreviewView.requireViewById(R.id.full_res_image),
                                    viewModel =
                                        if (
                                            screen == Screen.LOCK_SCREEN &&
                                                wallpapers.lockWallpaper != null
                                        ) {
                                            // Only if home and lock screen are different, use lock
                                            // view model, otherwise, re-use home view model for
                                            // lock.
                                            viewModel.staticLockWallpaperPreviewViewModel
                                        } else {
                                            viewModel.staticHomeWallpaperPreviewViewModel
                                        },
                                    displaySize = displaySize,
                                    parentCoroutineScope = this,
                                )
                                // TODO (b/348462236): investigate cinematic wallpaper toggle case
                                // Previously all live wallpaper services are shut down to enable
                                // static photos wallpaper to show up when cinematic effect is
                                // toggled off, using WallpaperConnectionUtils.disconnectAllServices
                                // This cannot work when previewing current wallpaper, and one
                                // wallpaper is live and the other is static--it causes live
                                // wallpaper to black screen occasionally.
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
}
