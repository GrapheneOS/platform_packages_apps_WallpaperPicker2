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

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.wallpaper.picker.preview.ui.util.SurfaceViewUtil
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WorkspacePreviewConfigViewModel
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.SurfaceViewUtils
import kotlinx.coroutines.launch

object WorkspacePreviewBinder {
    fun bind(
        surface: SurfaceView,
        config: WorkspacePreviewConfigViewModel,
    ) {
        surface.visibility = View.VISIBLE
        surface.setZOrderMediaOverlay(true)
        surface.holder.addCallback(
            object : SurfaceViewUtil.SurfaceCallback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    renderWorkspacePreview(
                        surface = surface,
                        previewUtils = config.previewUtils,
                        displayId = config.displayId,
                    )
                }
            }
        )
    }

    /**
     * Binds the workspace preview in the full screen, where we need to listen to the changes of the
     * [WorkspacePreviewConfigViewModel] according to which small preview the user clicks on.
     */
    fun bindFullWorkspacePreview(
        surface: SurfaceView,
        viewModel: WallpaperPreviewViewModel,
        lifecycleOwner: LifecycleOwner
    ) {
        surface.visibility = View.VISIBLE
        surface.setZOrderMediaOverlay(true)
        surface.holder.addCallback(
            object : SurfaceViewUtil.SurfaceCallback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.fullWorkspacePreviewConfigViewModel.collect {
                            renderWorkspacePreview(
                                surface = surface,
                                previewUtils = it.previewUtils,
                                displayId = it.displayId,
                            )
                        }
                    }
                }
            }
        )
    }

    private fun renderWorkspacePreview(
        surface: SurfaceView,
        previewUtils: PreviewUtils,
        displayId: Int,
    ) {
        if (previewUtils.supportsPreview()) {
            val request =
                SurfaceViewUtils.createSurfaceViewRequest(
                    surface,
                    bundleOf(Pair(SurfaceViewUtils.KEY_DISPLAY_ID, displayId)),
                )
            previewUtils.renderPreview(
                request,
                object : PreviewUtils.WorkspacePreviewCallback {
                    override fun onPreviewRendered(resultBundle: Bundle?) {
                        if (resultBundle != null) {
                            surface.setChildSurfacePackage(
                                SurfaceViewUtils.getSurfacePackage(resultBundle)
                            )
                        }
                    }
                }
            )
        }
    }
}
