/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.picker.common.preview.ui.binder

import android.view.SurfaceView
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.picker.customization.ui.util.ViewAlphaAnimator.animateToAlpha
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import kotlinx.coroutines.launch

/**
 * Animates the preview to a targeted alpha. The timing to call bind() is critical. Avoid calling it
 * during Activity or Fragment transition that also takes care of the alpha fade of the screen.
 */
object PreviewAlphaAnimationBinder {

    fun bind(
        previewPager: View,
        viewModel: CustomizationPickerViewModel2,
        lifecycleOwner: LifecycleOwner,
    ) {
        val lockPreview: View = previewPager.requireViewById(R.id.lock_preview)
        val homePreview: View = previewPager.requireViewById(R.id.home_preview)

        val lockWallpaperSurface: SurfaceView = lockPreview.requireViewById(R.id.wallpaper_surface)
        val lockWorkspaceSurface: SurfaceView = lockPreview.requireViewById(R.id.workspace_surface)
        val homeWallpaperSurface: SurfaceView = homePreview.requireViewById(R.id.wallpaper_surface)
        val homeWorkspaceSurface: SurfaceView = homePreview.requireViewById(R.id.workspace_surface)

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.lockPreviewAnimateToAlpha.collect {
                        lockWallpaperSurface.animateToAlpha(it)
                        lockWorkspaceSurface.animateToAlpha(it)
                    }
                }

                launch {
                    viewModel.homePreviewAnimateToAlpha.collect {
                        homeWallpaperSurface.animateToAlpha(it)
                        homeWorkspaceSurface.animateToAlpha(it)
                    }
                }
            }
        }
    }
}
