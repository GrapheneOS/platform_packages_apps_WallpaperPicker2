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

import android.view.SurfaceControlViewHost
import android.view.SurfaceHolder
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.picker.TouchForwardingLayout
import com.android.wallpaper.picker.preview.ui.view.FullPreviewSurfaceView
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewSurfaceViewModel
import kotlinx.coroutines.launch

/** Binds wallpaper preview surface view and its view models. */
object FullPreviewSurfaceViewBinder {

    fun bind(
        surfaceView: FullPreviewSurfaceView,
        surfaceViewModel: FullPreviewSurfaceViewModel,
        viewHierarchyContainer: View,
        surfaceTouchForwardingLayout: TouchForwardingLayout,
        touchRecipientView: View?,
        viewLifecycleOwner: LifecycleOwner,
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
                                val host =
                                    SurfaceControlViewHost(
                                        surfaceView.context,
                                        surfaceView.display,
                                        surfaceView.hostToken,
                                    )
                                host.setView(
                                    viewHierarchyContainer,
                                    surfaceView.width,
                                    surfaceView.height,
                                )
                                surfaceView.setChildSurfacePackage(
                                    checkNotNull(host.surfacePackage)
                                )
                                touchRecipientView?.let {
                                    surfaceTouchForwardingLayout.initTouchForwarding(it)
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
        }
    }

    private fun TouchForwardingLayout.initTouchForwarding(targetView: View) {
        setForwardingEnabled(true)
        setTargetView(targetView)
    }
}
