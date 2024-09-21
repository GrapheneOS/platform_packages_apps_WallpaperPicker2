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
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WorkspacePreviewConfigViewModel
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.SurfaceViewUtils
import kotlin.coroutines.resume
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

object WorkspacePreviewBinder {
    fun bind(
        surface: SurfaceView,
        config: WorkspacePreviewConfigViewModel,
        viewModel: WallpaperPreviewViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        var surfaceCallback: SurfaceViewUtils.SurfaceCallback? = null
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                surfaceCallback =
                    bindSurface(
                        surface = surface,
                        viewModel = viewModel,
                        config = config,
                        lifecycleOwner = lifecycleOwner,
                    )
                surface.setZOrderMediaOverlay(true)
                surface.holder.addCallback(surfaceCallback)
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
        surface: SurfaceView,
        viewModel: WallpaperPreviewViewModel,
        config: WorkspacePreviewConfigViewModel,
        lifecycleOwner: LifecycleOwner,
    ): SurfaceViewUtils.SurfaceCallback {
        return object : SurfaceViewUtils.SurfaceCallback {

            var job: Job? = null
            var previewDisposableHandle: DisposableHandle? = null

            override fun surfaceCreated(holder: SurfaceHolder) {
                job =
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.wallpaperColorsModel.collect {
                            if (it is WallpaperColorsModel.Loaded) {
                                val workspaceCallback =
                                    renderWorkspacePreview(
                                        surface = surface,
                                        previewUtils = config.previewUtils,
                                        displayId =
                                            viewModel.getDisplayId(config.deviceDisplayType),
                                        wallpaperColors = it.colors,
                                    )
                                // Dispose the previous preview on the renderer side.
                                previewDisposableHandle?.dispose()
                                previewDisposableHandle = DisposableHandle {
                                    config.previewUtils.cleanUp(workspaceCallback)
                                }
                            }
                        }
                    }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                job?.cancel()
                job = null
                previewDisposableHandle?.dispose()
                previewDisposableHandle = null
            }
        }
    }

    /**
     * Binds the workspace preview in the full screen, where we need to listen to the changes of the
     * [WorkspacePreviewConfigViewModel] according to which small preview the user clicks on.
     */
    fun bindFullWorkspacePreview(
        surface: SurfaceView,
        viewModel: WallpaperPreviewViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        var surfaceCallback: SurfaceViewUtils.SurfaceCallback? = null
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                surfaceCallback =
                    bindFullSurface(
                        surface = surface,
                        viewModel = viewModel,
                        lifecycleOwner = lifecycleOwner,
                    )
                surface.setZOrderMediaOverlay(true)
                surface.holder.addCallback(surfaceCallback)
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
    private fun bindFullSurface(
        surface: SurfaceView,
        viewModel: WallpaperPreviewViewModel,
        lifecycleOwner: LifecycleOwner,
    ): SurfaceViewUtils.SurfaceCallback {
        return object : SurfaceViewUtils.SurfaceCallback {

            var job: Job? = null
            var previewDisposableHandle: DisposableHandle? = null

            override fun surfaceCreated(holder: SurfaceHolder) {
                job =
                    lifecycleOwner.lifecycleScope.launch {
                        combine(
                                viewModel.fullWorkspacePreviewConfigViewModel,
                                viewModel.wallpaperColorsModel,
                            ) { config, colorsModel ->
                                config to colorsModel
                            }
                            .collect { (config, colorsModel) ->
                                if (colorsModel is WallpaperColorsModel.Loaded) {
                                    val workspaceCallback =
                                        renderWorkspacePreview(
                                            surface = surface,
                                            previewUtils = config.previewUtils,
                                            displayId =
                                                viewModel.getDisplayId(config.deviceDisplayType),
                                            wallpaperColors = colorsModel.colors,
                                        )
                                    // Dispose the previous preview on the renderer side.
                                    previewDisposableHandle?.dispose()
                                    previewDisposableHandle = DisposableHandle {
                                        config.previewUtils.cleanUp(workspaceCallback)
                                    }
                                }
                            }
                    }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                job?.cancel()
                job = null
                previewDisposableHandle?.dispose()
                previewDisposableHandle = null
            }
        }
    }

    private suspend fun renderWorkspacePreview(
        surface: SurfaceView,
        previewUtils: PreviewUtils,
        displayId: Int,
        wallpaperColors: WallpaperColors? = null,
    ): Message? {
        var workspaceCallback: Message? = null
        if (previewUtils.supportsPreview()) {
            // surfaceView.width and surfaceFrame.width here can be different, one represents the
            // size of the view and the other represents the size of the surface. When requesting a
            // preview, make sure to specify the width and height in the bundle so we are using the
            // surface size and not the view size.
            val surfacePosition = surface.holder.surfaceFrame
            val extras =
                bundleOf(
                    Pair(SurfaceViewUtils.KEY_DISPLAY_ID, displayId),
                    Pair(SurfaceViewUtils.KEY_VIEW_WIDTH, surfacePosition.width()),
                    Pair(SurfaceViewUtils.KEY_VIEW_HEIGHT, surfacePosition.height()),
                )
            wallpaperColors?.let {
                extras.putParcelable(SurfaceViewUtils.KEY_WALLPAPER_COLORS, wallpaperColors)
            }
            val request = SurfaceViewUtils.createSurfaceViewRequest(surface, extras)
            workspaceCallback = suspendCancellableCoroutine { continuation ->
                previewUtils.renderPreview(
                    request,
                    object : PreviewUtils.WorkspacePreviewCallback {
                        override fun onPreviewRendered(resultBundle: Bundle?) {
                            if (resultBundle != null) {
                                SurfaceViewUtils.getSurfacePackage(resultBundle).apply {
                                    if (this != null) {
                                        surface.setChildSurfacePackage(this)
                                    } else {
                                        Log.w(
                                            TAG,
                                            "Result bundle from rendering preview does not contain " +
                                                "a child surface package.",
                                        )
                                    }
                                }
                                continuation.resume(SurfaceViewUtils.getCallback(resultBundle))
                            } else {
                                Log.w(TAG, "Result bundle from rendering preview is null.")
                                continuation.resume(null)
                            }
                        }
                    },
                )
            }
        }
        return workspaceCallback
    }

    const val TAG = "WorkspacePreviewBinder"
}
