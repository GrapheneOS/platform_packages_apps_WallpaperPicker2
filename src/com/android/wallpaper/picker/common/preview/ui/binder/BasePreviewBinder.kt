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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Common base preview binder that is only responsible for binding the workspace and wallpaper, and
 * uses the [CustomizationPickerViewModel2].
 */
// Based on SmallPreviewBinder, except cleaned up to only bind bind wallpaper and workspace
// (workspace binding to be added). Also we enable a screen to be defined during binding rather than
// reading from viewModel.isViewAsHome.
object BasePreviewBinder {

    fun bind(
        applicationContext: Context,
        view: View,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        workspaceCallbackBinder: WorkspaceCallbackBinder,
        screen: Screen,
        deviceDisplayType: DeviceDisplayType,
        displaySize: Point,
        mainScope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
        onLaunchPreview: ((WallpaperModel) -> Unit)? = null,
        onTransitionToScreen: ((Screen) -> Unit)? = null,
        onPreviewReady: ((Screen) -> Unit)? = null,
        onPreviewSurfaceDestroyed: ((Screen) -> Unit)? = null,
        clockViewFactory: ClockViewFactory,
    ) {
        val wallpaperSurface: SurfaceView = view.requireViewById(R.id.wallpaper_surface)
        val workspaceSurface: SurfaceView = view.requireViewById(R.id.workspace_surface)

        view.contentDescription =
            view.context.getString(
                R.string.wallpaper_preview_card_content_description_non_editable,
                when (screen) {
                    LOCK_SCREEN -> view.context.getString(R.string.lock_screen_tab)
                    HOME_SCREEN -> view.context.getString(R.string.home_screen_tab)
                },
                "", // No need to specify folded or unfolded state for main screen preview
            )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    val previewAlpha =
                        if (screen == HOME_SCREEN) {
                            viewModel.homePreviewAlpha
                        } else {
                            viewModel.lockPreviewAlpha
                        }
                    previewAlpha.collect {
                        if (it.alpha == 0f) {
                            view.importantForAccessibility =
                                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                        } else {
                            view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                        }
                    }
                }

                launch {
                    combine(
                            viewModel.basePreviewViewModel.wallpapers.filterNotNull().map {
                                if (screen == HOME_SCREEN) it.homeWallpaper
                                else it.lockWallpaper ?: it.homeWallpaper
                            },
                            viewModel.selectedPreviewScreen,
                            viewModel.isPreviewClickable,
                            ::Triple,
                        )
                        .collect { (wallpaper, selectedPreviewScreen, isPreviewClickable) ->
                            if (isPreviewClickable) {
                                if (selectedPreviewScreen == screen) {
                                    view.setOnClickListener { onLaunchPreview?.invoke(wallpaper) }
                                    // Set selected state to be announced for Talkback
                                    view.isSelected = true
                                } else {
                                    view.setOnClickListener { onTransitionToScreen?.invoke(screen) }
                                    // Set selected state to be announced for Talkback
                                    view.isSelected = false
                                }
                                view.isClickable = true
                            } else {
                                view.setOnClickListener(null)
                                view.isClickable = false
                                view.isSelected = false
                            }
                        }
                }
            }
        }

        WallpaperPreviewBinder.bind(
            applicationContext = applicationContext,
            surfaceView = wallpaperSurface,
            viewModel = viewModel.basePreviewViewModel,
            screen = screen,
            displaySize = displaySize,
            deviceDisplayType = deviceDisplayType,
            mainScope = mainScope,
            viewLifecycleOwner = lifecycleOwner,
            wallpaperConnectionUtils = wallpaperConnectionUtils,
            isFirstBindingDeferred = isFirstBindingDeferred,
            onPreviewReady = onPreviewReady,
            onPreviewSurfaceDestroyed = onPreviewSurfaceDestroyed,
        )

        WorkspacePreviewBinder.bind(
            surfaceView = workspaceSurface,
            viewModel = viewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            workspaceCallbackBinder = workspaceCallbackBinder,
            screen = screen,
            deviceDisplayType = deviceDisplayType,
            lifecycleOwner = lifecycleOwner,
            clockViewFactory = clockViewFactory,
        )
    }
}
