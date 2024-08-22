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

package com.android.wallpaper.picker.customization.ui.binder

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object ColorUpdateBinder {

    private const val COLOR_ANIMATION_DURATION_MILLIS = 1500L

    fun bind(
        setColor: (color: Int) -> Unit,
        color: Flow<Int>,
        lifecycleOwner: LifecycleOwner,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var currentColor: Int? = null
                var animator: Animator? = null
                color.collect { newColor ->
                    val previousColor = currentColor
                    if (previousColor == null) {
                        setColor(newColor)
                    } else {
                        animator?.end()
                        ValueAnimator.ofArgb(
                                previousColor,
                                newColor,
                            )
                            .apply {
                                addUpdateListener { setColor(it.animatedValue as Int) }
                                duration = COLOR_ANIMATION_DURATION_MILLIS
                            }
                            .also { animator = it }
                            .start()
                    }
                    currentColor = newColor
                }
            }
        }
    }
}
