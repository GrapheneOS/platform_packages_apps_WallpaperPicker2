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

    const val COLOR_ANIMATION_DURATION_MILLIS = 1500L

    interface Binding {
        /** Destroys the binding in spite of lifecycle state. */
        fun destroy()
    }

    /**
     * Binds the color of any view according to a color flow, optionally with a color transition
     * animation.
     *
     * @param setColor a function that sets the color on a view or views when given a color int
     * @param color a color flow used to bind the view color
     * @param shouldAnimate a function that evaluates whether the color update should be animated at
     *   a given time
     * @param lifecycleOwner the lifecycle owner for collecting the color flow
     * @return a binding object that can be used to stop the color flow collection
     */
    fun bind(
        setColor: (color: Int) -> Unit,
        color: Flow<Int>,
        shouldAnimate: () -> Boolean = { true },
        lifecycleOwner: LifecycleOwner,
    ): Binding {
        var animator: Animator? = null
        val job =
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    var currentColor: Int? = null
                    color.collect { newColor ->
                        animator?.end()
                        val previousColor = currentColor
                        if (shouldAnimate() && previousColor != null) {
                            ValueAnimator.ofArgb(previousColor, newColor)
                                .apply {
                                    addUpdateListener { setColor(it.animatedValue as Int) }
                                    duration = COLOR_ANIMATION_DURATION_MILLIS
                                }
                                .also { animator = it }
                                .start()
                        } else {
                            setColor(newColor)
                        }
                        currentColor = newColor
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
