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

package com.android.wallpaper.picker.customization.ui.binder

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel.Companion.COLOR_ANIMATION_DURATION_MILLIS
import kotlinx.coroutines.launch

object DarkModeUpdateBinder {

    interface Binding {
        /** Destroys the binding in spite of lifecycle state. */
        fun destroy()
    }

    /**
     * Enables binding views with animation based on dark mode progress, where a progress of 1f
     * means dark mode is enabled and progress of 0f means dark mode is disabled.
     *
     * @param onProgressChange called when the dark mode progress is updated
     * @param colorUpdateViewModel the view model representing the color state of the views
     * @param shouldAnimate a function that evaluates whether the dark mode progress should be
     *   animated at a given time
     * @param lifecycleOwner the lifecycle owner for collecting the dark mode state from the view
     *   model
     * @return a binding object that can be used to stop the color flow collection
     */
    fun bind(
        onProgressChange: (progress: Float) -> Unit,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimate: () -> Boolean = { true },
        lifecycleOwner: LifecycleOwner,
    ): Binding {
        var animator: Animator? = null
        val job =
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    var currentDarkMode: Boolean? = null
                    colorUpdateViewModel.isDarkMode.collect { isDarkMode ->
                        animator?.end()
                        val previousDarkMode = currentDarkMode
                        if (shouldAnimate() && previousDarkMode != null) {
                            animator =
                                ValueAnimator.ofFloat(
                                        if (previousDarkMode) 1f else 0f,
                                        if (isDarkMode) 1f else 0f,
                                    )
                                    .apply {
                                        duration = COLOR_ANIMATION_DURATION_MILLIS
                                        addUpdateListener {
                                            val progress = it.animatedValue as Float
                                            onProgressChange(progress)
                                        }
                                    }
                                    .also { it.start() }
                        } else {
                            onProgressChange(if (isDarkMode) 1f else 0f)
                        }
                        currentDarkMode = isDarkMode
                    }
                }
            }
        return object : Binding {
            override fun destroy() {
                job.cancel()
                animator?.cancel()
            }
        }
    }
}
