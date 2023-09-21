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
import android.view.SurfaceControlViewHost
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.wallpaper.R
import com.android.wallpaper.dispatchers.MainDispatcher
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.module.WallpaperPersister
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils.setUpSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object WallpaperPreviewBinder {
    fun bind(
        applicationContext: Context,
        wallpaperSurface: SurfaceView,
        viewModel: WallpaperPreviewViewModel,
        @MainDispatcher mainScope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        isRtl: Boolean,
    ) {
        wallpaperSurface.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    val wallpaper = viewModel.editingWallpaper
                    if (wallpaper is LiveWallpaperInfo) {
                        lifecycleOwner.lifecycleScope.launch {
                            wallpaperSurface.setUpSurface(applicationContext)
                            WallpaperConnectionUtils.connect(
                                applicationContext,
                                mainScope,
                                wallpaper.wallpaperComponent,
                                // TODO b/301088528(giolin): Pass correspondent
                                //                           destination for live
                                //                           wallpaper preview
                                WallpaperPersister.DEST_LOCK_SCREEN,
                                wallpaperSurface,
                            )
                        }
                    } else {
                        val staticWallpaperPreview =
                            LayoutInflater.from(applicationContext)
                                .inflate(R.layout.fullscreen_wallpaper_preview, null)
                        // We need to attach the ordinary view to a surface view since
                        // we overlay lock screen and home screen UI on top of the wallpaper.
                        attachStaticWallpaperPreviewToSurface(
                            applicationContext,
                            staticWallpaperPreview,
                            wallpaperSurface,
                        )
                        StaticWallpaperPreviewBinder.bind(
                            fullResImageView =
                                staticWallpaperPreview.requireViewById(R.id.full_res_image),
                            lowResImageView =
                                staticWallpaperPreview.requireViewById(R.id.low_res_image),
                            viewModel = viewModel.getStaticWallpaperPreviewViewModel(),
                            lifecycleOwner = lifecycleOwner,
                            isSingleDisplayOrUnfoldedHorizontalHinge =
                                isSingleDisplayOrUnfoldedHorizontalHinge,
                            isRtl,
                        )
                    }
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {}
            }
        )
        // TODO b/300979155(giolin): Clean up surface when no longer needed, e.g. onDestroyed
    }

    private fun attachStaticWallpaperPreviewToSurface(
        applicationContext: Context,
        preview: View,
        surface: SurfaceView
    ) {
        val width = surface.width
        val height = surface.height
        preview.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        preview.layout(0, 0, width, height)
        val host = SurfaceControlViewHost(applicationContext, surface.display, surface.hostToken)
        host.setView(
            preview,
            preview.width,
            preview.height,
        )
        host.surfacePackage?.let { surface.setChildSurfacePackage(it) }
    }
}
