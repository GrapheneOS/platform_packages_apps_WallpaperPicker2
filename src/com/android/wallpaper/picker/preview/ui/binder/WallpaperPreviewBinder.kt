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
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.wallpaper.dispatchers.MainDispatcher
import com.android.wallpaper.module.WallpaperPersister
import com.android.wallpaper.picker.preview.ui.viewmodel.SmallPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/* Binds preview of static and live wallpaper. */
object WallpaperPreviewBinder {
    fun bind(
        applicationContext: Context,
        @MainDispatcher mainScope: CoroutineScope,
        viewLifecycleOwner: LifecycleOwner,
        viewModel: WallpaperPreviewViewModel,
        smallPreviewConfig: SmallPreviewConfigViewModel,
        wallpaperSurface: SurfaceView,
        fullResImageView: SubsamplingScaleImageView? = null,
        lowResImageView: ImageView? = null,
    ) {
        fullResImageView?.let {
            // Bind static wallpaper
            StaticWallpaperPreviewBinder.bind(
                fullResImageView,
                checkNotNull(lowResImageView),
                viewModel.getStaticWallpaperPreviewViewModel(),
                smallPreviewConfig,
                viewLifecycleOwner,
            )
        }
        // Bind live wallpaper
        ?: viewLifecycleOwner.lifecycleScope.launch {
                WallpaperConnectionUtils.connect(
                    applicationContext,
                    mainScope,
                    checkNotNull(viewModel.editingWallpaper).wallpaperComponent,
                    // TODO b/301088528(giolin): Pass correspondent
                    //                           destination for live
                    //                           wallpaper preview
                    WallpaperPersister.DEST_LOCK_SCREEN,
                    wallpaperSurface,
                )
            }
    }
}
