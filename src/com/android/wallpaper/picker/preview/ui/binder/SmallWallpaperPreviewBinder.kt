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
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.wallpaper.R
import com.android.wallpaper.dispatchers.MainDispatcher
import com.android.wallpaper.model.wallpaper.ScreenOrientation
import com.android.wallpaper.model.wallpaper.WallpaperModel
import com.android.wallpaper.module.CustomizationSections.Screen
import com.android.wallpaper.picker.preview.ui.util.SurfaceViewUtil
import com.android.wallpaper.picker.preview.ui.util.SurfaceViewUtil.attachView
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import kotlinx.coroutines.CoroutineScope
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
        screen: Screen,
        screenOrientation: ScreenOrientation,
        applicationContext: Context,
        @MainDispatcher mainScope: CoroutineScope,
        viewLifecycleOwner: LifecycleOwner,
    ) {
        var job: Job? = null
        surface.setZOrderMediaOverlay(true)
        surface.holder.addCallback(
            object : SurfaceViewUtil.SurfaceCallback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    job =
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.wallpaper.collect { wallpaper ->
                                if (wallpaper is WallpaperModel.LiveWallpaperModel) {
                                    WallpaperConnectionUtils.connect(
                                        applicationContext,
                                        mainScope,
                                        wallpaper.liveWallpaperData.systemWallpaperInfo,
                                        screen.toFlag(),
                                        surface,
                                    )
                                } else if (wallpaper is WallpaperModel.StaticWallpaperModel) {
                                    val staticPreviewView =
                                        LayoutInflater.from(applicationContext)
                                            .inflate(R.layout.fullscreen_wallpaper_preview, null)
                                    surface.attachView(staticPreviewView)
                                    // Bind static wallpaper
                                    StaticWallpaperPreviewBinder.bind(
                                        staticPreviewView.requireViewById(R.id.low_res_image),
                                        staticPreviewView.requireViewById(R.id.full_res_image),
                                        viewModel.getStaticWallpaperPreviewViewModel(),
                                        screenOrientation,
                                        viewLifecycleOwner,
                                    )
                                }
                            }
                        }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    job?.cancel()
                }
            }
        )
        // TODO (b/300979155): Clean up surface when no longer needed, e.g. onDestroyed
    }
}
