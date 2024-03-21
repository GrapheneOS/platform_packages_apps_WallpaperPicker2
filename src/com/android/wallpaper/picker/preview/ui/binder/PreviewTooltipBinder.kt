/*
 * Copyright 2023 The Android Open Source Project
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

import android.view.View
import android.view.ViewStub
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object PreviewTooltipBinder {
    interface TooltipViewModel {
        val shouldShowTooltip: Flow<Boolean>
        fun dismissTooltip()
    }

    fun bindSmallPreviewTooltip(
        tooltipStub: ViewStub,
        viewModel: TooltipViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        var tooltip: View? = null
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.shouldShowTooltip.collect { shouldShowTooltip ->
                        if (shouldShowTooltip && tooltip == null) {
                            tooltip = tooltipStub.inflate()
                        }
                        tooltip?.doOnLayout {
                            it.isVisible = true
                            it.alpha = if (shouldShowTooltip) 0f else 1f
                            it.pivotX = it.measuredWidth / 2f
                            it.pivotY = it.measuredHeight.toFloat()
                            it.scaleX = if (shouldShowTooltip) 0.2f else 1f
                            it.scaleY = if (shouldShowTooltip) 0.2f else 1f

                            if (shouldShowTooltip) {
                                it.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .alpha(1f)
                                    .setStartDelay(1000L)
                                    .setDuration(200L)
                                    .setInterpolator(AccelerateDecelerateInterpolator())
                                    .start()
                            } else {
                                it.animate()
                                    .alpha(0f)
                                    .setDuration(75L)
                                    .setInterpolator(AccelerateDecelerateInterpolator())
                                    .withEndAction { tooltip?.isVisible = false }
                                    .start()
                            }
                        }
                    }
                }
            }
        }
    }

    fun bindFullPreviewTooltip(
        tooltipStub: ViewStub,
        viewModel: TooltipViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        var tooltip: View? = null
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.shouldShowTooltip.collect { shouldShowTooltip ->
                        if (shouldShowTooltip && tooltip == null) {
                            tooltip = tooltipStub.inflate()
                            tooltip?.setOnClickListener { viewModel.dismissTooltip() }
                        }
                        tooltip?.doOnLayout {
                            it.isVisible = true
                            it.alpha = if (shouldShowTooltip) 0f else 1f
                            it.translationY = if (shouldShowTooltip) -20f else 0f

                            if (shouldShowTooltip) {
                                it.animate()
                                    .alpha(1f)
                                    .translationY(0f)
                                    .setStartDelay(500L)
                                    .setDuration(200L)
                                    .setInterpolator(AccelerateDecelerateInterpolator())
                                    .start()
                            } else {
                                it.animate()
                                    .alpha(0f)
                                    .translationY(-20f)
                                    .setDuration(75L)
                                    .setInterpolator(AccelerateDecelerateInterpolator())
                                    .withEndAction { tooltip?.isVisible = false }
                                    .start()
                            }
                        }
                    }
                }
            }
        }
    }
}
