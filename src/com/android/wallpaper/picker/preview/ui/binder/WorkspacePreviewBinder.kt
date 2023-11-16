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
import com.android.wallpaper.picker.preview.ui.viewmodel.WorkspacePreviewConfigViewModel
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.SurfaceViewUtils

object WorkspacePreviewBinder {
    fun bind(surface: SurfaceView, config: WorkspacePreviewConfigViewModel) {
        surface.visibility = View.VISIBLE
        surface.setZOrderMediaOverlay(true)
        surface.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    if (config.previewUtils.supportsPreview()) {
                        val request =
                            SurfaceViewUtils.createSurfaceViewRequest(
                                surface,
                                bundleOf(Pair(SurfaceViewUtils.KEY_DISPLAY_ID, config.displayId)),
                            )
                        config.previewUtils.renderPreview(
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

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {}
            }
        )
    }
}
