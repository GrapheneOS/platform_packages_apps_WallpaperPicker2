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
import android.graphics.Point
import android.view.SurfaceView
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.android.wallpaper.R
import com.android.wallpaper.dispatchers.MainDispatcher
import com.android.wallpaper.module.CustomizationSections
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewTransitionViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.PreviewUtils
import kotlinx.coroutines.CoroutineScope

object SmallPreviewBinder {

    fun bind(
        applicationContext: Context,
        view: View,
        viewModel: WallpaperPreviewViewModel,
        @MainDispatcher mainScope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        isRtl: Boolean,
        previewDisplaySize: Point,
        previewDisplayId: Int? = null,
        previewUtils: PreviewUtils? = null,
        navigate: (() -> Unit)? = null,
    ) {
        view.setOnClickListener {
            // TODO(b/291761856): update preview transition view model from
            //                    [SmallPreviewFragment].
            viewModel.previewTransitionViewModel =
                PreviewTransitionViewModel(
                    previewTab = CustomizationSections.Screen.HOME_SCREEN,
                    targetDisplaySize = previewDisplaySize,
                )
            navigate?.invoke()
        }
        val workspaceSurface = view.requireViewById<SurfaceView>(R.id.workspace_surface)
        workspaceSurface.visibility = View.VISIBLE
        workspaceSurface.setZOrderMediaOverlay(true)
        previewUtils?.let { WorkspacePreviewBinder.bind(workspaceSurface, it, previewDisplayId) }

        val wallpaperSurface = view.requireViewById<SurfaceView>(R.id.wallpaper_surface)
        wallpaperSurface.setZOrderMediaOverlay(true)
        WallpaperPreviewBinder.bind(
            applicationContext,
            wallpaperSurface,
            viewModel,
            mainScope,
            lifecycleOwner,
            isSingleDisplayOrUnfoldedHorizontalHinge,
            isRtl
        )
    }
}
