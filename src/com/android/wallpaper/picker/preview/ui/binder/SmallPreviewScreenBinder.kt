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
import android.widget.Button
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.Transition
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.preview.ui.view.ClickableMotionLayout
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.Companion.PreviewScreen
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

object SmallPreviewScreenBinder {
    fun bind(
        applicationContext: Context,
        mainScope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        fragmentLayout: MotionLayout,
        viewModel: WallpaperPreviewViewModel,
        previewDisplaySize: Point,
        transition: Transition?,
        transitionConfig: FullPreviewConfigViewModel?,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
        isFoldable: Boolean,
        navigate: (View) -> Unit,
    ) {
        val previewPager = fragmentLayout.requireViewById<ClickableMotionLayout>(R.id.preview_pager)
        previewPager.jumpToState(
            if (viewModel.smallPreviewSelectedTab.value == Screen.LOCK_SCREEN)
                R.id.lock_preview_selected
            else R.id.home_preview_selected
        )
        val previewPagerContainer =
            fragmentLayout.requireViewById<MotionLayout>(R.id.small_preview_container)
        val nextButton = fragmentLayout.requireViewById<Button>(R.id.button_next)

        PreviewPagerBinder2.bind(
            applicationContext,
            mainScope,
            lifecycleOwner,
            previewPager,
            viewModel,
            previewDisplaySize,
            transition,
            transitionConfig,
            wallpaperConnectionUtils,
            isFirstBindingDeferred,
            isFoldable,
            navigate,
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                            viewModel.currentPreviewScreen,
                            viewModel.smallPreviewSelectedTab,
                            viewModel.previewActionsViewModel.isActionChecked,
                            viewModel.isSetWallpaperButtonVisible,
                            viewModel.previewActionsViewModel.isDownloadVisible,
                            ::Quintuple,
                        )
                        .collect { (screen, tab, isActionChecked, isNextVisible, isDownloadEnabled)
                            ->
                            when (screen) {
                                PreviewScreen.SMALL_PREVIEW -> {
                                    val endState =
                                        if (isNextVisible) R.id.small_preview
                                        else R.id.small_preview_not_downloaded
                                    if (
                                        fragmentLayout.endState == R.id.small_preview_no_header &&
                                            fragmentLayout.startState ==
                                                R.id.small_preview_not_downloaded &&
                                            !isDownloadEnabled
                                    ) {
                                        // When entering the non downloadable wallpaper preview the
                                        // first time, use scheduleTransitionTo so the transition
                                        // is not conflicting with the rest of the transition.
                                        fragmentLayout.scheduleTransitionTo(endState)
                                    } else {
                                        fragmentLayout.transitionToState(endState)
                                    }

                                    previewPagerContainer.transitionToState(
                                        if (isActionChecked) R.id.floating_sheet_visible
                                        else R.id.floating_sheet_gone
                                    )
                                    previewPager.transitionToState(
                                        if (tab == Screen.LOCK_SCREEN) R.id.lock_preview_selected
                                        else R.id.home_preview_selected
                                    )
                                }
                                PreviewScreen.FULL_PREVIEW -> {
                                    // TODO(b/367374790): Transition to full preview
                                }
                                PreviewScreen.APPLY_WALLPAPER -> {
                                    fragmentLayout.transitionToState(R.id.small_preview_no_header)
                                    previewPagerContainer.transitionToState(
                                        R.id.show_apply_wallpaper
                                    )
                                    previewPager.transitionToState(
                                        if (isFoldable) R.id.apply_wallpaper_lock_preview_selected
                                        else R.id.apply_wallpaper_preview_only
                                    )
                                }
                            }
                        }
                }

                launch {
                    viewModel.shouldEnableClickOnPager.collect {
                        previewPager.shouldInterceptTouch = it
                    }
                }

                launch {
                    viewModel.isSetWallpaperButtonVisible.collect { nextButton.isVisible = it }
                }

                launch {
                    viewModel.isSetWallpaperButtonEnabled.collect { nextButton.isEnabled = it }
                }

                launch {
                    viewModel.onNextButtonClicked.collect { onClicked ->
                        nextButton.setOnClickListener(
                            if (onClicked != null) {
                                { onClicked() }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E,
    )
}
