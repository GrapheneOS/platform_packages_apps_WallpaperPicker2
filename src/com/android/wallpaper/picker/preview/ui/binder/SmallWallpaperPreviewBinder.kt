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
import android.view.SurfaceControlViewHost
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.android.wallpaper.R
import com.android.wallpaper.dispatchers.MainDispatcher
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.CoroutineScope

/** Binds wallpaper [SurfaceView] for small preview. */
object SmallWallpaperPreviewBinder {
    fun bind(
        applicationContext: Context,
        wallpaperSurface: SurfaceView,
        viewModel: WallpaperPreviewViewModel,
        viewLifecycleOwner: LifecycleOwner,
        @MainDispatcher mainScope: CoroutineScope,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        isRtl: Boolean,
        staticPreviewView: View? = null,
    ) {
        wallpaperSurface.setZOrderMediaOverlay(true)
        wallpaperSurface.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    staticPreviewView?.let {
                        val host =
                            SurfaceControlViewHost(
                                wallpaperSurface.context,
                                wallpaperSurface.display,
                                wallpaperSurface.hostToken,
                            )
                        if (it.parent == null) {
                            host.setView(
                                it,
                                wallpaperSurface.width,
                                wallpaperSurface.height,
                            )
                            wallpaperSurface.setChildSurfacePackage(
                                checkNotNull(host.surfacePackage)
                            )
                        }
                    }
                    WallpaperPreviewBinder.bind(
                        applicationContext,
                        isSingleDisplayOrUnfoldedHorizontalHinge,
                        isRtl,
                        mainScope,
                        viewLifecycleOwner,
                        viewModel,
                        wallpaperSurface,
                        staticPreviewView?.requireViewById(R.id.full_res_image),
                        staticPreviewView?.requireViewById(R.id.low_res_image),
                    )
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
        // TODO (b/300979155): Clean up surface when no longer needed, e.g. onDestroyed
    }
}
