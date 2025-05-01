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
package com.android.wallpaper.picker.preview.ui.binder

import android.content.Context
import android.graphics.Point
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.transition.Transition
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.preview.ui.view.ClickableMotionLayout
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout.Companion.getViewId
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout2
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope

/** Binds single preview home screen and lock screen tabs view pager. */
object PreviewPagerBinder2 {

    private val pagerItems = linkedSetOf(R.id.lock_preview, R.id.home_preview)
    private val commonClickableViewIds =
        listOf(R.id.apply_button, R.id.cancel_button, R.id.home_checkbox, R.id.lock_checkbox)

    fun bind(
        applicationContext: Context,
        mainScope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        previewPager: ClickableMotionLayout,
        viewModel: WallpaperPreviewViewModel,
        previewDisplaySize: Point,
        transition: Transition?,
        transitionConfig: FullPreviewConfigViewModel?,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
        isFoldable: Boolean,
        onPreviewReady: ((Screen) -> Unit)? = null,
        onStartTransition: (() -> Unit)? = null,
        onPreviewSurfaceDestroyed: ((Screen) -> Unit)? = null,
        navigate: (View) -> Unit,
    ) {
        pagerItems.forEach { item ->
            val container = previewPager.requireViewById<View>(item)
            PreviewTooltipBinder.bindSmallPreviewTooltip(
                tooltipStub = container.requireViewById(R.id.small_preview_tooltip_stub),
                viewModel = viewModel.smallTooltipViewModel,
                lifecycleOwner = lifecycleOwner,
            )

            if (isFoldable) {
                val dualDisplayAspectRatioLayout: DualDisplayAspectRatioLayout2 =
                    container.requireViewById(R.id.dual_preview)
                val displaySizes =
                    mapOf(
                        DeviceDisplayType.FOLDED to viewModel.smallerDisplaySize,
                        DeviceDisplayType.UNFOLDED to viewModel.wallpaperDisplaySize.value,
                    )
                dualDisplayAspectRatioLayout.setDisplaySizes(displaySizes)
                previewPager.setClickableViewIds(
                    commonClickableViewIds.toList() +
                        DeviceDisplayType.FOLDABLE_DISPLAY_TYPES.map { it.getViewId() }
                )
                DeviceDisplayType.FOLDABLE_DISPLAY_TYPES.forEach { display ->
                    dualDisplayAspectRatioLayout.getPreviewDisplaySize(display)?.let { displaySize
                        ->
                        SmallPreviewBinder.bind(
                            applicationContext = applicationContext,
                            view =
                                dualDisplayAspectRatioLayout.requireViewById(display.getViewId()),
                            viewModel = viewModel,
                            screen = viewModel.smallPreviewTabs[pagerItems.indexOf(item)],
                            displaySize = displaySize,
                            deviceDisplayType = display,
                            mainScope = mainScope,
                            viewLifecycleOwner = lifecycleOwner,
                            currentNavDestId = R.id.smallPreviewFragment,
                            transition = transition,
                            transitionConfig = transitionConfig,
                            wallpaperConnectionUtils = wallpaperConnectionUtils,
                            isFirstBindingDeferred = isFirstBindingDeferred,
                            navigate = navigate,
                            onPreviewReady =
                                // Only report onPreviewReady for UNFOLDED preview as it loads
                                // longer than the FOLDED preview
                                if (display == DeviceDisplayType.UNFOLDED) onPreviewReady else null,
                            onStartTransition = onStartTransition,
                            onPreviewSurfaceDestroyed = onPreviewSurfaceDestroyed,
                            isFoldable = isFoldable,
                        )
                    }
                }
            } else {
                val previewViewId = R.id.preview
                previewPager.setClickableViewIds(commonClickableViewIds.toList() + previewViewId)
                SmallPreviewBinder.bind(
                    applicationContext = applicationContext,
                    view = container.requireViewById(previewViewId),
                    viewModel = viewModel,
                    screen = viewModel.smallPreviewTabs[pagerItems.indexOf(item)],
                    displaySize = previewDisplaySize,
                    deviceDisplayType = DeviceDisplayType.SINGLE,
                    mainScope = mainScope,
                    viewLifecycleOwner = lifecycleOwner,
                    currentNavDestId = R.id.smallPreviewFragment,
                    transition = transition,
                    transitionConfig = transitionConfig,
                    wallpaperConnectionUtils = wallpaperConnectionUtils,
                    isFirstBindingDeferred = isFirstBindingDeferred,
                    navigate = navigate,
                    onPreviewReady = onPreviewReady,
                    onStartTransition = onStartTransition,
                    onPreviewSurfaceDestroyed = onPreviewSurfaceDestroyed,
                    isFoldable = isFoldable,
                )
            }
        }
    }
}
