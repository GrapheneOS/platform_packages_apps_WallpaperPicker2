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
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import androidx.viewpager.widget.ViewPager
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout.Companion.getViewId
import com.android.wallpaper.picker.preview.ui.view.DualPreviewViewPager
import com.android.wallpaper.picker.preview.ui.view.adapters.DualPreviewPagerAdapter
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.RtlUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch

/** Binds dual preview home screen and lock screen view pager. */
object DualPreviewPagerBinder {

    fun bind(
        dualPreviewView: DualPreviewViewPager,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        motionLayout: MotionLayout?,
        applicationContext: Context,
        viewLifecycleOwner: LifecycleOwner,
        currentNavDestId: Int,
        transition: Transition?,
        transitionConfig: FullPreviewConfigViewModel?,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
        navigate: (View) -> Unit,
    ) {
        // ViewPager & PagerAdapter do not support RTL. Enable RTL compatibility by converting all
        // indices to LTR using this function.
        val convertToLTR = { position: Int ->
            if (RtlUtils.isRtl(dualPreviewView.context)) {
                wallpaperPreviewViewModel.smallPreviewTabs.size - position - 1
            } else position
        }

        var transitionDisposableHandle: DisposableHandle? = null
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                if (transitionConfig != null && transition != null) {
                    val listener =
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                super.onTransitionStart(transition)
                                // Full to small preview return transition is handled by small
                                // preview. Temporarily remove clip to padding to enable the scaled
                                // shared element to display fully.
                                dualPreviewView.clipToPadding = false
                            }

                            override fun onTransitionEnd(transition: Transition) {
                                super.onTransitionEnd(transition)
                                dualPreviewView.clipToPadding = true
                                transition.removeListener(this)
                                transitionDisposableHandle = null
                            }
                        }
                    transition.addListener(listener)
                    transitionDisposableHandle = DisposableHandle {
                        transition.removeListener(listener)
                    }
                }
            }
            // Remove transition listeners on destroy
            transitionDisposableHandle?.dispose()
            transitionDisposableHandle = null
        }

        // implement adapter for the dual preview pager
        dualPreviewView.adapter = DualPreviewPagerAdapter { view, position ->
            val positionLTR = convertToLTR(position)
            // Set tag to allow small to full preview transition to accurately identify view
            view.tag = positionLTR

            PreviewTooltipBinder.bindSmallPreviewTooltip(
                tooltipStub = view.requireViewById(R.id.small_preview_tooltip_stub),
                viewModel = wallpaperPreviewViewModel.smallTooltipViewModel,
                lifecycleOwner = viewLifecycleOwner,
            )

            val dualDisplayAspectRatioLayout: DualDisplayAspectRatioLayout =
                view.requireViewById(R.id.dual_preview)

            val displaySizes =
                mapOf(
                    DeviceDisplayType.FOLDED to wallpaperPreviewViewModel.smallerDisplaySize,
                    DeviceDisplayType.UNFOLDED to
                        wallpaperPreviewViewModel.wallpaperDisplaySize.value,
                )
            dualDisplayAspectRatioLayout.setDisplaySizes(displaySizes)
            dualPreviewView.setDisplaySizes(displaySizes)

            DeviceDisplayType.FOLDABLE_DISPLAY_TYPES.forEach { display ->
                val previewDisplaySize = dualDisplayAspectRatioLayout.getPreviewDisplaySize(display)
                previewDisplaySize?.let {
                    SmallPreviewBinder.bind(
                        applicationContext = applicationContext,
                        view = dualDisplayAspectRatioLayout.requireViewById(display.getViewId()),
                        motionLayout = motionLayout,
                        viewModel = wallpaperPreviewViewModel,
                        viewLifecycleOwner = viewLifecycleOwner,
                        screen = wallpaperPreviewViewModel.smallPreviewTabs[positionLTR],
                        displaySize = it,
                        deviceDisplayType = display,
                        currentNavDestId = currentNavDestId,
                        transition = transition,
                        transitionConfig = transitionConfig,
                        wallpaperConnectionUtils = wallpaperConnectionUtils,
                        isFirstBindingDeferred = isFirstBindingDeferred,
                        navigate = navigate,
                    )
                }
            }

            dualPreviewView.overScrollMode = OVER_SCROLL_NEVER
        }

        val onPageChangeListenerPreviews =
            object : ViewPager.OnPageChangeListener {
                override fun onPageSelected(position: Int) {
                    val positionLTR = convertToLTR(position)
                    wallpaperPreviewViewModel.setSmallPreviewSelectedTabIndex(positionLTR)
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
                ) {}

                override fun onPageScrollStateChanged(state: Int) {}
            }
        dualPreviewView.addOnPageChangeListener(onPageChangeListenerPreviews)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                wallpaperPreviewViewModel.smallPreviewSelectedTabIndex.collect { position ->
                    val positionLTR = convertToLTR(position)
                    if (dualPreviewView.currentItem != positionLTR) {
                        dualPreviewView.setCurrentItem(positionLTR, /* smoothScroll= */ true)
                    }
                }
            }
        }
    }
}
