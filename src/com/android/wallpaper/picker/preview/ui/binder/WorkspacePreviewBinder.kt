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
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.os.bundleOf
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.SurfaceViewUtils

object WorkspacePreviewBinder {
    fun bind(workspaceSurface: SurfaceView, previewUtils: PreviewUtils, displayId: Int? = null) {
        workspaceSurface.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    if (previewUtils.supportsPreview()) {
                        if (displayId == null && workspaceSurface.display == null) {
                            Log.w(
                                "WorkspacePreviewBinder",
                                "No display ID, avoiding asking for workspace preview, lest WallpaperPicker " +
                                    "crash"
                            )
                            return
                        }
                        val request =
                            SurfaceViewUtils.createSurfaceViewRequest(
                                workspaceSurface,
                                bundleOf(Pair(SurfaceViewUtils.KEY_DISPLAY_ID, displayId)),
                            )
                        previewUtils.renderPreview(
                            request,
                            object : PreviewUtils.WorkspacePreviewCallback {
                                override fun onPreviewRendered(resultBundle: Bundle?) {
                                    if (resultBundle != null) {
                                        workspaceSurface.setChildSurfacePackage(
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
