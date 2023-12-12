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
import android.view.SurfaceView
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.FoldableDisplay
import com.android.wallpaper.model.wallpaper.ScreenOrientation
import com.android.wallpaper.module.CustomizationSections.Screen
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.CoroutineScope

object SmallPreviewBinder {
    /** @param foldableDisplay Only used for foldable devices; otherwise, set to null. */
    fun bind(
        applicationContext: Context,
        view: View,
        viewModel: WallpaperPreviewViewModel,
        screen: Screen,
        orientation: ScreenOrientation,
        foldableDisplay: FoldableDisplay?,
        @MainDispatcher mainScope: CoroutineScope,
        viewLifecycleOwner: LifecycleOwner,
        navigate: ((View) -> Unit)? = null,
    ) {
        val wallpaperSurface: SurfaceView = view.requireViewById(R.id.wallpaper_surface)
        val workspaceSurface: SurfaceView = view.requireViewById(R.id.workspace_surface)

        view.setOnClickListener {
            viewModel.onSmallPreviewClicked(screen, orientation, foldableDisplay)
            navigate?.invoke(wallpaperSurface)
        }

        val config = viewModel.getWorkspacePreviewConfig(screen, foldableDisplay)
        WorkspacePreviewBinder.bind(
            workspaceSurface,
            config,
        )

        SmallWallpaperPreviewBinder.bind(
            surface = wallpaperSurface,
            viewModel = viewModel,
            screen = screen,
            screenOrientation = orientation,
            applicationContext = applicationContext,
            mainScope = mainScope,
            viewLifecycleOwner = viewLifecycleOwner,
        )
    }
}
