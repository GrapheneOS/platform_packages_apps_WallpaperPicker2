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
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.dispatchers.MainDispatcher
import com.android.wallpaper.picker.TouchForwardingLayout
import com.android.wallpaper.picker.preview.ui.view.FullPreviewSurfaceView
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewSurfaceViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Binds wallpaper preview surface view and its view models. */
object FullWallpaperPreviewBinder {

    fun bind(
        applicationContext: Context,
        surfaceView: FullPreviewSurfaceView,
        surfaceTouchForwardingLayout: TouchForwardingLayout,
        surfaceViewModel: FullPreviewSurfaceViewModel,
        previewViewModel: WallpaperPreviewViewModel,
        viewLifecycleOwner: LifecycleOwner,
        @MainDispatcher mainScope: CoroutineScope,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        isRtl: Boolean,
        staticPreviewView: View? = null,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                surfaceView.let { surfaceView ->
                    surfaceView.setCurrentAndTargetDisplaySize(
                        currentSize = surfaceViewModel.currentDisplaySize,
                        targetSize = surfaceViewModel.previewTransitionViewModel.targetDisplaySize,
                    )
                    surfaceView.holder.addCallback(
                        object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                staticPreviewView?.let { previewView ->
                                    val host =
                                        SurfaceControlViewHost(
                                            surfaceView.context,
                                            surfaceView.display,
                                            surfaceView.hostToken,
                                        )
                                    if (previewView.parent == null) {
                                        host.setView(
                                            previewView,
                                            surfaceView.width,
                                            surfaceView.height,
                                        )
                                        surfaceView.setChildSurfacePackage(
                                            checkNotNull(host.surfacePackage)
                                        )
                                        previewView
                                            .requireViewById<SubsamplingScaleImageView>(
                                                R.id.full_res_image
                                            )
                                            .let {
                                                surfaceTouchForwardingLayout.initTouchForwarding(it)
                                            }
                                    }
                                }
                                WallpaperPreviewBinder.bind(
                                    applicationContext,
                                    isSingleDisplayOrUnfoldedHorizontalHinge,
                                    isRtl,
                                    mainScope,
                                    viewLifecycleOwner,
                                    previewViewModel,
                                    surfaceView,
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
        }
    }

    private fun TouchForwardingLayout.initTouchForwarding(targetView: View) {
        setForwardingEnabled(true)
        setTargetView(targetView)
    }
}
