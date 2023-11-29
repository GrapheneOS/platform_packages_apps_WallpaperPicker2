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
import android.graphics.PointF
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.wallpaper.R
import com.android.wallpaper.dispatchers.MainDispatcher
import com.android.wallpaper.model.wallpaper.WallpaperModel
import com.android.wallpaper.picker.TouchForwardingLayout
import com.android.wallpaper.picker.preview.ui.util.FullResImageViewUtil.getCropRect
import com.android.wallpaper.picker.preview.ui.util.SurfaceViewUtil
import com.android.wallpaper.picker.preview.ui.util.SurfaceViewUtil.attachView
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.OnStateChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Binds wallpaper preview surface view and its view models. */
object FullWallpaperPreviewBinder {

    fun bind(
        applicationContext: Context,
        surfaceView: SurfaceView,
        surfaceTouchForwardingLayout: TouchForwardingLayout,
        viewModel: WallpaperPreviewViewModel,
        viewLifecycleOwner: LifecycleOwner,
        @MainDispatcher mainScope: CoroutineScope,
    ) {
        val previewConfig = viewModel.selectedSmallPreviewConfig ?: return
        var job: Job? = null
        surfaceView.setZOrderMediaOverlay(true)
        surfaceView.holder.addCallback(
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
                                        previewConfig.previewTab.toFlag(),
                                        surfaceView,
                                    )
                                } else if (wallpaper is WallpaperModel.StaticWallpaperModel) {
                                    val (lowResImageView, fullResImageView) =
                                        initStaticPreviewSurface(
                                            applicationContext,
                                            surfaceView,
                                            surfaceTouchForwardingLayout,
                                        ) { rect ->
                                            viewModel
                                                .getStaticWallpaperPreviewViewModel()
                                                .fullPreviewCrop = rect
                                        }
                                    // Bind static wallpaper
                                    StaticWallpaperPreviewBinder.bind(
                                        lowResImageView,
                                        fullResImageView,
                                        viewModel.getStaticWallpaperPreviewViewModel(),
                                        previewConfig.screenOrientation,
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

    private fun initStaticPreviewSurface(
        applicationContext: Context,
        surfaceView: SurfaceView,
        surfaceTouchForwardingLayout: TouchForwardingLayout,
        onNewCrop: (crop: Rect) -> Unit
    ): Pair<ImageView, SubsamplingScaleImageView> {
        val preview =
            LayoutInflater.from(applicationContext)
                .inflate(R.layout.fullscreen_wallpaper_preview, null)
        surfaceView.attachView(preview)
        val fullResImageView =
            preview.requireViewById<SubsamplingScaleImageView>(R.id.full_res_image)
        surfaceTouchForwardingLayout.initTouchForwarding(fullResImageView)
        fullResImageView.setOnNewCropListener { onNewCrop.invoke(it) }
        return Pair(preview.requireViewById(R.id.low_res_image), fullResImageView)
    }

    private fun TouchForwardingLayout.initTouchForwarding(targetView: View) {
        setForwardingEnabled(true)
        setTargetView(targetView)
    }

    private fun SubsamplingScaleImageView.setOnNewCropListener(onNewCrop: (crop: Rect) -> Unit) {
        setOnStateChangedListener(
            object : OnStateChangedListener {
                override fun onScaleChanged(p0: Float, p1: Int) {
                    onNewCrop.invoke(getCropRect())
                }

                override fun onCenterChanged(p0: PointF?, p1: Int) {
                    onNewCrop.invoke(getCropRect())
                }
            }
        )
    }
}
