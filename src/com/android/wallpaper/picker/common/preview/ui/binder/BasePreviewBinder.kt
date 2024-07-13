/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context
import android.graphics.Point
import android.view.SurfaceView
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.common.preview.ui.viewmodel.BasePreviewViewModel

/**
 * Common base preview binder that is only responsible for binding the workspace and wallpaper, and
 * uses the [BasePreviewViewModel].
 */
// Based on SmallPreviewBinder, except cleaned up to only bind bind wallpaper and workspace
// (workspace binding to be added). Also we enable a screen to be defined during binding rather than
// reading from viewModel.isViewAsHome.
// TODO (b/348462236): bind workspace
object BasePreviewBinder {
    fun bind(
        applicationContext: Context,
        view: View,
        viewModel: BasePreviewViewModel,
        screen: Screen,
        deviceDisplayType: DeviceDisplayType,
        displaySize: Point,
        lifecycleOwner: LifecycleOwner,
        isFirstBinding: Boolean,
    ) {
        val wallpaperSurface: SurfaceView = view.requireViewById(R.id.wallpaper_surface)

        WallpaperPreviewBinder.bind(
            applicationContext = applicationContext,
            surface = wallpaperSurface,
            viewModel = viewModel,
            screen = screen,
            displaySize = displaySize,
            deviceDisplayType = deviceDisplayType,
            viewLifecycleOwner = lifecycleOwner,
            isFirstBinding = isFirstBinding,
        )
    }
}
